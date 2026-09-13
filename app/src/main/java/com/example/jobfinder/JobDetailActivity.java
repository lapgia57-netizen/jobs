package com.example.jobfinder;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.jobfinder.model.Job;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class JobDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvCompany, tvLocation, tvSalary, tvDescription, tvSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTitle = findViewById(R.id.tvDetailTitle);
        tvCompany = findViewById(R.id.tvDetailCompany);
        tvLocation = findViewById(R.id.tvDetailLocation);
        tvSalary = findViewById(R.id.tvDetailSalary);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvSource = findViewById(R.id.tvDetailSource);

        String jobId = getIntent().getStringExtra("job_id");
        if (TextUtils.isEmpty(jobId)) {
            Toast.makeText(this, "Không tìm thấy tin", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseDatabase.getInstance().getReference("stories_details").child(jobId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Job job = snapshot.getValue(Job.class);
                        if (job == null) {
                            Toast.makeText(JobDetailActivity.this, "Tin không tồn tại", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        tvTitle.setText(job.getTitle());
                        tvCompany.setText(job.getCompany());
                        String loc = job.getLocation();
                        tvLocation.setText(TextUtils.isEmpty(loc) ? "" : "📍 " + loc);
                        String salary = job.getSalary();
                        if (TextUtils.isEmpty(salary)) {
                            tvSalary.setVisibility(android.view.View.GONE);
                        } else {
                            tvSalary.setVisibility(android.view.View.VISIBLE);
                            tvSalary.setText(salary);
                        }
                        tvDescription.setText(TextUtils.isEmpty(job.getDescription())
                                ? "Không có mô tả" : job.getDescription());
                        String src = job.getSource();
                        tvSource.setText(TextUtils.isEmpty(src) ? "" : "Nguồn: " + src);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(JobDetailActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
