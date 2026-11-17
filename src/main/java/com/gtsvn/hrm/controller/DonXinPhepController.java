package com.gtsvn.hrm.controller;

import com.gtsvn.hrm.dto.DonXinPhepSubmitResponseDTO;
import com.gtsvn.hrm.entity.DonXinPhep;
import com.gtsvn.hrm.entity.LichSuTruyCap;
import com.gtsvn.hrm.entity.NhanVien;
import com.gtsvn.hrm.repository.LichSuTruyCapRepository;
import com.gtsvn.hrm.service.DonXinPhepService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/don-xin-phep")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DonXinPhepController {

    private final DonXinPhepService donXinPhepService;
    private final LichSuTruyCapRepository auditRepo;

    @PostMapping("/submit")
    public ResponseEntity<?> submit(
            @RequestParam("nhanVienId") Long nhanVienId,
            @RequestParam("loaiNghiPhep") String loaiNghiPhep,
            @RequestParam("ngayBatDau") String ngayBatDau,
            @RequestParam("ngayKetThuc") String ngayKetThuc,
            @RequestParam(value = "buoiNghi", defaultValue = "CA_NGAY") String buoiNghi,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            LocalDate start = LocalDate.parse(ngayBatDau);
            LocalDate end = LocalDate.parse(ngayKetThuc);

            DonXinPhepSubmitResponseDTO response = donXinPhepService.submitLeaveRequest(
                    nhanVienId, loaiNghiPhep, start, end, buoiNghi, file
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi nộp đơn: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/nhan-vien/{id}")
    public ResponseEntity<?> getNhanVien(@PathVariable Long id) {
        return donXinPhepService.getNhanVien(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nhan-vien/{id}/timesheet")
    public ResponseEntity<?> getTimesheet(@PathVariable Long id) {
        return ResponseEntity.ok(donXinPhepService.getNhanVienTimesheet(id));
    }

    @GetMapping("/nhan-vien/list")
    public ResponseEntity<List<NhanVien>> getAllNhanVien() {
        return ResponseEntity.ok(donXinPhepService.getAllNhanVien());
    }

    @GetMapping("/list")
    public ResponseEntity<List<DonXinPhep>> getRecentLeaves() {
        return ResponseEntity.ok(donXinPhepService.getRecentLeaves());
    }

    @PostMapping("/{id}/decrypt")
    public ResponseEntity<?> decrypt(
            @PathVariable Long id,
            @RequestParam(value = "role", defaultValue = "ROLE_HR_CNB") String role,
            HttpServletRequest request
    ) {
        try {
            String ip = request.getRemoteAddr();
            String currentUser = "duc.tm (Minh Đức Trịnh)";
            String diagnosis = donXinPhepService.decryptAndAuditDiagnosis(id, currentUser, role, ip);
            return ResponseEntity.ok(Map.of("donId", id, "chanDoan", diagnosis));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Long id,
            @RequestParam(value = "approver", defaultValue = "duc.tm (HR Lead)") String approver
    ) {
        try {
            Map<String, Object> result = donXinPhepService.approveLeave(id, approver);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<LichSuTruyCap>> getAuditLogs() {
        return ResponseEntity.ok(auditRepo.findAll());
    }
}
