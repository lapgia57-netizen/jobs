package com.example.jobfinder.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jobfinder.R;
import com.example.jobfinder.model.Job;

import java.util.ArrayList;
import java.util.List;

public class JobAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_JOB = 0;
    private static final int TYPE_LOADING = 1;

    public interface OnJobClickListener {
        void onJobClick(Job job);
    }

    private final List<Job> jobs = new ArrayList<>();
    private final OnJobClickListener listener;
    private boolean showLoadingFooter = false;

    public JobAdapter(OnJobClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Job> newJobs) {
        jobs.clear();
        if (newJobs != null) jobs.addAll(newJobs);
        notifyDataSetChanged();
    }

    public void appendList(List<Job> more) {
        if (more == null || more.isEmpty()) return;
        int start = jobs.size();
        jobs.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    public void setShowLoadingFooter(boolean show) {
        if (this.showLoadingFooter == show) return;
        this.showLoadingFooter = show;
        if (show) {
            notifyItemInserted(jobs.size());
        } else {
            notifyItemRemoved(jobs.size());
        }
    }

    public List<Job> getCurrentList() {
        return new ArrayList<>(jobs);
    }

    @Override
    public int getItemViewType(int position) {
        if (showLoadingFooter && position == jobs.size()) return TYPE_LOADING;
        return TYPE_JOB;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LOADING) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_loading, parent, false);
            return new LoadingVH(v);
        }
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_job, parent, false);
        return new JobVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof JobVH) {
            Job job = jobs.get(position);
            JobVH h = (JobVH) holder;
            h.tvTitle.setText(job.getTitle());
            h.tvCompany.setText(job.getCompany() != null ? job.getCompany() : "");
            String loc = job.getLocation();
            h.tvLocation.setText(loc != null && !loc.isEmpty() ? "📍 " + loc : "📍 Không rõ");
            String salary = job.getSalary();
            if (salary != null && !salary.isEmpty()) {
                h.tvSalary.setVisibility(View.VISIBLE);
                h.tvSalary.setText(salary);
            } else {
                h.tvSalary.setVisibility(View.GONE);
            }
            h.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onJobClick(job);
            });
        }
    }

    @Override
    public int getItemCount() {
        return jobs.size() + (showLoadingFooter ? 1 : 0);
    }

    static class JobVH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCompany, tvLocation, tvSalary;

        JobVH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCompany = itemView.findViewById(R.id.tvCompany);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvSalary = itemView.findViewById(R.id.tvSalary);
        }
    }

    static class LoadingVH extends RecyclerView.ViewHolder {
        LoadingVH(@NonNull View itemView) {
            super(itemView);
        }
    }
}
