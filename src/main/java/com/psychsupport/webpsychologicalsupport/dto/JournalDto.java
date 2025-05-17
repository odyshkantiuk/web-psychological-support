package com.psychsupport.webpsychologicalsupport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalDto {
    private Long id;
    private Long userId;
    private String userName;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer moodRating;
    private Long sharedWithPsychologistId;
    private String sharedWithPsychologistName;
    private boolean removeSharing;
}