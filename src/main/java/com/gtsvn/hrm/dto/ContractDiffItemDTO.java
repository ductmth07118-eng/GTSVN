package com.gtsvn.hrm.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractDiffItemDTO {
    private String fieldKey;       // e.g. "mucLuong", "chucDanh"
    private String fieldName;      // e.g. "Mức Lương Cơ Bản"
    private String originalValue;  // e.g. "15,000,000 VNĐ"
    private String scannedValue;   // e.g. "25,000,000 VNĐ"
    private boolean isMatched;     // true nếu trùng khớp
    private String severity;       // "AN_TOAN", "CANH_BAO", "NGHIEM_TRONG"
    private String description;    // Chi tiết phân tích sai lệch
}
