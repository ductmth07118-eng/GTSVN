package com.gtsvn.hrm.controller;

import com.gtsvn.hrm.dto.ContractDiffResponseDTO;
import com.gtsvn.hrm.service.ContractDiffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hop-dong")
@RequiredArgsConstructor
@Slf4j
public class ContractController {

    private final ContractDiffService contractDiffService;

    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> getAllContracts() {
        return ResponseEntity.ok(contractDiffService.getAllContracts());
    }

    @GetMapping("/{soHopDong}")
    public ResponseEntity<?> getContract(@PathVariable String soHopDong) {
        return contractDiffService.getContractBySoHopDong(soHopDong)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/compare")
    public ResponseEntity<?> compareContract(
            @RequestParam("soHopDong") String soHopDong,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng chọn 1 file ảnh hợp đồng scan!"));
            }

            // Lưu file scan vào thư mục uploads
            String uploadsDir = "uploads";
            Path uploadPath = Paths.get(uploadsDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, file.getBytes());
            String savedFileUrl = "uploads/" + filename;

            ContractDiffResponseDTO result = contractDiffService.compareContractScan(
                    soHopDong, file.getBytes(), file.getOriginalFilename(), savedFileUrl);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi đối chiếu hợp đồng: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Demo 1 chạm phục vụ nghiệm thu / chấm điểm đồ án:
     * testType = 'HOP_LE', 'SUA_LUONG_15M_TO_25M', 'SUA_THOI_HAN_0_NGAY'
     */
    @PostMapping("/demo-diff")
    public ResponseEntity<?> demoDiff(
            @RequestParam(defaultValue = "HD-2026-089") String soHopDong,
            @RequestParam(defaultValue = "HOP_LE") String testType) {
        try {
            String fakeFilename;
            if ("SUA_LUONG_15M_TO_25M".equalsIgnoreCase(testType)) {
                fakeFilename = "hop_dong_scan_sua_luong_25m.jpg";
            } else if ("SUA_THOI_HAN_0_NGAY".equalsIgnoreCase(testType)) {
                fakeFilename = "hop_dong_scan_sua_thoi_han_0_ngay.jpg";
            } else {
                fakeFilename = "hop_dong_scan_hop_le_chuan.jpg";
            }

            byte[] dummyBytes = new byte[100];
            String savedFileUrl = "uploads/sample_" + fakeFilename;

            ContractDiffResponseDTO result = contractDiffService.compareContractScan(
                    soHopDong, dummyBytes, fakeFilename, savedFileUrl);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi demo diff: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
