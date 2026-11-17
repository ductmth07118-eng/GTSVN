package com.gtsvn.hrm.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractEntityDTO {
    private String hoTen;
    private String chucDanh;
    private String phongBan;
    private Double mucLuong;
    private String mucLuongChu;
    private String ngayBatDau;
    private Integer thoiHanBaoTruoc; // Số ngày báo trước khi chấm dứt HĐ
    private String diaDiemLamViec;
    private String thoiGioLamViec;
    private String loaiHopDong;
}
