package com.gtsvn.hrm.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyQuestionRequestDTO {
    private String question;
    private Long employeeId;
    private String category;
}
