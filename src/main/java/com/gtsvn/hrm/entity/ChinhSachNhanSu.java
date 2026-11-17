package com.gtsvn.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chinh_sach_nhan_su")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChinhSachNhanSu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String maDieuKhoan;

    @Column(nullable = false, columnDefinition = "NVARCHAR(300)")
    private String tieuDe;

    @Column(nullable = false, columnDefinition = "NVARCHAR(100)")
    private String danhMuc; // 'NGHI_PHEP', 'BHXH', 'BLACKOUT', 'PHUC_LOI', 'HOP_DONG', 'TIEN_LUONG'

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String noiDungTomTat;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String noiDungChiTiet;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String canCuPhapLy; // BLLĐ 2019, Luật BHXH 2024, Luật BV DLCN 91/2025/QH15

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String quyenLoiNhanVien;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String thuTucThucHien;

    @Column(length = 50)
    @Builder.Default
    private String hanhDongLienKet = "NONE"; // 'TAB_LEAVE', 'TAB_CONTRACT', 'NONE'

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
