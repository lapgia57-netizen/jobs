package com.example.jobfinder.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jobfinder.R;
import com.example.jobfinder.model.Job;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class DashboardJobAdapter extends RecyclerView.Adapter<DashboardJobAdapter.VH> {

    public interface Callback {
        void onToggleVisible(Job job, boolean makeVisible);
        void onEdit(Job job);
        void onDelete(Job job);
        void onClick(Job job);
    }

    private final List<Job> jobs = new ArrayList<>();
    private final Callback callback;

    public DashboardJobAdapter(Callback callback) {
        this.callback = callback;
    }

    public void submitList(List<Job> newJobs) {
        jobs.clear();
        jobs.addAll(newJobs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_job, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Job job = jobs.get(position);
        holder.tvTitle.setText(job.getTitle());
        holder.tvCompany.setText(job.getCompany());
        holder.tvStatus.setText(job.isVisible() ? "Đang hiển thị công khai" : "Đã ẩn");

        // Gỡ listener cũ trước khi setChecked để tránh trigger callback ngoài ý muốn khi bind lại.
        holder.switchVisible.setOnCheckedChangeListener(null);
        holder.switchVisible.setChecked(job.isVisible());
        holder.switchVisible.setOnCheckedChangeListener((buttonView, isChecked) ->
                callback.onToggleVisible(job, isChecked));

        holder.btnEdit.setOnClickListener(v -> callback.onEdit(job));
        holder.btnDelete.setOnClickListener(v -> callback.onDelete(job));
        holder.itemView.setOnClickListener(v -> callback.onClick(job));
    }

    @Override
    public int getItemCount() {
        return jobs.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCompany, tvStatus;
        SwitchMaterial switchVisible;
        ImageButton btnEdit, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCompany = itemView.findViewById(R.id.tvCompany);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            switchVisible = itemView.findViewById(R.id.switchVisible);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
