package com.example.jobfinder.model;

/**
 * Model tương ứng với 1 tin tuyển dụng, được lưu song song ở 3 nơi:
 *  - stories_index/{id}   : bản rút gọn để hiển thị danh sách công khai nhanh
 *  - stories_details/{id} : bản đầy đủ, chỉ tải khi xem chi tiết
 *  - users/{ownerUid}/jobs/{id} : bản rút gọn dùng riêng cho Dashboard của
 *                                 chủ tin, để không phải quét (scan) toàn bộ
 *                                 stories_index mới lọc ra tin của mình.
 *
 * "visible" quyết định tin có xuất hiện ở màn hình "Tất cả việc làm" (công khai)
 * hay không. Ẩn tin KHÔNG xoá dữ liệu, chỉ ẩn khỏi danh sách công khai - chủ tin
 * vẫn thấy tin đó (kèm nhãn "Đã ẩn") trong Dashboard riêng của họ.
 */
public class Job {

    private String id;
    private String title;
    private String company;
    private String location;
    private String salary;      // chuỗi hiển thị, ví dụ "10-15 triệu"
    private long salaryMin;     // dùng để lọc tìm kiếm chính xác theo số
    private long salaryMax;
    private String description;
    private String source;
    private String createdBy;   // email người đăng
    private String ownerUid;    // uid người đăng (dùng để phân quyền + lọc dashboard)
    private boolean visible;    // true = hiển thị công khai, false = đã ẩn
    private long createdAt;

    public Job() {
        // Bắt buộc phải có constructor rỗng để Firebase deserialize
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public long getSalaryMin() { return salaryMin; }
    public void setSalaryMin(long salaryMin) { this.salaryMin = salaryMin; }

    public long getSalaryMax() { return salaryMax; }
    public void setSalaryMax(long salaryMax) { this.salaryMax = salaryMax; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
