package com.psychsupport.webpsychologicalsupport.dto;

import com.psychsupport.webpsychologicalsupport.model.Appointment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto {
    private Long id;
    private Long psychologistId;
    private String psychologistName;
    private Long clientId;
    private String clientName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String notes;
    private Appointment.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}