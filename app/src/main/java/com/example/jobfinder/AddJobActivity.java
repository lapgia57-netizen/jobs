package com.example.jobfinder;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.jobfinder.model.Job;
import com.example.jobfinder.util.FirebaseConfig;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Đăng tin mới hoặc sửa tin (extra "edit_job_id").
 * Ghi đồng thời 3 path bằng updateChildren().
 */
public class AddJobActivity extends AppCompatActivity {

    private TextInputEditText edtTitle, edtCompany, edtLocation, edtSalary,
            edtSalaryMin, edtSalaryMax, edtDescription, edtSource;
    private MaterialButton btnSave;
    private Toolbar toolbar;

    private String editJobId;
    private long originalCreatedAt;
    private boolean originalVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_job);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        edtTitle = findViewById(R.id.edtJobTitle);
        edtCompany = findViewById(R.id.edtJobCompany);
        edtLocation = findViewById(R.id.edtJobLocation);
        edtSalary = findViewById(R.id.edtJobSalary);
        edtSalaryMin = findViewById(R.id.edtSalaryMin);
        edtSalaryMax = findViewById(R.id.edtSalaryMax);
        edtDescription = findViewById(R.id.edtJobDescription);
        edtSource = findViewById(R.id.edtJobSource);
        btnSave = findViewById(R.id.btnSave);

        editJobId = getIntent().getStringExtra("edit_job_id");
        if (editJobId != null) {
            toolbar.setTitle(R.string.edit_job_title);
            loadExistingJob(editJobId);
        }

        btnSave.setOnClickListener(v -> saveJob());
    }

    private void loadExistingJob(String jobId) {
        FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL).getReference("stories_details").child(jobId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Job job = snapshot.getValue(Job.class);
                        if (job == null) {
                            Toast.makeText(AddJobActivity.this, "Không tải được tin", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        edtTitle.setText(job.getTitle());
                        edtCompany.setText(job.getCompany());
                        edtLocation.setText(job.getLocation());
                        edtSalary.setText(job.getSalary());
                        if (job.getSalaryMin() > 0) edtSalaryMin.setText(String.valueOf(job.getSalaryMin() / 1_000_000L));
                        if (job.getSalaryMax() > 0) edtSalaryMax.setText(String.valueOf(job.getSalaryMax() / 1_000_000L));
                        edtDescription.setText(job.getDescription());
                        edtSource.setText(job.getSource());
                        originalCreatedAt = job.getCreatedAt();
                        originalVisible = job.isVisible();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AddJobActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveJob() {
        String title = text(edtTitle);
        String company = text(edtCompany);
        String location = text(edtLocation);
        String salary = text(edtSalary);
        String description = text(edtDescription);
        String source = text(edtSource);
        // Người dùng nhập số triệu; lưu thành VND * 1_000_000 để lọc
        long salaryMinMil = parseLongOrZero(text(edtSalaryMin));
        long salaryMaxMil = parseLongOrZero(text(edtSalaryMax));
        long salaryMin = salaryMinMil * 1_000_000L;
        long salaryMax = salaryMaxMil * 1_000_000L;

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(company)) {
            Toast.makeText(this, "Vui lòng nhập ít nhất vị trí và công ty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (salaryMax > 0 && salaryMin > salaryMax) {
            Toast.makeText(this, "Lương tối thiểu không được lớn hơn lương tối đa", Toast.LENGTH_SHORT).show();
            return;
        }
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        boolean isEdit = editJobId != null;
        String jobId = isEdit ? editJobId
                : FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL).getReference("stories_index").push().getKey();
        if (jobId == null) {
            Toast.makeText(this, "Không tạo được ID, thử lại", Toast.LENGTH_SHORT).show();
            return;
        }
        long createdAt = isEdit ? originalCreatedAt : System.currentTimeMillis();
        boolean visible = isEdit ? originalVisible : true;

        Map<String, Object> indexData = new HashMap<>();
        indexData.put("title", title);
        indexData.put("company", company);
        indexData.put("location", location);
        indexData.put("createdBy", email);
        indexData.put("ownerUid", uid);
        indexData.put("visible", visible);
        indexData.put("createdAt", createdAt);
        // salary hiển thị trên card list nếu có
        if (!TextUtils.isEmpty(salary)) indexData.put("salary", salary);

        Map<String, Object> detailData = new HashMap<>();
        detailData.put("title", title);
        detailData.put("company", company);
        detailData.put("location", location);
        detailData.put("salary", salary);
        detailData.put("salaryMin", salaryMin);
        detailData.put("salaryMax", salaryMax);
        detailData.put("description", description);
        detailData.put("source", source);
        detailData.put("createdBy", email);
        detailData.put("ownerUid", uid);
        detailData.put("visible", visible);
        detailData.put("createdAt", createdAt);

        Map<String, Object> userJobData = new HashMap<>();
        userJobData.put("title", title);
        userJobData.put("company", company);
        userJobData.put("visible", visible);
        userJobData.put("createdAt", createdAt);

        Map<String, Object> multiPathUpdate = new HashMap<>();
        multiPathUpdate.put("/stories_index/" + jobId, indexData);
        multiPathUpdate.put("/stories_details/" + jobId, detailData);
        multiPathUpdate.put("/users/" + uid + "/jobs/" + jobId, userJobData);

        // Đảm bảo ownerUid luôn có giá trị (rule yêu cầu bắt buộc)
        if (TextUtils.isEmpty(uid)) {
            Toast.makeText(this, "Không lấy được UID người dùng", Toast.LENGTH_LONG).show();
            return;
        }

        btnSave.setEnabled(false);
        FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL).getReference().updateChildren(multiPathUpdate)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, isEdit ? "Đã cập nhật tin" : "Đã đăng tin", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    // Hiển thị rõ hơn khi bị rule chặn
                    if (msg.toLowerCase().contains("permission") || msg.toLowerCase().contains("denied")) {
                        msg = "Permission denied – kiểm tra Rules đã deploy chưa & đã đăng nhập đúng tài khoản. Chi tiết: " + msg;
                    }
                    Toast.makeText(this, "Lỗi lưu: " + msg, Toast.LENGTH_LONG).show();
                });
    }

    private static String text(TextInputEditText edt) {
        return edt.getText() != null ? edt.getText().toString().trim() : "";
    }

    private long parseLongOrZero(String s) {
        if (TextUtils.isEmpty(s)) return 0L;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
