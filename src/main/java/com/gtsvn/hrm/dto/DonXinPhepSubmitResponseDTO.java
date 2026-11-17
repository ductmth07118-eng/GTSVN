package com.gtsvn.hrm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonXinPhepSubmitResponseDTO {

    private Long donXinPhepId;
    private String tenNhanVien;
    private String phongBan;
    private String fileUrl;             // Đường dẫn URL file đã lưu vật lý
    private String buoiNghi;            // 'CA_NGAY', 'BUOI_SANG', 'BUOI_CHIEU'
    private Double soNgayLamViecNghi;   // Số ngày làm việc thực tế (hỗ trợ 0.5 công, loại trừ T7, CN, Lễ)
    private String hoTenBenhNhan;       // Họ tên người bệnh trên chứng từ
    private boolean isNameMatched;      // Có khớp với nhân viên nộp đơn không
    private String trangThai;           // 'CHO_DUYET', 'CHO_HR_XAC_MINH', 'CHO_GIAM_DOC_DUYET', 'TU_CHOI'
    private String canhBaoFlags;        // Cờ cảnh báo hỗ trợ HR
    private boolean blackoutBypassed;   // True nếu đã chọc thủng rào cản cấm nghỉ
    private String thongBao;
    private InspectionReportDTO report;
}
