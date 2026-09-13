package com.example.jobfinder;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jobfinder.model.Job;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class JobDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvCompany, tvLocation, tvSalary, tvDescription, tvSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_detail);

        tvTitle = findViewById(R.id.tvDetailTitle);
        tvCompany = findViewById(R.id.tvDetailCompany);
        tvLocation = findViewById(R.id.tvDetailLocation);
        tvSalary = findViewById(R.id.tvDetailSalary);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvSource = findViewById(R.id.tvDetailSource);

        String jobId = getIntent().getStringExtra("job_id");
        if (jobId == null) {
            finish();
            return;
        }

        DatabaseReference detailRef = FirebaseDatabase.getInstance()
                .getReference("stories_details").child(jobId);

        detailRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Job job = snapshot.getValue(Job.class);
                if (job != null) {
                    bind(job);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(JobDetailActivity.this, "Lỗi tải chi tiết: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bind(Job job) {
        tvTitle.setText(job.getTitle());
        tvCompany.setText(job.getCompany());
        tvLocation.setText(job.getLocation());
        tvSalary.setText(job.getSalary());
        tvDescription.setText(job.getDescription());
        tvSource.setText(job.getSource());
    }
}
