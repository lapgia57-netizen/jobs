package com.example.jobfinder;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Bật disk persistence để dữ liệu đã tải được cache trên máy,
 * giúp danh sách hiển thị mượt (mở app lại vẫn thấy data cũ ngay, rồi sync).
 */
public class JobFinderApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Phải gọi trước bất kỳ getInstance() nào khác
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
