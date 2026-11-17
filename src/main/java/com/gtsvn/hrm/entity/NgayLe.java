package com.gtsvn.hrm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "danh_muc_ngay_le")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NgayLe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate ngayLe;

    @Column(nullable = false, length = 150)
    private String tenNgayLe; // Tết Dương Lịch, Tết Âm Lịch, 30/4, 1/5, Quốc Khánh...

    private Integer nam; // 2026
}
