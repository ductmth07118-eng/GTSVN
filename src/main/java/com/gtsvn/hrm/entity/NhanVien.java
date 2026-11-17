package com.gtsvn.hrm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "nhan_vien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhanVien {

    @Id
    private Long id;

    @Nationalized
    @Column(nullable = false, length = 100)
    private String hoTen;

    @Column(length = 100)
    private String email;

    @Nationalized
    @Column(length = 100)
    private String phongBan;

    @Nationalized
    @Column(length = 100)
    private String chucVu;

    @Builder.Default
    private Double soNgayPhepConLai = 12.0;

    @Builder.Default
    private Double soNgayDaNghi = 0.0;
}
