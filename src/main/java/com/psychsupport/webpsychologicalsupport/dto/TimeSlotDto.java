package com.psychsupport.webpsychologicalsupport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDto {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean available;
}