package com.example.jobfinder.util;

/**
 * Cấu hình Firebase dùng chung cho toàn app.
 *
 * VÌ SAO CẦN FILE NÀY (đây chính là nguyên nhân gây lỗi "không lưu được dữ liệu"):
 * Toàn bộ code cũ gọi FirebaseDatabase.getInstance() KHÔNG có tham số. Với cách gọi
 * đó, Firebase SDK bắt buộc phải đọc field "firebase_url" trong app/google-services.json
 * để biết địa chỉ Realtime Database. File google-services.json mẫu đi kèm repo này
 * KHÔNG có field đó (vì được dựng tay từ config Web, không tải từ Console) -> SDK
 * ném IllegalStateException ngay khi gọi getInstance(), nên MỌI lệnh đọc/ghi đều
 * fail từ trước khi kịp chạm .updateChildren().
 *
 * CÁCH SỬA TRIỆT ĐỂ: truyền thẳng URL vào FirebaseDatabase.getInstance(URL) ở khắp
 * nơi, không phụ thuộc vào việc google-services.json có field đó hay không.
 *
 * BẠN CẦN LÀM 1 VIỆC DUY NHẤT:
 * Vào Firebase Console -> chọn project "quyangianghokhongmangthesu" -> Realtime
 * Database -> copy đúng URL hiển thị trên đầu trang (dạng
 * https://<ten-project>-default-rtdb.<region>.firebasedatabase.app hoặc
 * https://<ten-project>.firebaseio.com) rồi dán thay cho giá trị bên dưới.
 */
public final class FirebaseConfig {

    // TODO: THAY BẰNG URL THẬT lấy từ Firebase Console (Realtime Database).
    // Đây là placeholder, KHÔNG phải URL thật -> nếu chưa thay, app vẫn sẽ báo lỗi
    // (nhưng sẽ là lỗi rõ ràng "network error / host not found" thay vì
    // IllegalStateException mơ hồ như trước).
    public static final String DATABASE_URL =
            "https://quyangianghokhongmangthesu-default-rtdb.asia-southeast1.firebasedatabase.app";

    private FirebaseConfig() {
        // no instances
    }
}
