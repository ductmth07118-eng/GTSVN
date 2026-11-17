package com.gtsvn.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "LichSuTruyCap")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichSuTruyCap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String taiKhoanTruyCap;

    @Column(nullable = false, length = 50)
    private String roleName;

    @Column(nullable = false)
    private Long donXinPhepId;

    @Column(nullable = false, length = 100)
    private String hanhDong; // 'XEM_CHAN_DOAN_BENH_NHAY_CAM'

    @Column(length = 50)
    private String ipAddress;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
