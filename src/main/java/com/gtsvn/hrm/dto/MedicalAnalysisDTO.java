package com.gtsvn.hrm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalAnalysisDTO {

    @Builder.Default
    private boolean isMedicalDocument = true; // False nếu phát hiện ảnh game, ảnh meme, ảnh tự sướng, phong cảnh...

    private String hoTenBenhNhan;             // Họ tên bệnh nhân trích xuất từ chứng từ y tế

    @Builder.Default
    private boolean isNameMatched = true;     // Có trùng khớp với nhân viên nộp đơn không

    private String benhVien;         // Tên bệnh viện / Cơ sở y tế
    private String chanDoan;         // Chẩn đoán bệnh
    private LocalDate ngayKham;      // Ngày khám
    private Integer soNgayNghi;      // Số ngày bác sĩ chỉ định nghỉ
    private boolean isKhanCap;       // Có từ khóa "Cấp cứu", "Nhập viện" không
    private boolean coDauMocDo;      // Có dấu mộc tròn đỏ không
    private boolean chuVietTayMo;    // Chữ viết tay có bị mờ/khó đọc không
    private int doTinCay;            // 0 - 100
    private String nhanRuiRo;        // CO_XANH, CO_VANG, CO_DO
    private String lyDoDanhGia;      // Lời giải thích chi tiết của AI
    private String nguonXuLy;        // "GEMINI_1_5_FLASH" hoặc "MOCK_AI_DEMO"
}
