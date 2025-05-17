package com.psychsupport.webpsychologicalsupport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityDto {
    private List<Map<String, Object>> weeklySchedule;
    private Map<String, Object> appointmentSettings;
    private List<Map<String, Object>> exceptions;
}