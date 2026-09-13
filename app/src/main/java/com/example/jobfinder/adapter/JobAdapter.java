package com.example.jobfinder.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jobfinder.R;
import com.example.jobfinder.model.Job;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter cho danh sách "Tất cả việc làm" (công khai, đã lọc theo tìm kiếm).
 * Dùng submitList() để thay toàn bộ dữ liệu hiển thị mỗi khi bộ lọc tìm kiếm
 * hoặc dữ liệu gốc thay đổi - kèm DiffUtil để chỉ re-bind đúng phần thay đổi,
 * tránh giật danh sách khi người dùng gõ tìm kiếm liên tục.
 */
public class JobAdapter extends RecyclerView.Adapter<JobAdapter.JobViewHolder> {

    public interface OnJobClickListener {
        void onJobClick(Job job);
    }

    private final List<Job> displayedJobs = new ArrayList<>();
    private final OnJobClickListener listener;

    public JobAdapter(OnJobClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Job> newJobs) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return displayedJobs.size(); }

            @Override
            public int getNewListSize() { return newJobs.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return displayedJobs.get(oldPos).getId().equals(newJobs.get(newPos).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                Job a = displayedJobs.get(oldPos);
                Job b = newJobs.get(newPos);
                return a.getTitle().equals(b.getTitle())
                        && a.getCompany().equals(b.getCompany())
                        && a.getLocation().equals(b.getLocation());
            }
        });
        displayedJobs.clear();
        displayedJobs.addAll(newJobs);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_job, parent, false);
        return new JobViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job = displayedJobs.get(position);
        holder.tvTitle.setText(job.getTitle());
        holder.tvCompany.setText(job.getCompany());
        holder.tvLocation.setText(job.getLocation());
        holder.itemView.setOnClickListener(v -> listener.onJobClick(job));
    }

    @Override
    public int getItemCount() {
        return displayedJobs.size();
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCompany, tvLocation;

        JobViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCompany = itemView.findViewById(R.id.tvCompany);
            tvLocation = itemView.findViewById(R.id.tvLocation);
        }
    }
}
