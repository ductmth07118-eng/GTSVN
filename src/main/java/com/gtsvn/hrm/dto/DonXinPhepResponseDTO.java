package com.gtsvn.hrm.dto;

import com.gtsvn.hrm.entity.DonXinPhep;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonXinPhepResponseDTO {

    private Long id;
    private Long nhanVienId;
    private String loaiNghiPhep;
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    private String fileMinhChungUrl;
    private String chanDoanBenh; // Sẽ bị che nếu không phải ROLE_HR_CNB
    private String trangThai;

    // AI Forensics
    private Integer doTinCay;
    private String nhanRuiRo;
    private String lyDoRuiRo;
    private Boolean isKhanCap;
    private LocalDateTime createdAt;

    public static DonXinPhepResponseDTO fromEntity(DonXinPhep entity) {
        return DonXinPhepResponseDTO.builder()
                .id(entity.getId())
                .nhanVienId(entity.getNhanVienId())
                .loaiNghiPhep(entity.getLoaiNghiPhep())
                .ngayBatDau(entity.getNgayBatDau())
                .ngayKetThuc(entity.getNgayKetThuc())
                .fileMinhChungUrl(entity.getFileMinhChungUrl())
                .chanDoanBenh(entity.getChanDoanBenh())
                .trangThai(entity.getTrangThai())
                .doTinCay(entity.getDoTinCay())
                .nhanRuiRo(entity.getNhanRuiRo())
                .lyDoRuiRo(entity.getLyDoRuiRo())
                .isKhanCap(entity.getIsKhanCap())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
