package com.gtsvn.hrm.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Mã hóa dữ liệu At-Rest (Data-at-Rest Encryption) cho chẩn đoán bệnh án
 * Tuân thủ Điều 17 Luật Bảo vệ dữ liệu cá nhân số 91/2025/QH15.
 * Khi lưu xuống SQL Server, dữ liệu tự động mã hóa dạng Ciphertext tiền tố ENC:...
 * Khi đọc lên qua JPA, tự động giải mã minh bạch (Transparent Decryption).
 */
@Converter
@Slf4j
public class AesCryptoConverter implements AttributeConverter<String, String> {

    // Khóa bí mật AES 128-bit nội bộ doanh nghiệp GTS (Trong thực tế nạp từ Key Vault / HSM)
    private static final String SECRET_KEY = "GtsHrmSecurity26"; // 16 bytes
    private static final String ALGORITHM = "AES";
    private static final String PREFIX = "ENC:";

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return attribute;
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Lỗi khi mã hóa At-Rest dữ liệu y tế: {}", e.getMessage());
            return attribute;
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || !dbData.startsWith(PREFIX)) {
            return dbData;
        }
        try {
            String ciphertext = dbData.substring(PREFIX.length());
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Lỗi khi giải mã At-Rest dữ liệu y tế: {}", e.getMessage());
            return dbData;
        }
    }
}
