# JobFinder — App việc làm (Java, Firebase)

## Cấu trúc dữ liệu Firebase (đã mở rộng: node user, ẩn/hiện, tìm kiếm chặt chẽ)
Mỗi tin tuyển dụng được lưu **song song ở 3 nơi** bằng đúng 1 lệnh `updateChildren()` (multi-path update, chỉ 1 round-trip mạng):

- `stories_index/{id}`: bản **nhẹ** (title, company, location, createdBy, ownerUid, visible, createdAt) — dùng cho màn hình "Tất cả việc làm" (public, mọi user), tải nhanh vì không kéo theo mô tả dài.
- `stories_details/{id}`: bản **đầy đủ** (thêm salary, salaryMin, salaryMax, description, source, ownerUid, visible) — chỉ tải khi xem chi tiết hoặc sửa tin.
- `users/{ownerUid}/jobs/{id}`: bản **rút gọn riêng cho Dashboard** của từng tài khoản (title, company, visible, createdAt) — Dashboard chỉ cần đọc đúng 1 đường dẫn `users/{uid}/jobs`, không phải quét (scan) toàn bộ `stories_index` rồi lọc theo `createdBy` như cách cũ → nhanh hơn nhiều khi dữ liệu lớn.

`visible` (true/false) quyết định tin có xuất hiện ở màn hình công khai hay không — **ẩn tin không xoá dữ liệu**, chủ tin vẫn thấy và quản lý được trong Dashboard riêng.

Rule (`database.rules.json`) đã được siết chặt hơn so với bản gốc: dùng `ownerUid` (uid) thay vì so email, để nhất quán và khó giả mạo hơn; thêm quyền ghi cho `users/{uid}/jobs/{id}` chỉ khi `auth.uid == $uid`. **Nhớ deploy file `database.rules.json` mới này** trong Firebase Console → Realtime Database → Rules.

## Dashboard riêng từng tài khoản (`DashboardActivity`)
- Chỉ hiển thị tin của chính người đang đăng nhập (đọc `users/{uid}/jobs`).
- Mỗi tin có **công tắc ẩn/hiện** (`SwitchMaterial`) — bật/tắt sẽ cập nhật `visible` ở cả 3 nơi cùng lúc bằng 1 multi-path update, nên màn hình "Tất cả việc làm" phản ánh ngay lập tức (nhờ `ChildEventListener` lắng nghe `onChildChanged`).
- Nút **Sửa** mở lại `AddJobActivity` ở chế độ chỉnh sửa (nạp sẵn dữ liệu cũ, ghi đè đúng 3 node thay vì tạo tin mới).
- Nút **Xoá** xoá tin khỏi cả 3 node sau khi xác nhận.

## Màn hình "Tất cả việc làm" (`MainActivity`) — liệt kê tin của TẤT CẢ user
- Đọc `stories_index` (toàn bộ, mọi user đã đăng), chỉ hiển thị tin có `visible == true`.
- Có ô **Tìm kiếm chặt chẽ** (xem mục bên dưới).

## Chức năng tìm kiếm chặt chẽ nhất
Nguyên tắc: **mọi điều kiện đã nhập đều phải đúng đồng thời (AND)** — không có kiểu "khớp 1 trong nhiều điều kiện" làm loãng kết quả:
1. **Từ khoá**: tách thành từng từ, TẤT CẢ các từ phải xuất hiện trong (tiêu đề + công ty + địa điểm) sau khi chuẩn hoá (bỏ dấu tiếng Việt, viết thường, gộp khoảng trắng thừa — xem `util/SearchUtils.java`). Nhờ chuẩn hoá, gõ "ha noi" hay "Hà Nội" đều cho cùng kết quả.
2. **Địa điểm**: nếu nhập, địa điểm của tin phải **chứa đúng** cụm đã nhập (so khớp trên dữ liệu đã chuẩn hoá).
3. **Lương tối thiểu mong muốn**: so sánh **số học chính xác** với `salaryMin` (không đoán từ chuỗi hiển thị như "10-15 triệu" — vì vậy `AddJobActivity` có thêm 2 ô nhập số `salaryMin`/`salaryMax` riêng, dùng để lọc, tách biệt với ô `salary` chỉ để hiển thị).

Bộ lọc chạy realtime khi gõ (TextWatcher), áp trên dữ liệu đã đồng bộ từ Firebase vào bộ nhớ (`masterJobs`), rồi đẩy kết quả đã lọc + sắp xếp mới nhất trước vào `JobAdapter` bằng `DiffUtil` để không giật danh sách.

## Việc cần làm trước khi build APK

1. **Đăng ký app Android trong Firebase Console** (bắt buộc):
   - Vào Firebase Console → dự án `quyangianghokhongmangthesu` → Add app → Android.
   - Package name: `com.example.jobfinder` (hoặc đổi lại theo ý bạn, nhớ sửa `applicationId` + `namespace` trong `app/build.gradle` và Manifest cho khớp).
   - Tải file `google-services.json` thật do Firebase Console sinh ra, **thay thế** file mẫu mình để sẵn trong `app/google-services.json` (file mẫu dựng từ config Web SDK bạn gửi nên thiếu `oauth_client` — sẽ không hoạt động đúng cho Android nếu không thay).
   - Trong Firebase Console → Authentication → bật **Email/Password** sign-in method.

2. **Sinh Gradle Wrapper** (repo hiện thiếu `gradlew` + `gradle-wrapper.jar` vì môi trường này không có mạng để tải):
   ```bash
   # Mở project bằng Android Studio 1 lần, hoặc chạy nếu máy bạn có sẵn Gradle:
   gradle wrapper --gradle-version 8.4
   git add gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar
   ```
   Sau bước này, workflow `.github/workflows/build-apk.yml` mới `./gradlew assembleDebug` chạy được trên GitHub Actions.

3. Push code lên GitHub → tab **Actions** sẽ tự build và cho tải file `app-debug.apk` ở phần Artifacts.

## Icon app
Icon adaptive (`ic_launcher_foreground.xml` + `ic_launcher_background.xml`, hình vali/túi xách màu trắng trên nền xanh `#1A73E8`) đã có sẵn, hoạt động từ Android 8.0 (API 26) trở lên. Để hỗ trợ máy Android cũ hơn (API 23–25, đúng theo `minSdk 23` mình đặt), mở Android Studio → chuột phải `res` → **New → Image Asset** → chọn `ic_launcher_foreground.xml` làm nguồn để Android Studio tự sinh PNG cho các mật độ màn hình (mdpi → xxxhdpi).

## Nạp dữ liệu từ nguồn ngoài (thay vì gõ tay từng tin)
- `scripts/import_jobs.js` là script Node.js chạy **trên máy chủ / máy tính của bạn** (không nhét vào app), dùng `firebase-admin` để ghi thẳng vào Realtime Database (bỏ qua rule vì có quyền admin), và ghi nhiều tin trong 1 lần gọi để nhanh nhất.
- **Về Facebook**: không nên scrape trực tiếp fanpage/bài đăng bằng cách giả lập trình duyệt hay dùng cookie cá nhân — vi phạm Điều khoản dịch vụ của Meta, rủi ro bị khóa tài khoản. Nếu có Page riêng, hãy dùng **Facebook Graph API** chính thức với Page Access Token.
- Với các trang tuyển dụng khác, ưu tiên nguồn có API/RSS công khai và đọc kỹ điều khoản sử dụng trước khi tự động lấy dữ liệu.
- Có thể lên lịch script này chạy định kỳ bằng GitHub Actions `schedule:` hoặc cron trên server riêng.

## Các màn hình đã code
- `LoginActivity` / `RegisterActivity`: đăng nhập, đăng ký bằng email/password (Firebase Auth) — bắt buộc đăng nhập xong mới vào `MainActivity`.
- `MainActivity`: danh sách **công khai** việc làm của **tất cả user** (chỉ tin `visible == true`), có ô tìm kiếm chặt chẽ theo từ khoá + địa điểm + lương tối thiểu. Menu góc trên có lối vào Dashboard và Đăng xuất.
- `DashboardActivity`: danh sách tin **của riêng tài khoản đang đăng nhập** (đọc `users/{uid}/jobs`), mỗi tin có công tắc ẩn/hiện, nút sửa, nút xoá.
- `JobDetailActivity`: xem chi tiết 1 tin (tải từ `stories_details`).
- `AddJobActivity`: đăng tin mới **hoặc sửa tin cũ** (nhận `edit_job_id` qua Intent), ghi/cập nhật đồng thời cả 3 node (`stories_index`, `stories_details`, `users/{uid}/jobs`) bằng 1 multi-path update.
- `util/SearchUtils`: chuẩn hoá chuỗi tiếng Việt (bỏ dấu, viết thường) phục vụ so khớp tìm kiếm chính xác.
