package com.gtsvn.hrm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForensicResultDTO {

    private boolean clean;
    private String riskFlag;         // 'CO_XANH', 'CO_VANG', 'CO_DO'
    private int confidenceScore;     // 0 - 100
    private boolean hasExif;
    private String detectedSoftware; // VD: Adobe Photoshop 2024, Canva
    private String detectedCamera;   // VD: Apple iPhone 14 Pro, Samsung Galaxy S23
    private String dateOriginal;     // Thời gian chụp ảnh gốc
    private String dateModified;     // Thời gian sửa ảnh (nếu có)
    private String summaryMessage;   // Thông điệp tóm tắt cho HR
    private List<String> warningList; // Danh sách các dấu vết cảnh báo
}
