/**
 * import_jobs.js
 * ----------------------------------------------------------------------
 * Script CHẠY PHÍA SERVER (không đóng gói vào app Android) để nạp dữ liệu
 * việc làm vào Firebase Realtime Database một cách nhanh và ổn định.
 *
 * Vì sao chạy ở server, không chạy trong app di động:
 *  - Dùng firebase-admin => ghi thẳng bằng quyền admin, không bị chặn bởi
 *    Realtime Database Rules (auth != null...), không cần đăng nhập giả
 *    trên máy người dùng.
 *  - Có thể chạy định kỳ (cron / GitHub Actions schedule) độc lập với app,
 *    người dùng mở app lên là đã thấy dữ liệu mới, không tốn pin/băng
 *    thông của điện thoại để tự scrape.
 *  - Dùng updateChildren() để ghi nhiều tin trong 1 lần gọi mạng => nhanh
 *    hơn nhiều so với ghi từng tin một.
 *
 * LƯU Ý QUAN TRỌNG VỀ NGUỒN DỮ LIỆU:
 *  - Facebook: KHÔNG scrape trực tiếp trang/fanpage bằng cách giả lập
 *    trình duyệt hoặc dùng cookie cá nhân — vi phạm Điều khoản dịch vụ của
 *    Meta và có thể khiến tài khoản bị khóa hoặc dính vấn đề pháp lý.
 *    Nếu muốn lấy tin từ Facebook, hãy dùng Facebook Graph API chính thức
 *    với một Page bạn quản lý (cần Page Access Token hợp lệ).
 *  - Các trang tuyển dụng khác: ưu tiên dùng API công khai / RSS feed nếu
 *    trang đó cung cấp, và luôn đọc kỹ Điều khoản sử dụng trước khi lấy
 *    dữ liệu tự động.
 *  - Dưới đây là bộ khung (framework) chung: bạn tự thay hàm fetchFromX()
 *    bằng nguồn dữ liệu hợp lệ mà bạn có quyền sử dụng.
 *
 * Cài đặt:
 *   npm install firebase-admin node-fetch
 *
 * Chạy:
 *   GOOGLE_APPLICATION_CREDENTIALS=./serviceAccountKey.json node import_jobs.js
 */

const admin = require("firebase-admin");

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  databaseURL: "https://quyangianghokhongmangthesu-default-rtdb.asia-southeast1.firebasedatabase.app",
});

const db = admin.database();

/**
 * Chuẩn hoá 1 tin tuyển dụng thô về đúng cấu trúc app đang dùng
 * (đối chiếu model Job.java).
 */
// UID cố định đại diện cho "tài khoản bot import" - nên tạo 1 user Firebase Auth
// thật cho bot này (Console -> Authentication -> Add user) rồi thay uid dưới đây,
// để tin import cũng xuất hiện gọn trong 1 Dashboard riêng, dễ quản lý/ẩn hàng loạt.
const IMPORT_BOT_UID = "REPLACE_WITH_BOT_UID";
const IMPORT_BOT_EMAIL = "import-bot@jobfinder.app";

function normalizeJob(raw) {
  return {
    title: raw.title || "",
    company: raw.company || "",
    location: raw.location || "",
    salary: raw.salary || "Thỏa thuận",
    salaryMin: raw.salaryMin || 0,
    salaryMax: raw.salaryMax || 0,
    description: raw.description || "",
    source: raw.source || "",
    createdBy: IMPORT_BOT_EMAIL,
    ownerUid: IMPORT_BOT_UID,
    visible: true,
    createdAt: Date.now(),
  };
}

/**
 * VÍ DỤ: lấy dữ liệu từ một API/RSS công khai mà bạn được phép dùng.
 * Thay nội dung hàm này bằng nguồn thật của bạn.
 */
async function fetchFromPublicSource() {
  // const res = await fetch("https://api.vi-du-trang-tuyen-dung.com/jobs/latest");
  // const data = await res.json();
  // return data.items.map(item => ({
  //   title: item.job_title,
  //   company: item.company_name,
  //   location: item.city,
  //   salary: item.salary_text,
  //   description: item.summary,
  //   source: item.url,
  // }));

  // Dữ liệu mẫu để bạn test luồng ghi Firebase trước khi nối nguồn thật:
  return [
    {
      title: "Nhân viên IT Helpdesk",
      company: "Công ty ABC",
      location: "Hà Nội",
      salary: "10-13 triệu",
      description: "Hỗ trợ kỹ thuật nội bộ, xử lý sự cố máy tính, mạng.",
      source: "https://example.com/job/12345",
    },
  ];
}

/**
 * Ghi nhiều tin cùng lúc bằng 1 multi-path update -> nhanh nhất có thể
 * trong giới hạn của Realtime Database.
 */
async function bulkImport(jobs) {
  const updates = {};
  const indexRef = db.ref("stories_index");

  for (const raw of jobs) {
    const job = normalizeJob(raw);
    const newKey = indexRef.push().key;

    updates[`/stories_index/${newKey}`] = {
      title: job.title,
      company: job.company,
      location: job.location,
      createdBy: job.createdBy,
      ownerUid: job.ownerUid,
      visible: job.visible,
      createdAt: job.createdAt,
    };
    updates[`/stories_details/${newKey}`] = job;
    updates[`/users/${job.ownerUid}/jobs/${newKey}`] = {
      title: job.title,
      company: job.company,
      visible: job.visible,
      createdAt: job.createdAt,
    };
  }

  await db.ref().update(updates);
  console.log(`Đã nạp ${jobs.length} tin thành công.`);
}

(async () => {
  try {
    const jobs = await fetchFromPublicSource();
    await bulkImport(jobs);
    process.exit(0);
  } catch (err) {
    console.error("Lỗi khi nạp dữ liệu:", err);
    process.exit(1);
  }
})();
