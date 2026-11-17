package com.gtsvn.hrm.controller;

import com.gtsvn.hrm.dto.ForensicResultDTO;
import com.gtsvn.hrm.dto.InspectionReportDTO;
import com.gtsvn.hrm.dto.MedicalAnalysisDTO;
import com.gtsvn.hrm.service.ForensicService;
import com.gtsvn.hrm.service.GeminiVisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RestController
@RequestMapping("/api/forensic")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ForensicController {

    private final ForensicService forensicService;
    private final GeminiVisionService geminiVisionService;

    @PostMapping("/inspect")
    public ResponseEntity<InspectionReportDTO> inspectImage(
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Nhận yêu cầu giám định toàn diện: file={}, size={} bytes", 
                file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // 1. Phân tích pháp y EXIF
            ForensicResultDTO forensicResult;
            try (InputStream is = file.getInputStream()) {
                forensicResult = forensicService.inspectImage(is);
            }

            // 2. Phân tích nội dung y tế & đọc chữ qua Gemini Vision
            byte[] fileBytes = file.getBytes();
            MedicalAnalysisDTO medicalResult = geminiVisionService.analyzeMedicalDocument(fileBytes, file.getOriginalFilename());

            // 3. Tổng hợp đánh giá và ra quyết định
            String overallRisk = "CO_XANH";
            String decision = "DUYET_NHANH";
            int confidence = (forensicResult.getConfidenceScore() + medicalResult.getDoTinCay()) / 2;

            if ("CO_DO".equals(forensicResult.getRiskFlag()) || "CO_DO".equals(medicalResult.getNhanRuiRo())) {
                overallRisk = "CO_DO";
                decision = "TU_CHOI_KHOA_DON";
            } else if ("CO_VANG".equals(forensicResult.getRiskFlag()) || "CO_VANG".equals(medicalResult.getNhanRuiRo()) || medicalResult.isChuVietTayMo()) {
                overallRisk = "CO_VANG";
                decision = "CAN_HR_SOI_LAI";
            } else if (medicalResult.isKhanCap()) {
                overallRisk = "CO_XANH";
                decision = "CHUYEN_GIAM_DOC_KHAN_CAP";
            }

            InspectionReportDTO report = InspectionReportDTO.builder()
                    .forensic(forensicResult)
                    .medical(medicalResult)
                    .overallRiskFlag(overallRisk)
                    .overallConfidence(confidence)
                    .finalDecision(decision)
                    .build();

            return ResponseEntity.ok(report);

        } catch (Exception e) {
            log.error("Lỗi khi xử lý giám định: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
