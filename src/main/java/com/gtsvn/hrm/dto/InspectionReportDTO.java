package com.gtsvn.hrm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionReportDTO {

    private ForensicResultDTO forensic;
    private MedicalAnalysisDTO medical;
    private String finalDecision;    // 'DUYET_NHANH', 'CHUYEN_GIAM_DOC_KHAN_CAP', 'CAN_HR_SOI_LAI', 'TU_CHOI_KHOA_DON'
    private String overallRiskFlag;  // 'CO_XANH', 'CO_VANG', 'CO_DO'
    private int overallConfidence;   // 0 - 100
}
