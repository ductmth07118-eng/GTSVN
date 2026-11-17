package com.gtsvn.hrm.service;

import com.gtsvn.hrm.dto.*;
import com.gtsvn.hrm.entity.*;
import com.gtsvn.hrm.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DonXinPhepService {

    private final DonXinPhepRepository donXinPhepRepository;
    private final NhanVienRepository nhanVienRepository;
    private final LichSuTruyCapRepository lichSuTruyCapRepository;
    private final ChamCongNgayRepository chamCongNgayRepository;
    private final NgayLeRepository ngayLeRepository;
    private final ForensicService forensicService;
    private final GeminiVisionService geminiVisionService;

    // Định nghĩa đợt cao điểm dự án Go-Live (Blackout Period)
    private static final LocalDate BLACKOUT_START = LocalDate.of(2026, 11, 10);
    private static final LocalDate BLACKOUT_END = LocalDate.of(2026, 11, 20);

    /**
     * Khởi tạo danh mục ngày nghỉ Lễ/Tết Quốc gia năm 2026 vào Database
     */
    @PostConstruct
    public void initHolidays() {
        if (ngayLeRepository.count() == 0) {
            List<NgayLe> holidays = List.of(
                    NgayLe.builder().ngayLe(LocalDate.of(2026, 1, 1)).tenNgayLe("Tết Dương Lịch 2026").nam(2026).build(),
                    NgayLe.builder().ngayLe(LocalDate.of(2026, 2, 16)).tenNgayLe("Nghỉ Tết Bính Ngọ (29 Tết)").nam(2026).build(),
                    NgayLe.builder().ngayLe(LocalDate.of(2026, 2, 17)).tenNgayLe("Mùng 1 Tết Bính Ngọ").nam(2026).build(),
                    NgayLe.builder().ngayLe(LocalDate.of(2026, 2, 18)).tenNgayLe("Mùng 2 Tết Bính Ngọ").nam(2026).build(),
                    NgayLe.builder().ngayLe(LocalDate.of(2026, 2, 19)).tenNgayLe("Mùng 3 Tết Bính Ngọ").nam(2026).build(),
                    NgayLe.builder().ngayLe(LocalDate.of(2026, 2, 20)).tenNgayLe("Mùng 4 Tết Bính Ngọ").nam(2026).build(),
                    NgayLe.builder().ngayLe(LocalDate.of(2026, 4, 26)).tenNgayLe("Giỗ Tổ Hùng Vương (10/3 AL)").nam(2026).build(),
                    NgayLe.builder().ngayLe(LocalDate.of(2026, 4, 30)).tenNgayLe("Giải phóng Miền Nam 30/4").nam(2026).build(),
                    NgayLe.builder().ngayLe(LocalDate.of(2026, 5, 1)).tenNgayLe("Quốc tế Lao động 1/5").nam(2026).build(),
                    NgayLe.builder().ngayLe(LocalDate.of(2026, 9, 2)).tenNgayLe("Quốc Khánh 2/9").nam(2026).build(),
                    NgayLe.builder().ngayLe(LocalDate.of(2026, 9, 3)).tenNgayLe("Nghỉ liền kề Quốc Khánh 3/9").nam(2026).build()
            );
            ngayLeRepository.saveAll(holidays);
            log.info("Đã khởi tạo thành công {} ngày nghỉ lễ Quốc gia năm 2026 vào danh_muc_ngay_le.", holidays.size());
        }
    }

    /**
     * Thuật toán tính ngày nghỉ theo NGÀY LÀM VIỆC THỰC TẾ (Điều 113 BLLĐ 2019 & Luật BHXH 2024)
     * Tự động loại trừ Thứ Bảy, Chủ Nhật VÀ Ngày Lễ/Tết trong Database.
     * Hỗ trợ nghỉ nửa ngày (0.5 công).
     */
    public double calculateWorkingDays(LocalDate startDate, LocalDate endDate, String buoiNghi) {
        if (startDate.isAfter(endDate)) return 0.0;

        List<NgayLe> holidays = ngayLeRepository.findByNgayLeBetween(startDate, endDate);
        Set<LocalDate> holidaySet = holidays.stream().map(NgayLe::getNgayLe).collect(Collectors.toSet());

        long days = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek dow = date.getDayOfWeek();
            boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
            boolean isHoliday = holidaySet.contains(date);
            if (!isWeekend && !isHoliday) {
                days++;
            }
        }

        if (days == 0) return 0.0;

        if ("BUOI_SANG".equalsIgnoreCase(buoiNghi) || "BUOI_CHIEU".equalsIgnoreCase(buoiNghi)) {
            return Math.max(0.5, days - 0.5);
        }
        return (double) days;
    }

    public double calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        return calculateWorkingDays(startDate, endDate, "CA_NGAY");
    }

    // =========================================================================
    // LAYER 2 DEFENSE: SO KHỚP CHUỖI TÊN TIẾNG VIỆT CÓ DẤU & LEVENSHTEIN
    // =========================================================================
    public static double calculateNameSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        String n1 = removeAccents(s1.trim().toLowerCase());
        String n2 = removeAccents(s2.trim().toLowerCase());
        if (n1.equals(n2)) return 1.0;
        if (n1.contains(n2) || n2.contains(n1)) return 0.95;

        int distance = computeLevenshtein(n1, n2);
        int maxLen = Math.max(n1.length(), n2.length());
        if (maxLen == 0) return 1.0;
        return 1.0 - ((double) distance / maxLen);
    }

    private static String removeAccents(String text) {
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace("đ", "d").replace("Đ", "D");
    }

    private static int computeLevenshtein(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    @Transactional
    public DonXinPhepSubmitResponseDTO submitLeaveRequest(
            Long nhanVienId,
            String loaiNghiPhep,
            LocalDate ngayBatDau,
            LocalDate ngayKetThuc,
            String buoiNghi,
            MultipartFile file
    ) throws Exception {

        if (ngayKetThuc.isBefore(ngayBatDau)) {
            throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu!");
        }

        String safeBuoiNghi = (buoiNghi != null && !buoiNghi.isBlank()) ? buoiNghi : "CA_NGAY";

        // =========================================================================
        // 1. TÍNH TOÁN NGÀY LÀM VIỆC THỰC TẾ (LOẠI TRỪ T7, CN, NGÀY NGHỈ LỄ TẾT)
        // =========================================================================
        double workingDays = calculateWorkingDays(ngayBatDau, ngayKetThuc, safeBuoiNghi);
        if (workingDays <= 0) {
            throw new IllegalArgumentException("Khoảng thời gian bạn chọn chỉ rơi vào ngày nghỉ cuối tuần hoặc nghỉ lễ Tết (0 ngày làm việc). Không cần tạo đơn xin nghỉ!");
        }

        // =========================================================================
        // 2. CHẶN NỘP ĐƠN TRÙNG LỊCH NGHỈ (RACE CONDITION & OVERLAP PROTECTION)
        // =========================================================================
        List<DonXinPhep> overlaps = donXinPhepRepository.findOverlappingLeaves(nhanVienId, ngayBatDau, ngayKetThuc);
        if (!overlaps.isEmpty()) {
            DonXinPhep o = overlaps.get(0);
            throw new IllegalStateException(String.format(
                    "TRÙNG LỊCH NGHỈ: Bạn đã có đơn #DXP-%d từ ngày %s đến %s (Trạng thái: %s). Không thể tạo đơn trùng lịch!",
                    o.getId(), o.getNgayBatDau(), o.getNgayKetThuc(), o.getTrangThai()
            ));
        }

        // 3. Kiểm tra thông tin nhân viên
        NhanVien nhanVien = nhanVienRepository.findById(nhanVienId)
                .orElseGet(() -> nhanVienRepository.save(
                        NhanVien.builder()
                                .id(nhanVienId)
                                .hoTen("Nhân viên #" + nhanVienId)
                                .phongBan("Bộ phận Vận hành GTS")
                                .chucVu("Chuyên viên")
                                .soNgayPhepConLai(12.0)
                                .soNgayDaNghi(0.0)
                                .build()
                ));

        // =========================================================================
        // 4. LƯU FILE VẬT LÝ VÀO THƯ MỤC UPLOADS
        // =========================================================================
        Path uploadDir = Paths.get("uploads");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        String safeFilename = System.currentTimeMillis() + "_" + (file.getOriginalFilename() != null ? file.getOriginalFilename().replaceAll("\\s+", "_") : "doc.jpg");
        Path targetPath = uploadDir.resolve(safeFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        String fileUrl = "uploads/" + safeFilename;

        // =========================================================================
        // 5. DOCUMENT METADATA INSPECTION ENGINE (Java + metadata-extractor)
        // =========================================================================
        ForensicResultDTO forensicResult;
        try (InputStream is = Files.newInputStream(targetPath)) {
            forensicResult = forensicService.inspectImage(is);
        }

        // =========================================================================
        // 6. DOCUMENT VISION & OCR VÀ SO KHỚP DANH TÍNH (Google Gemini Vision AI)
        // =========================================================================
        byte[] fileBytes = Files.readAllBytes(targetPath);
        MedicalAnalysisDTO medicalResult = geminiVisionService.analyzeMedicalDocument(fileBytes, safeFilename, nhanVien.getHoTen());

        // Kiểm tra đối chiếu chéo Họ Tên bệnh nhân (Layer 2 - Backend Fuzzy Matching)
        double nameSimilarity = calculateNameSimilarity(medicalResult.getHoTenBenhNhan(), nhanVien.getHoTen());
        boolean isNameMatched = medicalResult.isNameMatched() && (nameSimilarity >= 0.65 || "NGHI_CHAM_CON_OM".equalsIgnoreCase(loaiNghiPhep));

        // =========================================================================
        // 7. PHÂN LUỒNG RỦI RO & TÁCH TRẠNG THÁI VỚI CỜ CẢNH BÁO (DECISION SUPPORT)
        // =========================================================================
        List<String> flags = new ArrayList<>();
        String overallRisk = "CO_XANH";
        String finalDecision = "CHO_PHE_DUYET";
        int confidence = (forensicResult.getConfidenceScore() + medicalResult.getDoTinCay()) / 2;

        if (!forensicResult.isClean()) {
            flags.add("CO_METADATA_BAT_THUONG");
            overallRisk = "CO_VANG";
            finalDecision = "CHO_HR_XAC_MINH";
        }

        if (medicalResult.isChuVietTayMo() || medicalResult.getDoTinCay() < 70) {
            flags.add("OCR_DO_TIN_CAY_THAP");
            overallRisk = "CO_VANG";
            finalDecision = "CHO_HR_XAC_MINH";
        }

        // =========================================================================
        // 8. KIỂM DUYỆT NGỮ CẢNH, SO KHỚP TÊN & CHÍNH SÁCH BLACKOUT PERIOD
        // =========================================================================
        boolean isInBlackout = isDateInBlackout(ngayBatDau, ngayKetThuc);
        boolean isBypassed = false;
        String trangThai = "CHO_DUYET";
        String thongBao;

        if (!medicalResult.isMedicalDocument()) {
            overallRisk = "CO_DO";
            trangThai = "TU_CHOI_KHONG_HOP_LE";
            finalDecision = "TU_CHOI_KHONG_PHAI_CHUNG_TU_Y_TE";
            confidence = 0;
            flags.add("TAI_LIEU_PHI_Y_TE");
            thongBao = (medicalResult.getLyDoDanhGia() != null && !medicalResult.getLyDoDanhGia().isBlank())
                    ? medicalResult.getLyDoDanhGia()
                    : "⛔ TỪ CHỐI TIẾP NHẬN: Hình ảnh tải lên được nhận diện là Căn cước công dân / Giấy tờ tùy thân / Ảnh phi y tế! Vui lòng tải lên đúng chứng từ y tế.";
        } else if (!isNameMatched && !"NGHI_CHAM_CON_OM".equalsIgnoreCase(loaiNghiPhep)) {
            // BẮT QUẢ TANG MƯỢN CHỨNG TỪ NGƯỜI KHÁC
            overallRisk = "CO_DO";
            trangThai = "TU_CHOI_KHONG_HOP_LE";
            finalDecision = "TU_CHOI_MUON_CHUNG_TU_NGUOI_KHAC";
            confidence = 0;
            flags.add("TEN_BENH_NHAN_KHONG_KHOP");
            thongBao = String.format("⛔ CẢNH BÁO MƯỢN CHỨNG TỪ: Họ tên người bệnh trên chứng từ ('%s') KHÔNG TRÙNG KHỚP với nhân viên nộp đơn ('%s'). Phát hiện dấu hiệu mượn chứng từ y tế của người khác!",
                    medicalResult.getHoTenBenhNhan(), nhanVien.getHoTen());
        } else if (nameSimilarity < 0.85 && nameSimilarity >= 0.65) {
            // Chữ viết tay viết tắt hoặc mờ -> Đẩy vào Cờ Vàng
            flags.add("TEN_BENH_NHAN_NGHI_VAN_CHU_MO");
            overallRisk = "CO_VANG";
            trangThai = "CHO_HR_XAC_MINH";
            thongBao = String.format("⚠️ CỜ VÀNG (Cần HR xác minh): Họ tên trên giấy khám ('%s') có dấu hiệu viết tắt / chữ mờ so với hồ sơ nhân viên ('%s'). Đề nghị HR đối chiếu chứng từ gốc.",
                    medicalResult.getHoTenBenhNhan(), nhanVien.getHoTen());
        } else if (isInBlackout) {
            flags.add("CO_XUNG_DOT_BLACKOUT");
            if (medicalResult.isKhanCap()) {
                isBypassed = true;
                flags.add("CO_YEU_CAU_KHAN_CAP");
                trangThai = "CHO_GIAM_DOC_DUYET";
                finalDecision = "GHI_NHAN_KHAN_CAP_CHO_BO_SUNG_HO_SO";
                overallRisk = "CO_XANH";
                thongBao = "⚡ THÔNG BÁO CẤP CỨU Y TẾ: Ưu tiên an toàn & sức khỏe người lao động! " +
                           "Hệ thống đã ghi nhận ngoại lệ Blackout Period, phát cảnh báo khẩn đến HR & Quản lý; hồ sơ chứng từ và phê duyệt đặc cách được hoàn thiện sau khi xuất viện.";
            } else {
                trangThai = "BI_CHAN_BLACKOUT";
                finalDecision = "TU_CHOI_DO_CHINH_SACH_BLACKOUT";
                thongBao = "⛔ TỪ CHỐI DO CHÍNH SÁCH CÔNG TY: Đang trong đợt cao điểm Blackout Period (10/11 - 20/11 - Dự án Go-Live). " +
                           "Chứng từ y tế của bạn hoàn toàn hợp lệ, nhưng chính sách vận hành công ty tạm hoãn các đơn nghỉ thường không có yếu tố cấp cứu.";
            }
        } else {
            if ("CO_VANG".equals(overallRisk)) {
                trangThai = "CHO_HR_XAC_MINH";
                if (flags.contains("CO_METADATA_BAT_THUONG")) {
                    thongBao = "⚠️ CỜ VÀNG (Cần HR xác minh): Phát hiện siêu dữ liệu từ công cụ biên tập ảnh (" +
                               forensicResult.getDetectedSoftware() + "). Đề nghị HR đối chiếu chứng từ gốc theo Luật Bảo vệ DLCN 2025.";
                } else {
                    thongBao = "⚠️ CỜ VÀNG (Cần HR xác minh): Chữ bác sĩ viết tay khó đọc / ảnh mờ. HR đối chiếu lại nội dung.";
                }
            } else {
                trangThai = "CHO_DUYET";
                thongBao = "✅ HỒ SƠ HỢP LỆ: Chứng từ rõ nét (" + medicalResult.getBenhVien() + "), khớp họ tên (" + nhanVien.getHoTen() + "). Sẵn sàng phê duyệt 1 chạm.";
            }
        }

        InspectionReportDTO report = InspectionReportDTO.builder()
                .forensic(forensicResult)
                .medical(medicalResult)
                .overallRiskFlag(overallRisk)
                .overallConfidence(confidence)
                .finalDecision(finalDecision)
                .build();

        // 9. Lưu vào cơ sở dữ liệu SQL Server (Chẩn đoán bệnh tự động mã hóa AES At-Rest)
        String canhBaoStr = String.join(",", flags);
        DonXinPhep don = DonXinPhep.builder()
                .nhanVienId(nhanVienId)
                .loaiNghiPhep(loaiNghiPhep)
                .ngayBatDau(ngayBatDau)
                .ngayKetThuc(ngayKetThuc)
                .buoiNghi(safeBuoiNghi)
                .soNgayLamViecNghi(workingDays)
                .hoTenBenhNhan(medicalResult.getHoTenBenhNhan())
                .isNameMatched(isNameMatched)
                .fileMinhChungUrl(fileUrl)
                .chanDoanBenh(medicalResult.getChanDoan()) // JPA Converter tự động mã hóa ENC:AES
                .trangThai(trangThai)
                .canhBaoFlags(canhBaoStr)
                .doTinCay(confidence)
                .nhanRuiRo(overallRisk)
                .lyDoRuiRo(thongBao)
                .isKhanCap(medicalResult.isKhanCap())
                .createdAt(LocalDateTime.now())
                .build();

        DonXinPhep savedDon = donXinPhepRepository.save(don);
        log.info("Đã lưu Đơn xin phép #{} (Số ngày làm việc: {}, Buổi: {}) cho nhân viên: {}",
                savedDon.getId(), workingDays, safeBuoiNghi, nhanVien.getHoTen());

        return DonXinPhepSubmitResponseDTO.builder()
                .donXinPhepId(savedDon.getId())
                .tenNhanVien(nhanVien.getHoTen())
                .phongBan(nhanVien.getPhongBan())
                .fileUrl(fileUrl)
                .buoiNghi(safeBuoiNghi)
                .soNgayLamViecNghi(workingDays)
                .hoTenBenhNhan(medicalResult.getHoTenBenhNhan())
                .isNameMatched(isNameMatched)
                .trangThai(trangThai)
                .canhBaoFlags(canhBaoStr)
                .blackoutBypassed(isBypassed)
                .thongBao(thongBao)
                .report(report)
                .build();
    }

    // =========================================================================
    // PHÊ DUYỆT ĐƠN & ĐỒNG BỘ ATOMIC SANG BẢNG CHẤM CÔNG (TIMESHEET)
    // =========================================================================
    @Transactional
    public Map<String, Object> approveLeave(Long donId, String approverUser) {
        DonXinPhep don = donXinPhepRepository.findById(donId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn #" + donId));

        if ("DA_DUYET".equalsIgnoreCase(don.getTrangThai())) {
            throw new IllegalStateException("Đơn #" + donId + " đã được phê duyệt trước đó rồi (Chống cập nhật 2 lần)!");
        }

        don.setTrangThai("DA_DUYET");
        donXinPhepRepository.save(don);

        double workingDays = don.getSoNgayLamViecNghi() != null ? don.getSoNgayLamViecNghi() :
                calculateWorkingDays(don.getNgayBatDau(), don.getNgayKetThuc(), don.getBuoiNghi());

        NhanVien nv = nhanVienRepository.findById(don.getNhanVienId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên"));

        // =========================================================================
        // PHÂN LOẠI NGHỈ CHUẨN XÁC THEO BỘ LUẬT LAO ĐỘNG 2019 & LUẬT BHXH 2024
        // =========================================================================
        String loai = don.getLoaiNghiPhep();
        String maCong = "X";
        double standardHours = 8.0;
        double workHours = 0.0;

        boolean isHalfDay = "BUOI_SANG".equalsIgnoreCase(don.getBuoiNghi()) || "BUOI_CHIEU".equalsIgnoreCase(don.getBuoiNghi());
        if (isHalfDay) {
            standardHours = 4.0;
            workHours = 4.0; // Nửa ngày làm việc 4h
        }

        double gioNghiHuongLuong = 0.0;
        double gioNghiBhxh = 0.0;

        if ("NGHI_PHEP_NAM".equalsIgnoreCase(loai)) {
            // Nghỉ phép năm: Trừ trực tiếp vào quỹ phép năm, công ty trả 100% lương
            nv.setSoNgayPhepConLai(Math.max(0.0, nv.getSoNgayPhepConLai() - workingDays));
            nv.setSoNgayDaNghi(nv.getSoNgayDaNghi() + workingDays);
            maCong = isHalfDay ? "P/2" : "P";
            gioNghiHuongLuong = standardHours;
        } else if ("NGHI_VIEC_RIENG_HUONG_LUONG".equalsIgnoreCase(loai)) {
            // Điều 115 BLLĐ 2019: Kết hôn, tang lễ -> Hưởng 100% lương, KHÔNG trừ phép năm
            nv.setSoNgayDaNghi(nv.getSoNgayDaNghi() + workingDays);
            maCong = isHalfDay ? "VR/2" : "VR";
            gioNghiHuongLuong = standardHours;
        } else if ("NGHI_OM_BHXH".equalsIgnoreCase(loai)) {
            // Luật BHXH 2024: Nghỉ ốm do BHXH chi trả theo mức đóng, KHÔNG trừ phép năm
            nv.setSoNgayDaNghi(nv.getSoNgayDaNghi() + workingDays);
            maCong = isHalfDay ? "O/2" : "O";
            gioNghiBhxh = standardHours;
        } else if ("NGHI_KHONG_LUONG".equalsIgnoreCase(loai)) {
            // Nghỉ không lương: Không trừ phép năm, công ty không trả lương
            nv.setSoNgayDaNghi(nv.getSoNgayDaNghi() + workingDays);
            maCong = isHalfDay ? "KL/2" : "KL";
        } else {
            nv.setSoNgayDaNghi(nv.getSoNgayDaNghi() + workingDays);
            maCong = isHalfDay ? "P/2" : "P";
            gioNghiHuongLuong = standardHours;
        }
        nhanVienRepository.save(nv);

        // =========================================================================
        // LIÊN THÔNG BẢNG CHẤM CÔNG (TIMESHEET) TỪNG NGÀY LÀM VIỆC
        // =========================================================================
        for (LocalDate date = don.getNgayBatDau(); !date.isAfter(don.getNgayKetThuc()); date = date.plusDays(1)) {
            DayOfWeek dow = date.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                final LocalDate curDate = date;
                final double curGioHuongLuong = gioNghiHuongLuong;
                final double curGioBhxh = gioNghiBhxh;
                final double curGioThucTe = workHours;
                final String curMaCong = maCong;

                ChamCongNgay cc = chamCongNgayRepository.findByNhanVienIdAndNgayLamViec(nv.getId(), curDate)
                        .orElseGet(() -> ChamCongNgay.builder()
                                .nhanVienId(nv.getId())
                                .ngayLamViec(curDate)
                                .gioLich(8.0)
                                .build());

                cc.setGioThucTe(curGioThucTe);
                cc.setGioNghiHuongLuong(curGioHuongLuong);
                cc.setGioNghiBhxh(curGioBhxh);
                cc.setMaCong(curMaCong);
                cc.setDonXinPhepId(don.getId());
                chamCongNgayRepository.save(cc);
            }
        }

        log.info("HR {} đã duyệt đơn #{}. Nhân viên: {}, Ngày làm việc nghỉ: {}, Loại: {}, Phép còn: {}",
                approverUser, donId, nv.getHoTen(), workingDays, loai, nv.getSoNgayPhepConLai());

        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("status", "SUCCESS");
        resultMap.put("donId", donId);
        resultMap.put("nhanVienId", nv.getId());
        resultMap.put("tenNhanVien", nv.getHoTen());
        resultMap.put("soNgayLamViecNghi", workingDays);
        resultMap.put("buoiNghi", don.getBuoiNghi() != null ? don.getBuoiNghi() : "CA_NGAY");
        resultMap.put("loaiNghiPhep", loai);
        resultMap.put("maCong", maCong);
        resultMap.put("soNgayPhepConLai", nv.getSoNgayPhepConLai());
        resultMap.put("soNgayDaNghi", nv.getSoNgayDaNghi());
        resultMap.put("thongBao", String.format("Đã phê duyệt thành công đơn #%d (%s)! Đã tính %.1f ngày công làm việc và đồng bộ Timesheet tự động.",
                donId, loai, workingDays));
        return resultMap;
    }

    // =========================================================================
    // DỮ LIỆU BẢNG CHẤM CÔNG (TIMESHEET) CHO NHÂN VIÊN
    // =========================================================================
    public Map<String, Object> getNhanVienTimesheet(Long nhanVienId) {
        // Đảm bảo có dữ liệu chấm công cho 7 ngày gần nhất (10/11 - 16/11/2026)
        LocalDate startWeek = LocalDate.of(2026, 11, 10);
        LocalDate endWeek = LocalDate.of(2026, 11, 16);

        for (LocalDate date = startWeek; !date.isAfter(endWeek); date = date.plusDays(1)) {
            DayOfWeek dow = date.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                final LocalDate curDate = date;
                chamCongNgayRepository.findByNhanVienIdAndNgayLamViec(nhanVienId, curDate)
                        .orElseGet(() -> chamCongNgayRepository.save(
                                ChamCongNgay.builder()
                                        .nhanVienId(nhanVienId)
                                        .ngayLamViec(curDate)
                                        .gioLich(8.0)
                                        .gioThucTe(8.0)
                                        .gioNghiHuongLuong(0.0)
                                        .gioNghiBhxh(0.0)
                                        .maCong("X")
                                        .build()
                        ));
            }
        }

        List<ChamCongNgay> list = chamCongNgayRepository.findByNhanVienIdAndNgayLamViecBetweenOrderByNgayLamViecAsc(
                nhanVienId, startWeek, endWeek
        );

        double tongGioThucTe = list.stream().mapToDouble(ChamCongNgay::getGioThucTe).sum();
        double tongGioNghiHuongLuong = list.stream().mapToDouble(ChamCongNgay::getGioNghiHuongLuong).sum();
        double tongGioBhxh = list.stream().mapToDouble(ChamCongNgay::getGioNghiBhxh).sum();

        return Map.of(
                "nhanVienId", nhanVienId,
                "congChuanThang", 176.0, // 22 ngày công chuẩn x 8h
                "tongGioThucTe", tongGioThucTe,
                "tongGioNghiHuongLuong", tongGioNghiHuongLuong,
                "tongGioBhxh", tongGioBhxh,
                "records", list
        );
    }

    // =========================================================================
    // BẢO MẬT DỮ LIỆU Y TẾ (LUẬT BẢO VỆ DỮ LIỆU CÁ NHÂN SỐ 91/2025/QH15)
    // =========================================================================
    @Transactional
    public String decryptAndAuditDiagnosis(Long donId, String currentUser, String roleName, String ipAddress) {
        DonXinPhep don = donXinPhepRepository.findById(donId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn #" + donId));

        boolean isCnb = "ROLE_HR_CNB".equalsIgnoreCase(roleName);
        if (!isCnb) {
            return "*** ĐÃ ẨN BẢO MẬT Y TẾ (Theo Luật BV DLCN số 91/2025/QH15) - Chỉ HR C&B mới có quyền giải mã ***";
        }

        LichSuTruyCap logEntry = LichSuTruyCap.builder()
                .taiKhoanTruyCap(currentUser)
                .roleName(roleName)
                .donXinPhepId(donId)
                .hanhDong("XEM_GIAI_MA_CHAN_DOAN_BENH_NHAY_CAM")
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();
        lichSuTruyCapRepository.save(logEntry);
        log.info("AUDIT LOG (Luật 91/2025): Người dùng {} (Role: {}) đã giải mã xem chẩn đoán đơn #{}", currentUser, roleName, donId);

        return don.getChanDoanBenh();
    }

    public List<DonXinPhep> getRecentLeaves() {
        return donXinPhepRepository.findAll();
    }

    public List<NhanVien> getAllNhanVien() {
        return nhanVienRepository.findAll();
    }

    public Optional<NhanVien> getNhanVien(Long id) {
        return nhanVienRepository.findById(id);
    }

    private boolean isDateInBlackout(LocalDate start, LocalDate end) {
        return !(end.isBefore(BLACKOUT_START) || start.isAfter(BLACKOUT_END));
    }
}
