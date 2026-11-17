package com.gtsvn.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dead_letter_queue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeadLetterQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String loaiTienTrinh; // 'OCR_GIAY_KHAM', 'DIFF_HOP_DONG'

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String payload;

    @Builder.Default
    private Integer soLanDaThu = 0;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String loiCuoiCung;

    @Column(length = 30)
    @Builder.Default
    private String trangThai = "PENDING"; // 'PENDING', 'RESOLVED', 'FAILED'

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
