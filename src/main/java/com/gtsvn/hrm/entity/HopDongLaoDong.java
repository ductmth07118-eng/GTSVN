package com.gtsvn.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hop_dong_lao_dong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HopDongLaoDong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long nhanVienId;

    @Column(nullable = false, unique = true, length = 100)
    private String soHopDong;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String jsonGoc; // {"MucLuong": 15000000, "ChucDanh": "Dev", "NgayBatDau": "2026-12-01"}

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String jsonScanBocTach; // AI bóc tách từ bản scan

    @Column(length = 500)
    private String fileScanUrl;

    @Column(length = 20)
    @Builder.Default
    private String ketQuaDoiChieu = "KHOP"; // 'KHOP', 'LECH_DIEU_KHOAN'

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String chiTietLech;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
