package com.gtsvn.hrm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtsvn.hrm.dto.PolicyAnswerResponseDTO;
import com.gtsvn.hrm.dto.PolicyQuestionRequestDTO;
import com.gtsvn.hrm.entity.ChinhSachNhanSu;
import com.gtsvn.hrm.entity.NhanVien;
import com.gtsvn.hrm.repository.ChinhSachNhanSuRepository;
import com.gtsvn.hrm.repository.NhanVienRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PolicyCopilotService {

    private final ChinhSachNhanSuRepository policyRepository;
    private final NhanVienRepository nhanVienRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.builder().build();

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-3.5-flash-lite}")
    private String modelName;

    @PostConstruct
    @Transactional
    public void initPolicies() {
        if (policyRepository.count() == 0) {
            log.info("Khởi tạo Cơ sở tri thức Chính sách & Nội quy Nhân sự GTS Việt Nam...");

            // 1. Nghỉ phép năm & Nghỉ việc riêng
            policyRepository.save(ChinhSachNhanSu.builder()
                    .maDieuKhoan("DIEU_14_PHEP_NAM")
                    .tieuDe("Điều 14: Quy chế nghỉ phép năm & nghỉ việc riêng")
                    .danhMuc("NGHI_PHEP")
                    .noiDungTomTat("Quy định số ngày phép năm theo thâm niên, nghỉ nửa ngày (0.5 công), thủ tục nộp đơn và chế độ nghỉ việc riêng hưởng nguyên 100% lương.")
                    .noiDungChiTiet("""
                            1. HẠN MỨC PHÉP NĂM (Điều 113 BLLĐ 2019):
                            - Nhân viên làm việc đủ 12 tháng được nghỉ 12 ngày phép năm hưởng nguyên lương.
                            - Cứ mỗi 05 năm làm việc tại GTS Việt Nam, số ngày nghỉ phép năm được tăng thêm tương ứng 01 ngày.
                            
                            2. NGHỈ NỬA NGÀY (0.5 CÔNG):
                            - Hệ thống GTS-HRM cho phép nhân viên đăng ký nghỉ Buổi sáng (08:30 - 12:00) hoặc Buổi chiều (13:30 - 17:30).
                            - Khi phê duyệt, hệ thống tự động trừ 0.5 ngày phép và chấm công 4.0h thực tế + 4.0h nghỉ phép (Mã công P/2).
                            
                            3. NGHỈ VIỆC RIÊNG HƯỞNG NGUYÊN LƯƠNG (Điều 115 BLLĐ 2019):
                            - Bản thân kết hôn: Nghỉ 03 ngày (Mã công VR).
                            - Con đẻ, con nuôi kết hôn: Nghỉ 01 ngày.
                            - Cha đẻ, mẹ đẻ, cha nuôi, mẹ nuôi; cha đẻ, mẹ đẻ của vợ/chồng; vợ hoặc chồng; con đẻ, con nuôi chết: Nghỉ 03 ngày.
                            
                            4. THỦ TỤC NỘP ĐƠN:
                            - Đơn xin nghỉ phép cần được gửi qua cổng nội bộ GTS-HRM trước tối thiểu 24 giờ (đối với nghỉ phép thông thường).
                            """)
                    .canCuPhapLy("Điều 113, 114, 115 Bộ luật Lao động 2019 số 45/2019/QH14")
                    .quyenLoiNhanVien("Hưởng 100% lương ngày nghỉ; tự động đồng bộ Timesheet; không bị trừ tiền chuyên cần nếu nộp đơn đúng quy định.")
                    .thuTucThucHien("Truy cập Cổng GTS-HRM -> Tab Giám Định Chứng Từ & Nghỉ Phép -> Điền ngày nghỉ, chọn Buổi nghỉ (Cả ngày/0.5 công) -> Gửi phê duyệt.")
                    .hanhDongLienKet("TAB_LEAVE")
                    .createdAt(LocalDateTime.now())
                    .build());

            // 2. Chế độ ốm đau & BHXH
            policyRepository.save(ChinhSachNhanSu.builder()
                    .maDieuKhoan("CHE_DO_OM_DAU_BHXH")
                    .tieuDe("Chế độ nghỉ ốm đau & trợ cấp Bảo hiểm Xã hội (BHXH)")
                    .danhMuc("BHXH")
                    .noiDungTomTat("Mức hưởng trợ cấp ốm đau 75% lương đóng BHXH, điều kiện hưởng, hồ sơ giấy tờ y tế hợp lệ theo Thông tư 56/BYT và bảo mật bệnh án theo Luật 91/2025/QH15.")
                    .noiDungChiTiet("""
                            1. MỨC HƯỞNG TRỢ CẤP (Luật BHXH 2024):
                            - Mức hưởng = (Tiền lương đóng BHXH của tháng liền kề / 24 ngày) x 75% x Số ngày nghỉ việc hưởng BHXH.
                            - Thời gian hưởng tối đa trong năm: 30 ngày (làm việc dưới 15 năm đóng BHXH), 40 ngày (từ 15 đến dưới 30 năm).
                            
                            2. YÊU CẦU CHỨNG TỪ Y TẾ HỢP LỆ:
                            - Điều trị nội trú: Giấy ra viện có mộc tròn đỏ của bệnh viện.
                            - Điều trị ngoại trú: Giấy chứng nhận nghỉ việc hưởng BHXH (Mẫu chuẩn theo Thông tư 56/2017/TT-BYT).
                            - CHỐNG GIAN LẬN: Họ tên người bệnh trên chứng từ phải trùng khớp 100% với hồ sơ nhân sự (hệ thống có AI đối soát chống mượn giấy tờ).
                            
                            3. BẢO MẬT DỮ LIỆU BỆNH ÁN:
                            - Theo Luật Bảo vệ Dữ liệu Cá nhân số 91/2025/QH15 và NĐ 356/2025/NĐ-CP, chẩn đoán bệnh tật là Dữ liệu cá nhân nhạy cảm, được hệ thống mã hóa AES Data-at-Rest. Chỉ HR C&B mới có quyền giải mã và mọi thao tác đều được lưu vết kiểm toán bất biến.
                            """)
                    .canCuPhapLy("Luật BHXH 2024; Thông tư 56/2017/TT-BYT; Luật Bảo vệ DLCN số 91/2025/QH15")
                    .quyenLoiNhanVien("Được cơ quan BHXH chi trả 75% lương trợ cấp ốm đau; ngày nghỉ không trừ vào quỹ phép năm.")
                    .thuTucThucHien("Tải ảnh chụp Giấy khám bệnh / Giấy ra viện lên Cổng GTS-HRM -> AI Vision tự động OCR bóc tách và đối soát -> HR duyệt và lập hồ sơ gửi BHXH.")
                    .hanhDongLienKet("TAB_LEAVE")
                    .createdAt(LocalDateTime.now())
                    .build());

            // 3. Quy định Blackout Period
            policyRepository.save(ChinhSachNhanSu.builder()
                    .maDieuKhoan("QUY_DINH_BLACKOUT")
                    .tieuDe("Quy định đợt cao điểm Blackout Period (10/11 - 20/11) & Cơ chế Smart Bypass Cấp Cứu")
                    .danhMuc("BLACKOUT")
                    .noiDungTomTat("Chính sách siết chặt ngày nghỉ trong đợt bàn giao dự án cao điểm quý 4, tự động chặn đơn nghỉ thường và cơ chế Bypass thông minh cho ca Cấp Cứu.")
                    .noiDungChiTiet("""
                            1. MỤC ĐÍCH BLACKOUT PERIOD:
                            - Đảm bảo 100% nhân sự chủ chốt tập trung release sản phẩm và nghiệm thu dự án trọng điểm cùng khách hàng đối tác trong khoảng thời gian từ 10/11 đến 20/11 hằng năm.
                            
                            2. NGUYÊN TẮC ÁP DỤNG:
                            - TỰ ĐỘNG CHẶN: Hệ thống tự động từ chối các đơn xin nghỉ phép năm thông thường, nghỉ không lương, nghỉ du lịch nếu lịch nghỉ giao thoa với dải ngày 10/11 - 20/11.
                            
                            3. CƠ CHẾ SMART BYPASS DÀNH CHO CA CẤP CỨU:
                            - Ngoại lệ nhân văn: Nếu nhân viên gặp sự cố y tế khẩn cấp (tai nạn, ngộ độc, viêm ruột thừa cấp hoại tử, phẫu thuật khẩn cấp) được bác sĩ chỉ định.
                            - AI Vision phát hiện cờ khẩn cấp (isKhanCap = true) sẽ tự động KÍCH HOẠT SMART BYPASS: không bị chặn bởi bộ lọc Blackout, chuyển thẳng đơn tới Tổng Giám Đốc phê duyệt ưu tiên đặc biệt.
                            """)
                    .canCuPhapLy("Quy chế Nội bộ GTS Việt Nam số 08/2026/QC-GTS; Điều 113 BLLĐ 2019")
                    .quyenLoiNhanVien("Bảo vệ tối đa quyền lợi sức khỏe người lao động trong trường hợp rủi ro bất khả kháng.")
                    .thuTucThucHien("Nếu là ca cấp cứu: Nộp ảnh Giấy cấp cứu/Phẫu thuật lên Cổng GTS-HRM -> AI tự động nhận diện từ khóa khẩn cấp và kích hoạt Smart Bypass.")
                    .hanhDongLienKet("TAB_LEAVE")
                    .createdAt(LocalDateTime.now())
                    .build());

            // 4. Quản lý Hợp đồng lao động & Chống sửa lén điều khoản
            policyRepository.save(ChinhSachNhanSu.builder()
                    .maDieuKhoan("QUY_CHE_HOP_DONG_LAO_DONG")
                    .tieuDe("Quy chế ký kết Hợp đồng lao động & Đối soát điều khoản pháp lý")
                    .danhMuc("HOP_DONG")
                    .noiDungTomTat("Quy trình giao kết hợp đồng, bảo mật mức lương, thời hạn báo trước khi chấm dứt HĐLĐ và công nghệ AI so sánh đối chiếu chữ ký sống chống sửa đổi trái phép.")
                    .noiDungChiTiet("""
                            1. LOẠI HỢP ĐỒNG LAO ĐỘNG (Điều 20 BLLĐ 2019):
                            - Hợp đồng lao động xác định thời hạn (từ 12 đến 36 tháng).
                            - Hợp đồng lao động không xác định thời hạn.
                            
                            2. THỜI HẠN BÁO TRƯỚC KHI ĐƠN PHƯƠNG CHẤM DỨT HĐLĐ (Điều 35 BLLĐ 2019):
                            - Hợp đồng không xác định thời hạn: Báo trước ít nhất 45 ngày.
                            - Hợp đồng xác định thời hạn từ 12 - 36 tháng: Báo trước ít nhất 30 ngày.
                            
                            3. CƠ CHẾ AI CONTRACT DIFF-CHECK CHỐNG SỬA LÉN:
                            - Bản thảo hợp đồng được ký nháy và lưu trữ nguyên gốc trong CSDL (jsonGoc).
                            - Khi nộp lại bản scan có chữ ký sống, hệ thống AI Gemini Vision tự động trích xuất các thực thể trọng yếu (Họ tên, Mức lương, Thời hạn báo trước, Địa điểm).
                            - Nếu phát hiện mức lương bị sửa lén (ví dụ 15M -> 25M) hoặc thời hạn báo trước bị đổi về 0 ngày -> Hệ thống lập tức KHÓA HỢP ĐỒNG và báo cáo Ban Pháp chế xử lý kỷ luật.
                            """)
                    .canCuPhapLy("Điều 13, 20, 35 Bộ luật Lao động 2019")
                    .quyenLoiNhanVien("Đảm bảo tính minh bạch, công bằng về quyền lợi tiền lương, thưởng và bảo hiểm xã hội.")
                    .thuTucThucHien("Truy cập Tab 2 'Đối Chiếu Hợp Đồng' -> Kiểm tra bản thảo gốc hoặc tải bản scan để AI rà soát tự động trước khi nộp lưu trữ.")
                    .hanhDongLienKet("TAB_CONTRACT")
                    .createdAt(LocalDateTime.now())
                    .build());

            // 5. Chế độ Phúc lợi & Chăm sóc sức khỏe
            policyRepository.save(ChinhSachNhanSu.builder()
                    .maDieuKhoan("CHE_DO_PHUC_LOI_2026")
                    .tieuDe("Chế độ Phúc lợi, Khám sức khỏe định kỳ & Bảo hiểm GTS Care 2026")
                    .danhMuc("PHUC_LOI")
                    .noiDungTomTat("Gói bảo hiểm sức khỏe tai nạn 24/7, khám sức khỏe tổng quát hằng năm, trợ cấp ăn trưa và ngân sách đào tạo nâng cao kỹ năng AI.")
                    .noiDungChiTiet("""
                            1. KHÁM SỨC KHỎE TỔNG QUÁT ĐỊNH KỲ:
                            - Diễn ra định kỳ vào Quý 2 hằng năm tại Bệnh viện Đa khoa Quốc tế đối tác (Bạch Mai, Thu Cúc, Việt Đức).
                            - Chi phí 100% do công ty chi trả.
                            
                            2. GÓI BẢO HIỂM NÂNG CAO GTS CARE:
                            - Áp dụng cho toàn bộ nhân sự chính thức: Hạn mức bảo hiểm tai nạn 24/7 và điều trị nội trú lên tới 100.000.000 VNĐ/năm.
                            
                            3. TRỢ CẤP VÀ HỖ TRỢ ĐÀO TẠO:
                            - Phụ cấp ăn trưa: 730.000 VNĐ/tháng theo ngày công thực tế.
                            - Ngân sách hỗ trợ thi chứng chỉ công nghệ (AWS, Google Cloud, AI Coding): Tài trợ 100% lệ phí thi cho chứng chỉ đạt chuẩn.
                            """)
                    .canCuPhapLy("Thỏa ước Lao động Tập thể GTS Việt Nam 2026")
                    .quyenLoiNhanVien("Chăm sóc toàn diện thể chất và tinh thần; nâng cao năng lực chuyên môn nghiệp vụ.")
                    .thuTucThucHien("Theo dõi thông báo từ Phòng Nhân sự vào đầu Quý 2 để nhận phiếu khám và mã số bảo hiểm GTS Care cá nhân.")
                    .hanhDongLienKet("NONE")
                    .createdAt(LocalDateTime.now())
                    .build());

            log.info("Đã nạp thành công 5 văn bản chính sách & quy chế nhân sự vào SQL Server.");
        }
    }

    public List<ChinhSachNhanSu> getAllPolicies() {
        return policyRepository.findAll();
    }

    public List<ChinhSachNhanSu> getPoliciesByCategory(String category) {
        if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)) {
            return policyRepository.findAll();
        }
        return policyRepository.findByDanhMuc(category.toUpperCase());
    }

    /**
     * AI Copilot tra cứu và giải đáp chính sách theo câu hỏi tự nhiên của nhân viên
     */
    public PolicyAnswerResponseDTO askPolicyCopilot(PolicyQuestionRequestDTO request) {
        String query = request.getQuestion() != null ? request.getQuestion().trim() : "";
        if (query.isBlank()) {
            return PolicyAnswerResponseDTO.builder()
                    .question(query)
                    .answerSummary("Vui lòng nhập câu hỏi cần tra cứu!")
                    .answerDetail("Bạn có thể hỏi về: 'Quy định nghỉ phép năm', 'Chế độ ốm đau BHXH', 'Quy định đợt Blackout', 'Thời hạn báo trước hợp đồng'...")
                    .legalBasis("Quy chế Nội bộ GTS Việt Nam & BLLĐ 2019")
                    .actionType("NONE")
                    .aiSource("GTS_POLICY_ASSISTANT")
                    .build();
        }

        // 1. Tìm kiếm các điều khoản liên quan trong CSDL
        List<ChinhSachNhanSu> relatedArticles = findRelevantPolicies(query);

        // 2. Kiểm tra nếu có API key thật -> Gọi Google Gemini Live API
        if (apiKey != null && !apiKey.isBlank() && !apiKey.contains("YOUR_GEMINI_API_KEY")) {
            try {
                return callGeminiForPolicyAnswer(query, relatedArticles);
            } catch (Exception e) {
                log.warn("Gemini Live API Policy failed, fallback to smart rule engine: {}", e.getMessage());
            }
        }

        // 3. Fallback Smart Rule Engine (Hoạt động offline mượt mà & chính xác)
        return generateSmartPolicyAnswer(query, relatedArticles);
    }

    private List<ChinhSachNhanSu> findRelevantPolicies(String query) {
        String normalizedQuery = removeAccents(query);
        List<ChinhSachNhanSu> all = policyRepository.findAll();
        List<ChinhSachNhanSu> matched = new ArrayList<>();

        for (ChinhSachNhanSu p : all) {
            if (normalizedQuery.contains("phep") || normalizedQuery.contains("nghi phep") || normalizedQuery.contains("ket hon") || normalizedQuery.contains("nua ngay") || normalizedQuery.contains("0.5") || normalizedQuery.contains("viec rieng")) {
                if ("NGHI_PHEP".equals(p.getDanhMuc())) matched.add(p);
            } else if (normalizedQuery.contains("om") || normalizedQuery.contains("bhxh") || normalizedQuery.contains("benh") || normalizedQuery.contains("nam vien") || normalizedQuery.contains("con om")) {
                if ("BHXH".equals(p.getDanhMuc())) matched.add(p);
            } else if (normalizedQuery.contains("blackout") || normalizedQuery.contains("cao diem") || normalizedQuery.contains("cap cuu") || normalizedQuery.contains("chan") || normalizedQuery.contains("10/11")) {
                if ("BLACKOUT".equals(p.getDanhMuc())) matched.add(p);
            } else if (normalizedQuery.contains("hop dong") || normalizedQuery.contains("luong") || normalizedQuery.contains("thoi viec") || normalizedQuery.contains("bao truoc") || normalizedQuery.contains("sua len") || normalizedQuery.contains("thu viec")) {
                if ("HOP_DONG".equals(p.getDanhMuc())) matched.add(p);
            } else if (normalizedQuery.contains("phuc loi") || normalizedQuery.contains("kham suc khoe") || normalizedQuery.contains("bao hiem") || normalizedQuery.contains("gts care")) {
                if ("PHUC_LOI".equals(p.getDanhMuc())) matched.add(p);
            }
        }

        if (matched.isEmpty()) {
            return all.stream().limit(3).collect(Collectors.toList());
        }
        return matched;
    }

    private PolicyAnswerResponseDTO generateSmartPolicyAnswer(String query, List<ChinhSachNhanSu> articles) {
        String normalizedQuery = removeAccents(query);

        String answerSummary;
        StringBuilder detail = new StringBuilder();
        String legalBasis;
        String actionType = "NONE";
        String actionLabel = null;
        String actionTarget = null;

        if (normalizedQuery.contains("om") || normalizedQuery.contains("bhxh") || normalizedQuery.contains("benh") || normalizedQuery.contains("nam vien") || normalizedQuery.contains("con om")) {
            answerSummary = "Bạn được hưởng trợ cấp ốm đau 75% mức tiền lương đóng BHXH và ngày nghỉ không bị trừ vào quỹ phép năm.";
            detail.append("**1. Mức trợ cấp BHXH:** Được cơ quan BHXH chi trả 75% tiền lương đóng BHXH tháng liền kề cho mỗi ngày nghỉ bệnh có chỉ định y tế.\n\n");
            detail.append("**2. Yêu cầu chứng từ:** Cần nộp Giấy ra viện (nội trú) hoặc Giấy chứng nhận nghỉ việc hưởng BHXH theo Thông tư 56/BYT (ngoại trú).\n\n");
            detail.append("**3. Chống gian lận & Bảo mật:** Chứng từ phải mang tên chính chủ (AI đối soát tự động). Dữ liệu bệnh án được mã hóa AES theo Luật số 91/2025/QH15.\n\n");
            detail.append("**4. Thao tác tiếp theo:** Bạn có thể tải ngay giấy khám bệnh lên hệ thống để AI thẩm định và gửi phê duyệt tự động.");
            legalBasis = "Điều 25, 28 Luật Bảo hiểm Xã hội 2024; Thông tư 56/2017/TT-BYT; Luật số 91/2025/QH15";
            actionType = "TAB_LEAVE";
            actionLabel = "👉 Tạo Đơn Nghỉ Ốm & Nộp Chứng Từ Ngay";
            actionTarget = "forensic-tab";

        } else if (normalizedQuery.contains("blackout") || normalizedQuery.contains("cao diem") || normalizedQuery.contains("10/11") || normalizedQuery.contains("cap cuu")) {
            answerSummary = "Thời gian Blackout (10/11 - 20/11) hạn chế nghỉ thường để bàn giao dự án. Riêng ca Cấp Cứu được cơ chế Smart Bypass bảo vệ đặc biệt.";
            detail.append("**1. Quy tắc Blackout:** Các đơn xin nghỉ phép năm thông thường sẽ tự động bị hệ thống từ chối để đảm bảo tiến độ bàn giao dự án đối tác.\n\n");
            detail.append("**2. Smart Bypass Cấp Cứu:** Trường hợp tai nạn, viêm ruột thừa hoại tử, phẫu thuật khẩn cấp có giấy tờ viện, AI Vision sẽ tự động kích hoạt **Bypass** và chuyển thẳng tới Tổng Giám Đốc phê duyệt khẩn cấp.\n\n");
            detail.append("**3. Hướng dẫn nộp đơn:** Nếu thuộc diện cấp cứu khẩn cấp, vui lòng tải ảnh bệnh án có dấu mộc tròn đỏ của bệnh viện lên Cổng GTS-HRM.");
            legalBasis = "Quy chế Bàn giao Dự án Trọng điểm số 08/2026/QC-GTS; BLLĐ 2019";
            actionType = "TAB_LEAVE";
            actionLabel = "👉 Nộp Hồ Sơ Cấp Cứu Y Tế (Smart Bypass)";
            actionTarget = "forensic-tab";

        } else if (normalizedQuery.contains("phep") || normalizedQuery.contains("ket hon") || normalizedQuery.contains("viec rieng") || normalizedQuery.contains("nua ngay") || normalizedQuery.contains("0.5")) {
            answerSummary = "Bạn được hưởng 12 ngày phép năm (tăng theo thâm niên), hỗ trợ nghỉ nửa ngày (0.5 công) và nghỉ việc riêng hưởng 100% lương theo Điều 115 BLLĐ 2019.";
            detail.append("**1. Nghỉ phép năm:** 12 ngày/năm (làm đủ 12 tháng). Mỗi 5 năm làm việc tại GTS được cộng thêm 1 ngày.\n\n");
            detail.append("**2. Nghỉ nửa ngày (0.5 công):** Chọn nghỉ Buổi sáng hoặc Buổi chiều. Hệ thống tự động ghi nhận 4h làm việc + 4h nghỉ phép (Mã công P/2 hoặc O/2) trên Timesheet.\n\n");
            detail.append("**3. Nghỉ việc riêng hưởng nguyên 100% lương:** Bản thân kết hôn (3 ngày), con kết hôn (1 ngày), người thân trực hệ mất (3 ngày).\n\n");
            detail.append("**4. Thời hạn gửi đơn:** Gửi đơn trước tối thiểu 24 giờ trên hệ thống.");
            legalBasis = "Điều 113, 115 Bộ luật Lao động 2019; Quy chế GTS-HRM";
            actionType = "TAB_LEAVE";
            actionLabel = "👉 Nộp Đơn Xin Nghỉ Phép (0.5 hoặc Cả ngày)";
            actionTarget = "forensic-tab";

        } else if (normalizedQuery.contains("hop dong") || normalizedQuery.contains("luong") || normalizedQuery.contains("thoi viec") || normalizedQuery.contains("bao truoc") || normalizedQuery.contains("sua len") || normalizedQuery.contains("thu viec")) {
            answerSummary = "Thời hạn báo trước khi thôi việc là 30 ngày (HĐ xác định thời hạn) hoặc 45 ngày (HĐ không xác định thời hạn). Hợp đồng được bảo vệ bằng AI Side-by-Side Diff.";
            detail.append("**1. Thời hạn báo trước (Điều 35 BLLĐ 2019):** Ít nhất 30 ngày nếu HĐLĐ có thời hạn từ 12-36 tháng; ít nhất 45 ngày nếu HĐLĐ không xác định thời hạn.\n\n");
            detail.append("**2. Bảo vệ tiền lương:** Mức lương thỏa thuận chính thức được ký nháy và lưu trong CSDL. Mọi hành vi tự ý sửa đổi trên bản scan (như nâng khống lương) đều bị AI OCR phát hiện và lập biên bản kỷ luật.\n\n");
            detail.append("**3. Xem hợp đồng:** Bạn có thể vào Tab 'Đối Chiếu Hợp Đồng' để xem đối chiếu chi tiết giữa bản thảo và bản scan.");
            legalBasis = "Điều 13, 20, 35 Bộ luật Lao động 2019";
            actionType = "TAB_CONTRACT";
            actionLabel = "👉 Xem Đối Chiếu Hợp Đồng Lao Động (Side-by-Side)";
            actionTarget = "contract-tab";

        } else {
            answerSummary = "Hệ thống đã tra cứu các quy chế nhân sự GTS Việt Nam & Bộ luật Lao động 2019 liên quan đến yêu cầu của bạn.";
            detail.append("**Các chính sách áp dụng tại GTS Việt Nam:**\n\n");
            detail.append("- **Thời giờ làm việc:** 40 giờ/tuần (Thứ 2 đến Thứ 6), nghỉ T7 & CN.\n");
            detail.append("- **Nghỉ lễ tết:** Nghỉ nguyên lương 11 ngày lễ tết quốc gia trong năm.\n");
            detail.append("- **Bảo hiểm nâng cao:** Gói chăm sóc sức khỏe tai nạn 24/7 GTS Care lên đến 100.000.000 VNĐ.\n");
            detail.append("- **Liên kết nghiệp vụ:** Hãy sử dụng thanh công cụ phía trên để tra cứu sâu theo từng nhóm chính sách hoặc nộp đơn trực tiếp.");
            legalBasis = "Thỏa ước Lao động Tập thể GTS Việt Nam & BLLĐ 2019";
            actionType = "NONE";
        }

        return PolicyAnswerResponseDTO.builder()
                .question(query)
                .answerSummary(answerSummary)
                .answerDetail(detail.toString())
                .legalBasis(legalBasis)
                .actionType(actionType)
                .actionLabel(actionLabel)
                .actionTarget(actionTarget)
                .aiSource("GTS_KNOWLEDGE_BASE_AI")
                .relatedArticles(articles)
                .build();
    }

    private String removeAccents(String text) {
        if (text == null) return "";
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("đ", "d").replaceAll("Đ", "D")
                .toLowerCase();
    }

    private PolicyAnswerResponseDTO callGeminiForPolicyAnswer(String query, List<ChinhSachNhanSu> articles) throws Exception {
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                modelName, apiKey);

        StringBuilder contextBuilder = new StringBuilder();
        for (ChinhSachNhanSu a : articles) {
            contextBuilder.append("--- ").append(a.getTieuDe()).append(" ---\n")
                    .append("Căn cứ: ").append(a.getCanCuPhapLy()).append("\n")
                    .append(a.getNoiDungChiTiet()).append("\n\n");
        }

        String prompt = String.format("""
                Bạn là GTS Policy Copilot - Trợ lý AI pháp lý & nhân sự nội bộ chuyên nghiệp của Công ty GTS Việt Nam.
                Dựa trên CƠ SỞ TRI THỨC VĂN BẢN NỘI BỘ dưới đây:
                %s
                
                Hãy giải đáp câu hỏi của nhân viên: "%s"
                
                Yêu cầu trả về định dạng JSON thuần túy (không code markdown block):
                {
                  "answerSummary": "<Tóm tắt câu trả lời ngắn gọn, trực diện trong 1-2 câu>",
                  "answerDetail": "<Nội dung giải thích chi tiết, gạch đầu dòng rõ ràng, đầy đủ quyền lợi, thủ tục và điều kiện hưởng>",
                  "legalBasis": "<Căn cứ pháp lý cụ thể: Điều luật, Nghị định hoặc Quy chế công ty>",
                  "actionType": "<'TAB_LEAVE' nếu câu hỏi liên quan nghỉ phép/ốm đau/cấp cứu, 'TAB_CONTRACT' nếu liên quan hợp đồng/lương, hoặc 'NONE'>",
                  "actionLabel": "<Tên nút bấm gợi ý hành động, ví dụ: '👉 Tạo Đơn Nghỉ Phép Ngay'>",
                  "actionTarget": "<'forensic-tab' hoặc 'contract-tab' hoặc null>"
                }
                """, contextBuilder.toString(), query);

        Map<String, Object> partText = Map.of("text", prompt);
        Map<String, Object> contents = Map.of("parts", List.of(partText));
        Map<String, Object> requestBody = Map.of("contents", List.of(contents));

        String responseStr = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(responseStr);
        String rawText = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        String cleanJson = rawText.replaceAll("```json", "").replaceAll("```", "").trim();
        JsonNode jsonNode = objectMapper.readTree(cleanJson);

        return PolicyAnswerResponseDTO.builder()
                .question(query)
                .answerSummary(jsonNode.path("answerSummary").asText())
                .answerDetail(jsonNode.path("answerDetail").asText())
                .legalBasis(jsonNode.path("legalBasis").asText())
                .actionType(jsonNode.path("actionType").asText("NONE"))
                .actionLabel(jsonNode.hasNonNull("actionLabel") ? jsonNode.path("actionLabel").asText() : null)
                .actionTarget(jsonNode.hasNonNull("actionTarget") ? jsonNode.path("actionTarget").asText() : null)
                .aiSource("GOOGLE_GEMINI_LIVE_API (" + modelName + ")")
                .relatedArticles(articles)
                .build();
    }
}
