package com.example.jobfinder;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jobfinder.adapter.DashboardJobAdapter;
import com.example.jobfinder.model.Job;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard riêng của TỪNG tài khoản: chỉ đọc "users/{uid}/jobs" - tức chỉ 1
 * đường dẫn duy nhất, không phải quét toàn bộ "stories_index" rồi lọc theo
 * createdBy như cách làm cũ (chậm hơn khi dữ liệu lớn). Đây là lý do dữ liệu
 * được ghi trùng lặp có chủ đích vào node "users" khi đăng tin.
 *
 * Từ đây, chủ tin có thể: bật/tắt hiển thị công khai (ẩn/hiện), sửa, xoá tin
 * của chính mình.
 */
public class DashboardActivity extends AppCompatActivity implements DashboardJobAdapter.Callback {

    private RecyclerView recyclerView;
    private DashboardJobAdapter adapter;
    private TextView tvEmpty;
    private DatabaseReference myJobsRef;
    private ValueEventListener valueEventListener;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        setTitle(R.string.dashboard_title);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView = findViewById(R.id.recyclerViewDashboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DashboardJobAdapter(this);
        recyclerView.setAdapter(adapter);

        tvEmpty = findViewById(R.id.tvEmptyDashboard);

        FloatingActionButton fab = findViewById(R.id.fabAddJobDashboard);
        fab.setOnClickListener(v -> startActivity(new Intent(this, AddJobActivity.class)));

        myJobsRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("jobs");
        attachListener();
    }

    private void attachListener() {
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Job> jobs = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Job job = child.getValue(Job.class);
                    if (job != null) {
                        job.setId(child.getKey());
                        jobs.add(job);
                    }
                }
                Collections.sort(jobs, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                adapter.submitList(jobs);
                tvEmpty.setVisibility(jobs.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
        myJobsRef.addValueEventListener(valueEventListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (valueEventListener != null) {
            myJobsRef.removeEventListener(valueEventListener);
        }
    }

    @Override
    public void onToggleVisible(Job job, boolean makeVisible) {
        // Cập nhật "visible" đồng thời ở cả 3 nơi bằng 1 multi-path update duy nhất,
        // đảm bảo danh sách công khai (MainActivity) và dashboard luôn nhất quán ngay lập tức.
        Map<String, Object> updates = new HashMap<>();
        updates.put("/stories_index/" + job.getId() + "/visible", makeVisible);
        updates.put("/stories_details/" + job.getId() + "/visible", makeVisible);
        updates.put("/users/" + uid + "/jobs/" + job.getId() + "/visible", makeVisible);

        FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Không cập nhật được: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override
    public void onEdit(Job job) {
        Intent intent = new Intent(this, AddJobActivity.class);
        intent.putExtra("edit_job_id", job.getId());
        startActivity(intent);
    }

    @Override
    public void onDelete(Job job) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setPositiveButton("Xoá", (dialog, which) -> doDelete(job))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void doDelete(Job job) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("/stories_index/" + job.getId(), null);
        updates.put("/stories_details/" + job.getId(), null);
        updates.put("/users/" + uid + "/jobs/" + job.getId(), null);

        FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                .addOnSuccessListener(unused -> Toast.makeText(this, "Đã xoá tin", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Không xoá được: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override
    public void onClick(Job job) {
        Intent intent = new Intent(this, JobDetailActivity.class);
        intent.putExtra("job_id", job.getId());
        startActivity(intent);
    }
}
