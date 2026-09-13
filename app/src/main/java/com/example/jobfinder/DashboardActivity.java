package com.example.jobfinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jobfinder.adapter.DashboardJobAdapter;
import com.example.jobfinder.model.Job;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

public class DashboardActivity extends AppCompatActivity implements DashboardJobAdapter.Callback {

    private RecyclerView recyclerView;
    private View emptyState;
    private DashboardJobAdapter adapter;
    private DatabaseReference myJobsRef;
    private ChildEventListener listener;
    private String uid;

    private final Map<String, Job> jobsMap = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        uid = user.getUid();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        emptyState = findViewById(R.id.emptyState);
        FloatingActionButton fab = findViewById(R.id.fabAdd);

        adapter = new DashboardJobAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(v ->
                startActivity(new Intent(this, AddJobActivity.class)));

        myJobsRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child("jobs");

        listener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                Job job = snapshot.getValue(Job.class);
                if (job == null) return;
                job.setId(snapshot.getKey());
                jobsMap.put(job.getId(), job);
                refreshList();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                Job job = snapshot.getValue(Job.class);
                if (job == null) return;
                job.setId(snapshot.getKey());
                jobsMap.put(job.getId(), job);
                refreshList();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                jobsMap.remove(snapshot.getKey());
                refreshList();
            }

            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        myJobsRef.addChildEventListener(listener);
    }

    private void refreshList() {
        List<Job> list = new ArrayList<>(jobsMap.values());
        Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        adapter.submitList(list);
        emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onToggleVisible(Job job, boolean makeVisible) {
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("/stories_index/" + job.getId() + "/visible", makeVisible);
        updates.put("/stories_details/" + job.getId() + "/visible", makeVisible);
        updates.put("/users/" + uid + "/jobs/" + job.getId() + "/visible", makeVisible);
        FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Không cập nhật được: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override
    public void onEdit(Job job) {
        Intent i = new Intent(this, AddJobActivity.class);
        i.putExtra("edit_job_id", job.getId());
        startActivity(i);
    }

    @Override
    public void onDelete(Job job) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setPositiveButton("Xoá", (d, w) -> doDelete(job))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void doDelete(Job job) {
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("/stories_index/" + job.getId(), null);
        updates.put("/stories_details/" + job.getId(), null);
        updates.put("/users/" + uid + "/jobs/" + job.getId(), null);
        FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                .addOnSuccessListener(u -> Toast.makeText(this, "Đã xoá tin", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Không xoá được: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override
    public void onClick(Job job) {
        Intent i = new Intent(this, JobDetailActivity.class);
        i.putExtra("job_id", job.getId());
        startActivity(i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (myJobsRef != null && listener != null) {
            myJobsRef.removeEventListener(listener);
        }
    }
}
