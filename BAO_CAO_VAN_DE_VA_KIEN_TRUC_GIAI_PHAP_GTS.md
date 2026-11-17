# BÁO CÁO PHÂN TÍCH VẤN ĐỀ DOANH NGHIỆP & KIẾN TRÚC GIẢI PHÁP GTS HRM (AI BUILDATHON 2026)

---

## I. BỐI CẢNH DOANH NGHIỆP & 5 ĐIỂM NGHẼN CỐT LÕI (PROBLEM STATEMENTS)

**GTS Việt Nam** là doanh nghiệp công nghệ & vận hành quy mô đang mở rộng nhanh. Khảo sát thực tế nghiệp vụ quản trị nhân sự và hành chính tại GTS cho thấy **5 "nút thắt cổ chai" mang tính rủi ro tài chính và pháp lý cao** mà các phần mềm HRM truyền thống không giải quyết được:

```mermaid
flowchart TD
    subgraph PROBLEMS["5 Nút Thắt Cổ Chai Thực Tế Tại GTS Việt Nam"]
        P1["❌ 1. Mập Mờ Chính Sách & Nội Quy<br/>• HR quá tải trả lời hàng trăm câu hỏi lặp lại<br/>• Nhân viên không nắm rõ luật BHXH ốm đau 75%, đợt Blackout, thời hạn báo trước"]
        P2["❌ 2. Gian Lận Chứng Từ & Nghỉ Ốm Khống<br/>• Giấy khám bệnh photoshop, giả con dấu bác sĩ<br/>• Lấy tên người ngoài nộp đơn trục lợi<br/>• Tính sai ngày công (0.5 công, trùng ngày lễ)"]
        P3["❌ 3. Gian Lận Bẫy Hợp Đồng Lao Động<br/>• Sửa lén mức lương 15tr ➔ 25tr trên bản ký giấy<br/>• Tẩy xóa thời hạn báo trước 30 ngày ➔ 0 ngày<br/>• HR thủ công không thể dò từng dòng"]
        P4["❌ 4. Nguy Cơ Vi Phạm Pháp Lý Dữ Liệu Nhạy Cảm<br/>• Luật BV DLCN số 91/2025/QH15 & NĐ 356/2025/NĐ-CP<br/>• Bệnh án y tế lưu dạng plain text, không mã hóa<br/>• Thiếu vết kiểm toán (Audit Trail) khi bị thanh tra"]
        P5["❌ 5. Rủi Ro Hệ Thống Khi Tích Hợp AI<br/>• Mạng chập chờn, API timeout hoặc nghẽn quota<br/>• Khiến đơn từ bị treo, mất mát dữ liệu, sập tiến trình"]
    end
```

---

## II. SƠ ĐỒ KIẾN TRÚC TỔNG THỂ HỆ THỐNG (ENTERPRISE ARCHITECTURE)

Hệ thống được thiết kế theo mô hình **Multi-Layer Enterprise Architecture** khép kín, kết hợp sức mạnh xử lý của Spring Boot 3, cơ sở dữ liệu Microsoft SQL Server 2022, mô hình đa phương thức Google Gemini AI và lớp bảo vệ dữ liệu chuẩn pháp chế Việt Nam.

```mermaid
flowchart TB
    subgraph CLIENT["1. Presentation Layer (Giao Diện Trực Quan)"]
        UI_TAB0["Tab 0: GTS Policy Copilot<br/>(Hỏi đáp chính sách & Action 1-chạm)"]
        UI_TAB1["Tab 1: Giám Định Y Tế & Chấm Công<br/>(Vision OCR, 0.5 công, Dynamic Holidays)"]
        UI_TAB2["Tab 2: AI Contract Diff-Check<br/>(So khớp song song, cảnh báo bẫy lương)"]
        UI_TAB3["Tab 3: Hàng Đợi Chống Sập & Kiểm Toán<br/>(DLQ Monitor, Drain Worker, Audit Logs)"]
    end

    subgraph API_GATEWAY["2. Controller & Security Layer"]
        CORS["CORS & Origin Filter<br/>(Hỗ trợ cả Port 8088 & Live Server 5500)"]
        AUTH["Role-Based Access Control<br/>(ROLE_HR_CNB, ROLE_EMPLOYEE)"]
        CRYPTO["Lớp Mã Hóa AES-GCM<br/>(Data-at-Rest Encryption)"]
    end

    subgraph SERVICES["3. Business Logic & AI Integration Layer"]
        SVC_COPILOT["PolicyCopilotService<br/>(Chuẩn hóa tiếng Việt, RAG Context)"]
        SVC_VISION["VisionForensicService<br/>(Pháp y chứng từ, Bóc tách thực thể)"]
        SVC_DIFF["ContractDiffService<br/>(Side-by-Side Diff, Bẫy lương & Báo trước)"]
        SVC_QUEUE["ResilientQueueService<br/>(@Retryable Backoff, Dead Letter Queue)"]
        SVC_TIMESHEET["TimesheetService<br/>(Trừ phép 0.5 công, Bỏ qua ngày lễ BLLĐ 2019)"]
    end

    subgraph EXTERNAL_AI["4. AI Cloud Service"]
        GEMINI_VISION["Google Gemini 1.5/2.0 Vision<br/>(Đa phương thức: OCR, Con Dấu, Chữ Ký)"]
        GEMINI_TEXT["Google Gemini Language Model<br/>(Trích dẫn Luật Lao Động & Nội Quy)"]
    end

    subgraph PERSISTENCE["5. Persistence Layer (Microsoft SQL Server)"]
        DB_POLICIES[("chinh_sach_nhan_su<br/>(NVARCHAR Unicode)")]
        DB_LEAVES[("don_xin_phep<br/>(Mã hóa bệnh án)")]
        DB_CONTRACTS[("hop_dong_lao_dong<br/>(JSON gốc vs JSON scan)")]
        DB_DLQ[("dead_letter_queue<br/>(Lưu tác vụ lỗi chờ khôi phục)")]
        DB_AUDIT[("lich_su_truy_cap<br/>(Audit Logs Luật 91/2025/QH15)")]
    end

    CLIENT --> API_GATEWAY
    API_GATEWAY --> SERVICES
    SERVICES <--> EXTERNAL_AI
    SERVICES --> PERSISTENCE
```

---

## III. SƠ ĐỒ CHI TIẾT 4 PHÂN HỆ GIẢI QUYẾT BÀI TOÁN

### 1. Phân Hệ 1: Cổng Tra Cứu Quy Chế & Nội Quy (GTS Policy Copilot)
* **Giải quyết Vấn đề 1:** Giải phóng 90% thời gian của phòng Nhân sự. Trả lời chính xác từng điều khoản, trích dẫn văn bản pháp luật hiện hành và cung cấp nút bấm thao tác 1-chạm đưa nhân viên tới đúng nghiệp vụ.

```mermaid
sequenceDiagram
    autonumber
    actor NV as Nhân Viên
    participant UI as Giao Diện Tab 0
    participant SVC as PolicyCopilotService
    participant DB as SQL Server (chinh_sach_nhan_su)
    participant AI as Gemini Model

    NV->>UI: Đặt câu hỏi ("Nghỉ ốm hưởng bao nhiêu lương?", "Đợt Blackout có được nghỉ?")
    UI->>SVC: Gửi POST /api/policy/ask
    SVC->>SVC: Normalizer bỏ dấu tiếng Việt để đối sánh từ khóa thông minh
    SVC->>DB: Truy vấn điều khoản liên quan (BHXH, Nghỉ phép, Blackout)
    SVC->>AI: Nạp Context điều khoản + Câu hỏi người dùng
    AI-->>SVC: Sinh câu trả lời chuẩn xác + Trích dẫn căn cứ luật
    SVC-->>UI: Trả về JSON: Tóm tắt, Chi tiết, Căn cứ pháp lý, Action Button
    UI-->>NV: Hiển thị giao diện trực quan + Nút hành động 1-click (vd: Tới Tab Giám định y tế)
```

---

### 2. Phân Hệ 2: Giám Định Chứng Từ Y Tế & Chống Gian Lận Đơn Từ (Forensic AI & Anti-Fraud)
* **Giải quyết Vấn đề 2 & Vấn đề 4:** Phát hiện con dấu giả, chữ ký bác sĩ, kiểm tra chéo tên người nộp với hồ sơ nhân sự công ty, tự động kích hoạt **Smart Bypass** trong đợt Blackout và mã hóa bệnh án theo **Luật BV DLCN số 91/2025/QH15 & NĐ 356/2025/NĐ-CP**.

```mermaid
flowchart TD
    START(["Nhân viên nộp ảnh Giấy ra viện / Giấy nghỉ ốm"]) --> OCR["Gemini Vision AI: Bóc tách OCR Siêu Dữ Liệu"]
    
    OCR --> EXTRACT["Bóc tách: Họ tên BN, Cơ sở y tế, Chẩn đoán, Ngày chỉ định, Con dấu đỏ, Chữ ký bác sĩ"]
    
    EXTRACT --> CHECK_FRAUD{"Kiểm tra chéo họ tên<br/>(Anti-Fraud Cross-Check)?"}
    CHECK_FRAUD -- "Tên trên giấy KHÔNG TRÙNG với nhân viên" --> REJECT_FRAUD["❌ CỜ ĐỎ: TỪ CHỐI TỨC THÌ<br/>Cảnh báo gian lận nộp khống chứng từ"]
    
    CHECK_FRAUD -- "Họ tên trùng khớp" --> CHECK_BLACKOUT{"Thời gian xin nghỉ có rơi vào<br/>Blackout Period (10/11 - 20/11)?"}
    
    CHECK_BLACKOUT -- "KHÔNG" --> CALC_LEAVE["Hợp lệ: Tính trừ ngày công"]
    
    CHECK_BLACKOUT -- "CÓ" --> CHECK_EMERGENCY{"Chẩn đoán có thuộc diện CẤP CỨU<br/>(Độ tin cậy AI >= 70%)?"}
    
    CHECK_EMERGENCY -- "Cấp cứu / Khẩn cấp" --> SMART_BYPASS["⚡ KÍCH HOẠT SMART BYPASS:<br/>Không bị chặn, chuyển thẳng Cấp Giám Đốc phê duyệt"]
    CHECK_EMERGENCY -- "Ốm nhẹ / Nghỉ phép thường" --> AUTO_BLOCK["🔒 CHẶN TỰ ĐỘNG:<br/>Bị chặn do quy chế đợt cao điểm dự án"]
    
    CALC_LEAVE --> HOLIDAY_CHECK["Trừ ngày nghỉ lễ động & Thứ 7/CN (BLLĐ 2019) + Tính 0.5 công"]
    
    HOLIDAY_CHECK --> AES_ENCRYPT["Mã hóa AES bệnh án & chẩn đoán y tế nhạy cảm"]
    AES_ENCRYPT --> SAVE_DB[("Ghi vào SQL Server: don_xin_phep & lich_su_truy_cap")]
    SAVE_DB --> FINISH(["Hoàn tất quy trình giám định minh bạch"])
```

---

### 3. Phân Hệ 3: AI Contract Diff-Check (So Khớp Chống Sửa Lén Hợp Đồng)
* **Giải quyết Vấn đề 3:** Ngăn chặn tuyệt đối việc nhân viên hoặc đối tác sửa lén điều khoản bất lợi trước khi ký tay và nộp lại bản scan.

```mermaid
flowchart LR
    subgraph DRAFT["Bản Thảo Gốc (Cơ Sở Dữ Liệu)"]
        D1["Lương: 15.000.000 VNĐ"]
        D2["Báo trước: 30 ngày"]
        D3["Chức vụ: Senior Developer"]
        D4["Địa điểm: Tầng 5, Tòa GTS"]
    end

    subgraph SCANNED["Bản Scan Ký Sống (Tải Lên)"]
        S1["Lương: 25.000.000 VNĐ ⚠️"]
        S2["Báo trước: 0 ngày ⚠️"]
        S3["Chức vụ: Senior Developer"]
        S4["Địa điểm: Tầng 5, Tòa GTS"]
    end

    subgraph ENGINE["AI Diff-Check Engine"]
        COMPARE{"Thuật Toán So Khớp Thực Thể Song Song<br/>(Side-by-Side Entity Matching)"}
    end

    subgraph REPORT["Báo Cáo Giám Định Hợp Đồng"]
        REP_SALARY["🔴 CẢNH BÁO BẪY LƯƠNG: Bị tăng khống +10.000.000 VNĐ!"]
        REP_NOTICE["🔴 CẢNH BÁO THỜI HẠN: Bị xóa thời hạn báo trước về 0 ngày!"]
        REP_ACTION["⛔ HỦY PHÊ DUYỆT & LẬP BIÊN BẢN SAI PHẠM"]
    end

    DRAFT --> COMPARE
    SCANNED --> COMPARE
    COMPARE --> REPORT
```

---

### 4. Phân Hệ 4: Hàng Đợi Chống Sập & Khôi Phục Sự Cố (Dead Letter Queue - DLQ)
* **Giải quyết Vấn đề 5:** Bảo đảm nguyên tắc **Zero Data Loss** (Không bao giờ mất đơn của nhân viên), hệ thống tự phục hồi tự động khi API AI gặp sự cố mạng hoặc nghẽn quota.

```mermaid
stateDiagram-v2
    [*] --> NhậnTácVụ: Đơn xin phép / Giám định chứng từ
    NhậnTácVụ --> GọiGeminiAI: Gửi yêu cầu qua REST API
    
    state GọiGeminiAI {
        [*] --> Lần1
        Lần1 --> Lần2: Timeout / Lỗi mạng (Chờ 2s)
        Lần2 --> Lần3: Quá tải Quota (Chờ 4s)
    }

    GọiGeminiAI --> ThànhCông: Xử lý thành công trong 3 lần thử
    ThànhCông --> LưuDữLiệu: Ghi kết quả vào SQL Server
    
    GọiGeminiAI --> ĐẩyVàoDLQ: Thất bại cả 3 lần (@Recover)
    
    state ĐẩyVàoDLQ {
        [*] --> GhiBảngDeadLetterQueue: Lưu Payload + Chi tiết lỗi
        GhiBảngDeadLetterQueue --> TrạngTháiPENDING: Đơn được bảo toàn an toàn
    }

    TrạngTháiPENDING --> ScheduledDrainWorker: Tự động chạy định kỳ mỗi 60s
    TrạngTháiPENDING --> ManualDrain: Quản trị viên bấm nút Drain thủ công
    
    ScheduledDrainWorker --> XửLýLại: Khôi phục mạng
    ManualDrain --> XửLýLại
    
    XửLýLại --> ChuyểnRESOLVED: Cập nhật trạng thái RESOLVED
    ChuyểnRESOLVED --> [*]
    LưuDữLiệu --> [*]
```

---

## IV. MA TRẬN ĐỐI CHIẾU 5 TIÊU CHÍ BÀI TOÁN AI BUILDATHON 2026

| STT | Bài toán gợi ý từ Ban Tổ Chức | Phân hệ giải quyết trong GTS HRM | Điểm kỹ thuật & Nghiệp vụ nổi bật |
| :---: | :--- | :--- | :--- |
| **01** | **Tra cứu chính sách nhân sự** | **Tab 0: GTS Policy Copilot** | • Xử lý ngôn ngữ tự nhiên Tiếng Việt (có/không dấu)<br/>• Trích dẫn chính xác Luật Lao Động 2019, Luật BHXH 2024<br/>• Tích hợp nút hành động 1-chạm điều hướng nghiệp vụ |
| **02** | **Số hóa hồ sơ nhân sự** | **Tab 1: Giám định chứng từ & OCR** | • Vision AI đọc giấy khám bệnh/giấy ra viện<br/>• Bóc tách chuẩn hóa thông tin ngày điều trị, cơ sở y tế<br/>• Mã hóa chuẩn bảo vệ dữ liệu nhạy cảm AES |
| **03** | **Quản lý hợp đồng lao động** | **Tab 2: AI Contract Diff-Check** | • Bóc tách song song bản scan chữ ký sống và bản thảo gốc<br/>• Bắt trọn bẫy nâng khống lương và giảm thời hạn thôi việc<br/>• Nhắc hạn hợp đồng tự động |
| **04** | **Quản lý đơn từ hành chính** | **Tab 1: Phân loại & Chấm công tự động** | • **Anti-Fraud:** Kiểm tra chéo họ tên nhân viên với CSDL<br/>• **Smart Bypass:** Tự động mở đường cho ca Cấp Cứu trong đợt Blackout<br/>• Tính công chuẩn nửa ngày (0.5 công) và trừ ngày lễ động |
| **05** | **Thống kê & Quản trị rủi ro** | **Tab 3: Dead Letter Queue & Audit** | • Cơ chế chống sập với `@Retryable` & Dead Letter Queue<br/>• Worker tự động dọn hàng đợi khi mạng phục hồi<br/>• Báo cáo kiểm toán minh bạch tuân thủ Luật 91/2025/QH15 |

---

## V. KẾT LUẬN & ĐIỂM SÁNG TRANH GIẢI (KEY COMPETITIVE ADVANTAGES)

1. **Tính Thực Chiến Cao (Enterprise-Ready):** Không dừng lại ở một bản demo gọi API đơn giản, hệ thống giải quyết trọn vẹn bài toán vận hành thực tế của một doanh nghiệp công nghệ tại Việt Nam.
2. **Tuân Thủ Pháp Chế Toàn Diện:** Tích hợp sâu các quy định mới nhất của **Bộ luật Lao động 2019**, **Luật Bảo hiểm Xã hội 2024**, và đặc biệt là quy định nghiêm ngặt về dữ liệu cá nhân nhạy cảm trong y tế của **Luật Bảo vệ Dữ liệu Cá nhân số 91/2025/QH15 & Nghị định 356/2025/NĐ-CP**.
3. **Kiến Trúc Chống Sập (Resilient Architecture):** Trang bị cơ chế Dead Letter Queue chuẩn enterprise, đảm bảo doanh nghiệp vận hành liên tục không gián đoạn kể cả khi các dịch vụ AI bên ngoài gặp sự cố mạng.
4. **Vibe Coding & Trải Nghiệm Lập Trình Thần Tốc:** Toàn bộ sản phẩm được phát triển đồng bộ từ Backend Spring Boot 3, SQL Server, Frontend Responsive trên VS Code và tối ưu hóa 100% cho Live Server.
