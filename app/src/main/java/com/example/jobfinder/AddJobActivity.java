package com.example.jobfinder;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jobfinder.model.Job;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Đăng tin mới HOẶC sửa tin đã đăng (nếu Intent có extra "edit_job_id").
 *
 * Ghi/ cập nhật đồng thời 3 nơi bằng MỘT lệnh updateChildren() duy nhất
 * (multi-path update) - chỉ tốn 1 round-trip mạng:
 *   /stories_index/{id}        - bản rút gọn, hiển thị công khai
 *   /stories_details/{id}      - bản đầy đủ
 *   /users/{uid}/jobs/{id}     - bản rút gọn riêng cho Dashboard của chủ tin
 *
 * salaryMin/salaryMax là số (đơn vị: triệu VNĐ) để màn hình tìm kiếm có thể
 * lọc chính xác bằng phép so sánh số học, thay vì đoán từ chuỗi "10-15 triệu".
 */
public class AddJobActivity extends AppCompatActivity {

    private EditText edtTitle, edtCompany, edtLocation, edtSalary, edtSalaryMin, edtSalaryMax,
            edtDescription, edtSource;
    private Button btnSave;

    private String editJobId; // null nếu đang tạo tin mới
    private long originalCreatedAt;
    private boolean originalVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_job);

        edtTitle = findViewById(R.id.edtJobTitle);
        edtCompany = findViewById(R.id.edtJobCompany);
        edtLocation = findViewById(R.id.edtJobLocation);
        edtSalary = findViewById(R.id.edtJobSalary);
        edtSalaryMin = findViewById(R.id.edtJobSalaryMin);
        edtSalaryMax = findViewById(R.id.edtJobSalaryMax);
        edtDescription = findViewById(R.id.edtJobDescription);
        edtSource = findViewById(R.id.edtJobSource);
        btnSave = findViewById(R.id.btnSaveJob);

        editJobId = getIntent().getStringExtra("edit_job_id");
        if (editJobId != null) {
            setTitle(R.string.edit_job_title);
            loadExistingJob(editJobId);
        }

        btnSave.setOnClickListener(v -> saveJob());
    }

    private void loadExistingJob(String jobId) {
        btnSave.setEnabled(false);
        FirebaseDatabase.getInstance().getReference("stories_details").child(jobId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        btnSave.setEnabled(true);
                        Job job = snapshot.getValue(Job.class);
                        if (job == null) {
                            Toast.makeText(AddJobActivity.this, "Không tìm thấy tin cần sửa", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                        originalCreatedAt = job.getCreatedAt();
                        originalVisible = job.isVisible();
                        edtTitle.setText(job.getTitle());
                        edtCompany.setText(job.getCompany());
                        edtLocation.setText(job.getLocation());
                        edtSalary.setText(job.getSalary());
                        edtSalaryMin.setText(job.getSalaryMin() > 0 ? String.valueOf(job.getSalaryMin()) : "");
                        edtSalaryMax.setText(job.getSalaryMax() > 0 ? String.valueOf(job.getSalaryMax()) : "");
                        edtDescription.setText(job.getDescription());
                        edtSource.setText(job.getSource());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        btnSave.setEnabled(true);
                        Toast.makeText(AddJobActivity.this, "Lỗi tải tin: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveJob() {
        String title = edtTitle.getText().toString().trim();
        String company = edtCompany.getText().toString().trim();
        String location = edtLocation.getText().toString().trim();
        String salary = edtSalary.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String source = edtSource.getText().toString().trim();
        long salaryMin = parseLongOrZero(edtSalaryMin.getText().toString().trim());
        long salaryMax = parseLongOrZero(edtSalaryMax.getText().toString().trim());

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
                : FirebaseDatabase.getInstance().getReference("stories_index").push().getKey();
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

        // Bản rút gọn cho Dashboard riêng của chủ tin (đọc trực tiếp users/{uid}/jobs, không cần quét toàn bộ index).
        Map<String, Object> userJobData = new HashMap<>();
        userJobData.put("title", title);
        userJobData.put("company", company);
        userJobData.put("visible", visible);
        userJobData.put("createdAt", createdAt);

        Map<String, Object> multiPathUpdate = new HashMap<>();
        multiPathUpdate.put("/stories_index/" + jobId, indexData);
        multiPathUpdate.put("/stories_details/" + jobId, detailData);
        multiPathUpdate.put("/users/" + uid + "/jobs/" + jobId, userJobData);

        btnSave.setEnabled(false);
        FirebaseDatabase.getInstance().getReference().updateChildren(multiPathUpdate)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, isEdit ? "Đã cập nhật tin" : "Đã đăng tin", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
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
