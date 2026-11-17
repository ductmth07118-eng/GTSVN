package com.gtsvn.hrm.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractDiffResponseDTO {
    private String soHopDong;
    private Long nhanVienId;
    private String tenNhanVien;
    private String loaiHopDong;
    private String trangThaiDoiChieu; // "KHOP" (Cờ Xanh), "LECH_DIEU_KHOAN" (Cờ Đỏ)
    private int diffCount;
    private List<ContractDiffItemDTO> items;
    private String fileScanUrl;
    private int aiConfidence;
    private String canhBaoPhapLy;
    private String nguonXuLy;
    private ContractEntityDTO originalContract;
    private ContractEntityDTO scannedContract;
}
