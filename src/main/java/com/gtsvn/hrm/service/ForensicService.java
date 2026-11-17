package com.gtsvn.hrm.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.xmp.XmpDirectory;
import com.gtsvn.hrm.dto.ForensicResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class ForensicService {

    private static final List<String> BLACKLIST_EDITING_TOOLS = List.of(
            "photoshop", "canva", "gimp", "paint.net", "photopea",
            "picsart", "pixlr", "snapseed", "vsco", "meitu", "facetune",
            "b612", "lightroom", "affinity"
    );

    public ForensicResultDTO inspectImage(InputStream imageStream) {
        List<String> warnings = new ArrayList<>();
        String detectedSoftware = null;
        String detectedCamera = null;
        String dateOriginal = null;
        String dateModified = null;
        boolean hasExif = false;

        try (BufferedInputStream bis = new BufferedInputStream(imageStream)) {
            Metadata metadata = ImageMetadataReader.readMetadata(bis);

            // 1. Kiểm tra EXIF IFD0 (Camera Make, Model, Software, DateTime)
            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0 != null) {
                hasExif = true;
                String make = ifd0.getString(ExifIFD0Directory.TAG_MAKE);
                String model = ifd0.getString(ExifIFD0Directory.TAG_MODEL);
                if (make != null || model != null) {
                    detectedCamera = String.format("%s %s", make != null ? make.trim() : "", model != null ? model.trim() : "").trim();
                }

                String software = ifd0.getString(ExifIFD0Directory.TAG_SOFTWARE);
                if (software != null && !software.isBlank()) {
                    detectedSoftware = software.trim();
                }

                dateModified = ifd0.getString(ExifIFD0Directory.TAG_DATETIME);
            }

            // 2. Kiểm tra EXIF SubIFD (Thời gian chụp gốc)
            ExifSubIFDDirectory subIfd = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subIfd != null) {
                hasExif = true;
                dateOriginal = subIfd.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            }

            // 3. Quét sâu tầng XMP Metadata (Nơi Adobe Photoshop & Canva luôn để lại dấu vết)
            XmpDirectory xmp = metadata.getFirstDirectoryOfType(XmpDirectory.class);
            if (xmp != null) {
                hasExif = true;
                for (Tag tag : xmp.getTags()) {
                    String desc = tag.getDescription();
                    if (desc != null) {
                        String lowerDesc = desc.toLowerCase(Locale.ROOT);
                        for (String tool : BLACKLIST_EDITING_TOOLS) {
                            if (lowerDesc.contains(tool)) {
                                if (detectedSoftware == null) {
                                    detectedSoftware = tool.toUpperCase() + " (Phát hiện trong XMP Data)";
                                }
                                warnings.add("Phát hiện chuỗi nhận diện công cụ chỉnh sửa trong XMP: " + tag.getTagName());
                            }
                        }
                    }
                }
            }

            // 4. Quét toàn bộ các Tag còn lại nếu chưa phát hiện
            if (detectedSoftware == null) {
                for (Directory directory : metadata.getDirectories()) {
                    for (Tag tag : directory.getTags()) {
                        String tagName = tag.getTagName().toLowerCase(Locale.ROOT);
                        String desc = tag.getDescription() != null ? tag.getDescription().toLowerCase(Locale.ROOT) : "";
                        
                        if (tagName.contains("software") || tagName.contains("creatortool") || tagName.contains("history")) {
                            for (String tool : BLACKLIST_EDITING_TOOLS) {
                                if (desc.contains(tool)) {
                                    detectedSoftware = tag.getDescription();
                                    warnings.add("Dấu vết can thiệp tại " + tag.getTagName() + ": " + tag.getDescription());
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Không thể bóc tách EXIF hoặc file không hỗ trợ siêu dữ liệu: {}", e.getMessage());
            hasExif = false;
        }

        // ==========================================
        // 5. ĐÁNH GIÁ PHÂN LUỒNG RỦI RO (TRIAGE LOGIC)
        // ==========================================

        // TRƯỜNG HỢP 1: Có dấu vết phần mềm biên tập đồ họa -> CỜ VÀNG (Cần HR xác minh, không tự ý kết luận gian lận)
        if (detectedSoftware != null) {
            String lowerSoft = detectedSoftware.toLowerCase(Locale.ROOT);
            for (String tool : BLACKLIST_EDITING_TOOLS) {
                if (lowerSoft.contains(tool)) {
                    warnings.add("CẢNH BÁO GIÁM ĐỊNH TÍNH TOÀN VẸN: Ảnh có siêu dữ liệu từ công cụ: " + detectedSoftware);
                    return ForensicResultDTO.builder()
                            .clean(false)
                            .riskFlag("CO_VANG") // Cờ Vàng để HR xác minh, tuân thủ nguyên tắc không để máy móc tự tước quyền
                            .confidenceScore(40)
                            .hasExif(true)
                            .detectedSoftware(detectedSoftware)
                            .detectedCamera(detectedCamera)
                            .dateOriginal(dateOriginal)
                            .dateModified(dateModified)
                            .summaryMessage("CẢNH BÁO METADATA: Phát hiện siêu dữ liệu từ công cụ (" + detectedSoftware + "). Cần HR đối chiếu chứng từ gốc theo Luật Bảo vệ DLCN 2025.")
                            .warningList(warnings)
                            .build();
                }
            }
        }

        // TRƯỜNG HỢP 2: Hoàn toàn không có EXIF (Ảnh nén Zalo, OTT hoặc chụp màn hình) -> Thông tin bình thường
        if (!hasExif) {
            warnings.add("Không tìm thấy siêu dữ liệu EXIF. Khả năng cao ảnh đã được gửi nén qua ứng dụng nhắn tin OTT (Zalo, Messenger) hoặc ảnh chụp màn hình.");
            return ForensicResultDTO.builder()
                    .clean(true)
                    .riskFlag("CO_VANG")
                    .confidenceScore(70)
                    .hasExif(false)
                    .detectedSoftware(null)
                    .detectedCamera(null)
                    .dateOriginal(null)
                    .dateModified(null)
                    .summaryMessage("THÔNG TIN: Ảnh không chứa EXIF (thường do nén qua Zalo/OTT). Chuyển tiếp sang Document Vision kiểm tra tính toàn vẹn tài liệu.")
                    .warningList(warnings)
                    .build();
        }

        // TRƯỜNG HỢP 3: Có EXIF hợp lệ từ thiết bị máy ảnh/điện thoại, không có tool chỉnh sửa -> CỜ XANH
        return ForensicResultDTO.builder()
                .clean(true)
                .riskFlag("CO_XANH")
                .confidenceScore(98)
                .hasExif(true)
                .detectedSoftware(detectedSoftware != null ? detectedSoftware : "Không có phần mềm can thiệp")
                .detectedCamera(detectedCamera != null ? detectedCamera : "Thiết bị chụp thực tế")
                .dateOriginal(dateOriginal)
                .dateModified(dateModified)
                .summaryMessage("HỢP LỆ: Chứng từ có siêu dữ liệu chụp từ thiết bị " + (detectedCamera != null ? detectedCamera : "gốc") + ", không phát hiện công cụ đồ họa.")
                .warningList(warnings)
                .build();
    }
}
