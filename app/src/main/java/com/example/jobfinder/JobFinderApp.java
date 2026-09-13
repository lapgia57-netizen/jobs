package com.example.jobfinder;

import android.app.Application;

import com.example.jobfinder.util.FirebaseConfig;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Bật disk persistence để dữ liệu đã tải được cache trên máy,
 * giúp danh sách hiển thị mượt (mở app lại vẫn thấy data cũ ngay, rồi sync).
 */
public class JobFinderApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Phải gọi trước bất kỳ getInstance() nào khác.
        // Dùng getInstance(URL) tường minh thay vì getInstance() không tham số,
        // vì google-services.json mẫu chưa có "firebase_url" -> getInstance()
        // không tham số sẽ crash ngay tại đây (IllegalStateException).
        FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL).setPersistenceEnabled(true);
    }
}
