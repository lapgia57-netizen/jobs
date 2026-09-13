package com.example.jobfinder;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.jobfinder.adapter.JobAdapter;
import com.example.jobfinder.model.Job;
import com.example.jobfinder.util.FirebaseConfig;
import com.example.jobfinder.util.SearchUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn hình công khai: infinite scroll, mỗi lần tải 10 tin (theo createdAt giảm dần).
 * Chỉ hiển thị tin visible == true. Tìm kiếm AND chặt chẽ trên tập đã tải + vẫn cho load thêm.
 */
public class MainActivity extends AppCompatActivity implements JobAdapter.OnJobClickListener {

    private static final int PAGE_SIZE = 10;

    private RecyclerView recyclerView;
    private JobAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextInputEditText edtKeyword, edtLocation, edtMinSalary;
    private TextView tvResultCount, tvEmpty;
    private View emptyState;

    private DatabaseReference indexRef;

    /** Toàn bộ tin đã tải từ server (kể cả ẩn), key = jobId */
    private final Map<String, Job> loadedJobs = new LinkedHashMap<>();

    /** createdAt nhỏ nhất đã tải (để lấy trang cũ hơn) */
    private Long oldestCreatedAt = null;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            goLogin();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        edtKeyword = findViewById(R.id.edtKeyword);
        edtLocation = findViewById(R.id.edtLocation);
        edtMinSalary = findViewById(R.id.edtMinSalary);
        tvResultCount = findViewById(R.id.tvResultCount);
        emptyState = findViewById(R.id.emptyState);
        tvEmpty = findViewById(R.id.tvEmpty);
        FloatingActionButton fab = findViewById(R.id.fabAdd);

        adapter = new JobAdapter(this);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(adapter);

        indexRef = FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL).getReference("stories_index");

        swipeRefresh.setColorSchemeResources(R.color.brand_primary);
        swipeRefresh.setOnRefreshListener(this::reloadFromStart);

        fab.setOnClickListener(v ->
                startActivity(new Intent(this, AddJobActivity.class)));

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { applyFilterAndShow(); }
        };
        edtKeyword.addTextChangedListener(watcher);
        edtLocation.addTextChangedListener(watcher);
        edtMinSalary.addTextChangedListener(watcher);

        // Infinite scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || isLoading || !hasMore) return;
                int total = lm.getItemCount();
                int lastVisible = lm.findLastVisibleItemPosition();
                // Tải thêm khi còn ~3 item nữa là hết
                if (lastVisible >= total - 3) {
                    loadNextPage();
                }
            }
        });

        loadNextPage();
    }

    private void reloadFromStart() {
        loadedJobs.clear();
        oldestCreatedAt = null;
        hasMore = true;
        isFirstLoad = true;
        adapter.submitList(Collections.emptyList());
        loadNextPage();
    }

    private void loadNextPage() {
        if (isLoading || !hasMore) {
            swipeRefresh.setRefreshing(false);
            return;
        }
        isLoading = true;
        if (!isFirstLoad) {
            adapter.setShowLoadingFooter(true);
        }

        Query query;
        if (oldestCreatedAt == null) {
            // Trang đầu: 10 tin mới nhất
            query = indexRef.orderByChild("createdAt").limitToLast(PAGE_SIZE);
        } else {
            // Trang sau: tin cũ hơn oldestCreatedAt
            query = indexRef.orderByChild("createdAt")
                    .endAt(oldestCreatedAt - 1)
                    .limitToLast(PAGE_SIZE);
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Job> batch = new ArrayList<>();
                long minAt = Long.MAX_VALUE;

                for (DataSnapshot child : snapshot.getChildren()) {
                    Job job = child.getValue(Job.class);
                    if (job == null) continue;
                    job.setId(child.getKey());
                    // Giữ bản ghi (kể cả ẩn) để không mất khi toggle
                    loadedJobs.put(job.getId(), job);
                    batch.add(job);
                    if (job.getCreatedAt() < minAt) {
                        minAt = job.getCreatedAt();
                    }
                }

                if (batch.isEmpty()) {
                    hasMore = false;
                } else {
                    oldestCreatedAt = minAt;
                    // Nếu nhận ít hơn PAGE_SIZE → hết data
                    if (batch.size() < PAGE_SIZE) {
                        hasMore = false;
                    }
                }

                isLoading = false;
                isFirstLoad = false;
                adapter.setShowLoadingFooter(false);
                swipeRefresh.setRefreshing(false);
                applyFilterAndShow();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isLoading = false;
                isFirstLoad = false;
                adapter.setShowLoadingFooter(false);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(MainActivity.this,
                        "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilterAndShow() {
        String keyword = textOf(edtKeyword);
        String location = textOf(edtLocation);
        long minSalary = 0;
        try {
            String s = textOf(edtMinSalary);
            if (!TextUtils.isEmpty(s)) minSalary = Long.parseLong(s);
        } catch (NumberFormatException ignored) {}

        List<Job> filtered = new ArrayList<>();
        for (Job job : loadedJobs.values()) {
            if (!job.isVisible()) continue;
            if (!SearchUtils.matches(job, keyword, location, minSalary)) continue;
            filtered.add(job);
        }

        // Mới nhất trước
        Collections.sort(filtered, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        adapter.submitList(filtered);
        tvResultCount.setText(getString(R.string.result_count_format, filtered.size()));

        boolean empty = filtered.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            boolean hasFilter = !TextUtils.isEmpty(keyword)
                    || !TextUtils.isEmpty(location)
                    || minSalary > 0;
            tvEmpty.setText(hasFilter ? R.string.empty_search : R.string.empty_jobs);
        }
    }

    private static String textOf(TextInputEditText edt) {
        return edt.getText() != null ? edt.getText().toString().trim() : "";
    }

    @Override
    public void onJobClick(Job job) {
        Intent i = new Intent(this, JobDetailActivity.class);
        i.putExtra("job_id", job.getId());
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_dashboard) {
            startActivity(new Intent(this, DashboardActivity.class));
            return true;
        }
        if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            goLogin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void goLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Khi quay lại từ Dashboard (ẩn/hiện tin) hoặc AddJob, refresh nhẹ trang đã tải
        if (!isFirstLoad && !isLoading) {
            applyFilterAndShow();
        }
    }
}
