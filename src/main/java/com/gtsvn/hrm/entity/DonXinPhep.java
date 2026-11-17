package com.gtsvn.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "DonXinPhep")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonXinPhep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long nhanVienId;

    @Column(nullable = false, length = 50)
    private String loaiNghiPhep; // 'NGHI_OM', 'VIEC_RIENG', 'THAI_SAN'

    @Column(nullable = false)
    private LocalDate ngayBatDau;

    @Column(nullable = false)
    private LocalDate ngayKetThuc;

    @Column(length = 20)
    @Builder.Default
    private String buoiNghi = "CA_NGAY"; // 'CA_NGAY', 'BUOI_SANG', 'BUOI_CHIEU'

    @Builder.Default
    private Double soNgayLamViecNghi = 1.0; // Số ngày làm việc thực tế (hỗ trợ 0.5 công nghỉ nửa ngày, loại trừ T7/CN/Lễ)

    @Column(length = 500)
    private String fileMinhChungUrl;

    @Column(length = 150)
    private String hoTenBenhNhan; // Họ tên người bệnh trích xuất từ chứng từ y tế

    @Builder.Default
    private Boolean isNameMatched = true; // Trùng khớp với nhân viên nộp đơn trong SQL Server

    @Column(columnDefinition = "NVARCHAR(MAX)")
    @Convert(converter = com.gtsvn.hrm.converter.AesCryptoConverter.class)
    private String chanDoanBenh; // Dữ liệu y tế nhạy cảm: Mã hóa At-Rest bằng AES và Role-Based Masking trên UI

    @Column(length = 50)
    @Builder.Default
    private String trangThai = "CHO_DUYET"; // 'CHO_DUYET', 'CHO_HR_XAC_MINH', 'CHO_GIAM_DOC_DUYET', 'DA_DUYET', 'TU_CHOI'

    @Column(length = 255)
    private String canhBaoFlags; // Cờ cảnh báo: 'CO_METADATA_BAT_THUONG,OCR_DO_TIN_CAY_THAP,CO_XUNG_DOT_BLACKOUT,CO_YEU_CAU_KHAN_CAP'

    // AI Forensics & Traffic Light
    @Builder.Default
    private Integer doTinCay = 100; // 0 - 100

    @Column(length = 20)
    @Builder.Default
    private String nhanRuiRo = "CO_XANH"; // 'CO_XANH', 'CO_VANG', 'CO_DO'

    @Column(length = 500)
    private String lyDoRuiRo;

    @Builder.Default
    private Boolean isKhanCap = false; // Bypass Blackout period

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
