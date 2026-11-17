package com.gtsvn.hrm.dto;

import com.gtsvn.hrm.entity.ChinhSachNhanSu;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyAnswerResponseDTO {
    private String question;
    private String answerSummary;
    private String answerDetail;
    private String legalBasis;
    private String actionType;    // "TAB_LEAVE", "TAB_CONTRACT", "NONE"
    private String actionLabel;   // e.g. "Tạo Đơn Nghỉ Phép / Khám Bệnh"
    private String actionTarget;  // "forensic-tab", "contract-tab"
    private String aiSource;      // "GOOGLE_GEMINI_LIVE_API" hoặc "GTS_KNOWLEDGE_BASE"
    private List<ChinhSachNhanSu> relatedArticles;
}
