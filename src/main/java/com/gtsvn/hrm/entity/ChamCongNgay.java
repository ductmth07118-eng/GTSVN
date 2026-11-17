package com.gtsvn.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "cham_cong_ngay")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChamCongNgay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long nhanVienId;

    @Column(nullable = false)
    private LocalDate ngayLamViec;

    @Builder.Default
    private Double gioLich = 8.0;

    @Builder.Default
    private Double gioThucTe = 8.0;

    @Builder.Default
    private Double gioNghiHuongLuong = 0.0;

    @Builder.Default
    private Double gioNghiBhxh = 0.0;

    @Column(length = 10, nullable = false)
    @Builder.Default
    private String maCong = "X"; // 'X' (Đi làm), 'P' (Phép năm), 'VR' (Việc riêng 100%), 'O' (Ốm BHXH), 'KL' (Không lương)

    private Long donXinPhepId;
}
