package com.example.jobfinder;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jobfinder.adapter.JobAdapter;
import com.example.jobfinder.model.Job;
import com.example.jobfinder.util.SearchUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn hình "Tất cả việc làm" - liệt kê tin của TẤT CẢ user đã đăng (không chỉ
 * của riêng người đang đăng nhập), kèm bộ tìm kiếm nhiều điều kiện kết hợp
 * AND (chặt chẽ): mọi điều kiện đã nhập đều phải đúng đồng thời thì tin mới
 * được hiển thị - không có kiểu "khớp 1 trong nhiều điều kiện" (OR) làm loãng
 * kết quả.
 *
 * Dữ liệu gốc được đồng bộ realtime từ "stories_index" vào bộ nhớ (masterJobs),
 * sau đó mỗi lần dữ liệu đổi hoặc người dùng gõ tìm kiếm, ta tính lại danh sách
 * đã lọc và đẩy vào adapter (có DiffUtil nên không giật hình khi gõ liên tục).
 * Chỉ những tin có visible == true mới được đưa vào danh sách hiển thị công khai.
 */
public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private JobAdapter adapter;
    private EditText edtKeyword, edtLocation, edtMinSalary;
    private TextView tvResultCount;

    private DatabaseReference indexRef;
    private ChildEventListener childEventListener;

    // Dữ liệu gốc (toàn bộ, kể cả tin đã ẩn) giữ theo id để dễ thêm/sửa/xoá khi có sự kiện realtime.
    private final Map<String, Job> masterJobs = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        edtKeyword = findViewById(R.id.edtKeyword);
        edtLocation = findViewById(R.id.edtLocation);
        edtMinSalary = findViewById(R.id.edtMinSalary);
        tvResultCount = findViewById(R.id.tvResultCount);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new JobAdapter(job -> {
            Intent intent = new Intent(MainActivity.this, JobDetailActivity.class);
            intent.putExtra("job_id", job.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        TextWatcher liveFilterWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilterAndRender();
            }
        };
        edtKeyword.addTextChangedListener(liveFilterWatcher);
        edtLocation.addTextChangedListener(liveFilterWatcher);
        edtMinSalary.addTextChangedListener(liveFilterWatcher);

        FloatingActionButton fab = findViewById(R.id.fabAddJob);
        fab.setOnClickListener(v -> startActivity(new Intent(this, AddJobActivity.class)));

        indexRef = FirebaseDatabase.getInstance().getReference("stories_index");
        attachListener();
    }

    private void attachListener() {
        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                Job job = snapshot.getValue(Job.class);
                if (job != null) {
                    job.setId(snapshot.getKey());
                    masterJobs.put(job.getId(), job);
                    applyFilterAndRender();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                Job job = snapshot.getValue(Job.class);
                if (job != null) {
                    job.setId(snapshot.getKey());
                    masterJobs.put(job.getId(), job);
                    applyFilterAndRender();
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                masterJobs.remove(snapshot.getKey());
                applyFilterAndRender();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) { }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
        indexRef.addChildEventListener(childEventListener);
    }

    /**
     * Áp dụng toàn bộ bộ lọc theo kiểu AND chặt chẽ:
     *  1. visible == true (tin bị chủ tin ẩn sẽ không bao giờ lọt vào đây)
     *  2. Từ khoá: TẤT CẢ các từ người dùng gõ phải xuất hiện trong
     *     (tiêu đề + công ty + địa điểm), đã chuẩn hoá bỏ dấu/hoa-thường.
     *  3. Địa điểm: nếu có nhập, địa điểm tin phải chứa đúng cụm đã nhập
     *     (sau chuẩn hoá) - không so khớp mờ.
     *  4. Lương tối thiểu mong muốn: nếu có nhập, tin phải có salaryMin
     *     lớn hơn hoặc bằng giá trị đó (so sánh số học chính xác, không
     *     suy đoán từ chuỗi hiển thị).
     */
    private void applyFilterAndRender() {
        String keyword = edtKeyword.getText().toString();
        String location = SearchUtils.normalize(edtLocation.getText().toString());
        String minSalaryStr = edtMinSalary.getText().toString().trim();
        Long minSalaryWanted = null;
        if (!minSalaryStr.isEmpty()) {
            try {
                minSalaryWanted = Long.parseLong(minSalaryStr);
            } catch (NumberFormatException ignored) {
                // Nhập sai định dạng số -> coi như không lọc theo lương để tránh loại hết kết quả oan.
            }
        }

        String[] keywordWords = SearchUtils.toWords(SearchUtils.normalize(keyword));

        List<Job> result = new ArrayList<>();
        for (Job job : masterJobs.values()) {
            if (!job.isVisible()) continue;

            if (keywordWords.length > 0) {
                String haystack = job.getTitle() + " " + job.getCompany() + " " + job.getLocation();
                if (!SearchUtils.containsAllWords(haystack, keywordWords)) continue;
            }

            if (!location.isEmpty()) {
                String normalizedJobLocation = SearchUtils.normalize(job.getLocation());
                if (!normalizedJobLocation.contains(location)) continue;
            }

            if (minSalaryWanted != null && job.getSalaryMin() < minSalaryWanted) continue;

            result.add(job);
        }

        // Mới nhất lên trước. Dùng Collections.sort (không phải List.sort) để tương thích minSdk 23.
        Collections.sort(result, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        adapter.submitList(result);
        tvResultCount.setText(getString(R.string.result_count_format, result.size()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (childEventListener != null) {
            indexRef.removeEventListener(childEventListener);
        }
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
        } else if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
