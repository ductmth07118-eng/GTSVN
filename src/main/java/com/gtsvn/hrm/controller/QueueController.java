package com.gtsvn.hrm.controller;

import com.gtsvn.hrm.entity.DeadLetterQueue;
import com.gtsvn.hrm.service.ResilientQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
@Slf4j
public class QueueController {

    private final ResilientQueueService resilientQueueService;

    @GetMapping("/list")
    public ResponseEntity<List<DeadLetterQueue>> getQueueList() {
        return ResponseEntity.ok(resilientQueueService.getAllQueueItems());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQueueStats() {
        return ResponseEntity.ok(resilientQueueService.getQueueStats());
    }

    /**
     * Demo Mô Phỏng Rớt Mạng / AI Timeout:
     * Kích hoạt @Retryable thử lại 3 lần rồi @Recover đẩy vào Dead Letter Queue.
     */
    @PostMapping("/simulate-failure")
    public ResponseEntity<?> simulateFailure(
            @RequestParam(defaultValue = "OCR_GIAY_KHAM") String processType,
            @RequestParam(defaultValue = "{\"nhanVienId\": 101, \"file\": \"giay_kham_tai_nan.jpg\", \"action\": \"ANALYZE_MEDICAL\"}") String payload) {
        try {
            Map<String, Object> result = resilientQueueService.executeWithResilience(processType, payload, true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Unhandled exception: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Kích hoạt worker dọn hàng đợi ngay lập tức
     */
    @PostMapping("/drain")
    public ResponseEntity<?> drainQueue() {
        Map<String, Object> result = resilientQueueService.manualDrainQueue();
        return ResponseEntity.ok(result);
    }
}
