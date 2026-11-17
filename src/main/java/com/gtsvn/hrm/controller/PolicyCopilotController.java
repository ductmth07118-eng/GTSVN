package com.gtsvn.hrm.controller;

import com.gtsvn.hrm.dto.PolicyAnswerResponseDTO;
import com.gtsvn.hrm.dto.PolicyQuestionRequestDTO;
import com.gtsvn.hrm.entity.ChinhSachNhanSu;
import com.gtsvn.hrm.service.PolicyCopilotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/policy")
@RequiredArgsConstructor
@Slf4j
public class PolicyCopilotController {

    private final PolicyCopilotService policyCopilotService;

    @GetMapping("/articles")
    public ResponseEntity<List<ChinhSachNhanSu>> getArticles(
            @RequestParam(required = false, defaultValue = "ALL") String category) {
        return ResponseEntity.ok(policyCopilotService.getPoliciesByCategory(category));
    }

    @PostMapping("/ask")
    public ResponseEntity<PolicyAnswerResponseDTO> askPolicy(
            @RequestBody PolicyQuestionRequestDTO request) {
        return ResponseEntity.ok(policyCopilotService.askPolicyCopilot(request));
    }

    @GetMapping("/suggested-questions")
    public ResponseEntity<List<Map<String, String>>> getSuggestedQuestions() {
        return ResponseEntity.ok(List.of(
                Map.of("category", "Nghỉ phép năm", "question", "Quy định số ngày nghỉ phép năm và cách tính nghỉ nửa ngày (0.5 công)?"),
                Map.of("category", "Bảo hiểm xã hội", "question", "Bị ốm đau hoặc nằm viện thì được BHXH chi trả bao nhiêu tiền lương và cần giấy tờ gì?"),
                Map.of("category", "Blackout Period", "question", "Đợt cao điểm Blackout 10/11 - 20/11 có được nghỉ không, ca cấp cứu thì xử lý thế nào?"),
                Map.of("category", "Hợp đồng lao động", "question", "Thời hạn báo trước khi thôi việc là bao nhiêu ngày và làm sao biết hợp đồng không bị sửa lén?"),
                Map.of("category", "Việc riêng hưởng lương", "question", "Kết hôn hoặc người thân qua đời thì được nghỉ mấy ngày hưởng nguyên lương?")
        ));
    }
}
