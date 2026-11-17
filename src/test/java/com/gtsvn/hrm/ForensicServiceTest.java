package com.gtsvn.hrm;

import com.gtsvn.hrm.dto.ForensicResultDTO;
import com.gtsvn.hrm.service.ForensicService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ForensicServiceTest {

    @Autowired
    private ForensicService forensicService;

    @Test
    @DisplayName("Kiểm tra ảnh không có EXIF (giả lập ảnh tải qua Zalo) -> Trả về CỜ VÀNG")
    void testImageWithoutExifShouldReturnYellowFlag() {
        // Tạo stream rỗng hoặc mảng byte không có header EXIF
        byte[] dummyImageBytes = new byte[]{ (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00 };
        InputStream is = new ByteArrayInputStream(dummyImageBytes);

        ForensicResultDTO result = forensicService.inspectImage(is);

        assertNotNull(result);
        assertEquals("CO_VANG", result.getRiskFlag());
        assertFalse(result.isHasExif());
        assertTrue(result.getConfidenceScore() < 80);
        assertTrue(result.getSummaryMessage().contains("CỜ VÀNG"));
    }
}
