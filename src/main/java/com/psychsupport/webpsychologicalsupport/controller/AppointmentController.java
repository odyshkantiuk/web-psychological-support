package com.psychsupport.webpsychologicalsupport.controller;

import com.psychsupport.webpsychologicalsupport.dto.AppointmentDto;
import com.psychsupport.webpsychologicalsupport.dto.TimeSlotDto;
import com.psychsupport.webpsychologicalsupport.model.Appointment;
import com.psychsupport.webpsychologicalsupport.service.AppointmentService;
import com.psychsupport.webpsychologicalsupport.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AvailabilityService availabilityService;

    @GetMapping
    public ResponseEntity<List<AppointmentDto>> getAllAppointments(@RequestParam Long psychologistId) {
        List<AppointmentDto> appointments = appointmentService.getPsychologistAppointments(psychologistId);
        return ResponseEntity.ok(appointments);
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String createAppointment(
            @RequestParam Long clientId,
            @RequestParam Long psychologistId,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {
        
        try {
            AppointmentDto appointmentDto = new AppointmentDto();
            appointmentDto.setClientId(clientId);
            appointmentDto.setPsychologistId(psychologistId);
            appointmentDto.setStartTime(LocalDateTime.parse(startTime));
            appointmentDto.setEndTime(LocalDateTime.parse(endTime));
            appointmentDto.setNotes(notes);
            
            appointmentService.createAppointment(appointmentDto);
            redirectAttributes.addFlashAttribute("success", "Appointment scheduled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to schedule appointment: " + e.getMessage());
        }
        
        return "redirect:/client/appointments";
    }

    @GetMapping("/psychologist/{psychologistId}")
    public ResponseEntity<List<AppointmentDto>> getPsychologistAppointments(@PathVariable Long psychologistId) {
        List<AppointmentDto> appointments = appointmentService.getPsychologistAppointments(psychologistId);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<AppointmentDto>> getClientAppointments(@PathVariable Long clientId) {
        List<AppointmentDto> appointments = appointmentService.getClientAppointments(clientId);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDto> getAppointmentById(@PathVariable Long id) {
        AppointmentDto appointment = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(appointment);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentDto> updateAppointment(@PathVariable Long id, @Valid @RequestBody AppointmentDto appointmentDto) {
        AppointmentDto updatedAppointment = appointmentService.updateAppointment(id, appointmentDto);
        return ResponseEntity.ok(updatedAppointment);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentDto> updateAppointmentStatus(
            @PathVariable Long id, 
            @RequestParam Appointment.Status status,
            @RequestParam Long psychologistId,
            @RequestParam(required = false) Long clientId,
            @RequestBody(required = false) Map<String, String> requestBody) {
        
        String reason = null;
        if (requestBody != null && requestBody.containsKey("notes")) {
            reason = requestBody.get("notes");
        }
        
        AppointmentDto updatedAppointment = reason != null ? 
            appointmentService.updateAppointmentStatus(id, status, reason) :
            appointmentService.updateAppointmentStatus(id, status);
            
        return ResponseEntity.ok(updatedAppointment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable Long id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available")
    public ResponseEntity<List<TimeSlotDto>> getAvailableTimeSlots(
            @RequestParam Long psychologistId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<TimeSlotDto> availableSlots = availabilityService.getAvailableTimeSlots(psychologistId, date);
        return ResponseEntity.ok(availableSlots);
    }
}