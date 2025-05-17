package com.psychsupport.webpsychologicalsupport.service;

import com.psychsupport.webpsychologicalsupport.dto.AppointmentDto;
import com.psychsupport.webpsychologicalsupport.exception.InvalidAppointmentException;
import com.psychsupport.webpsychologicalsupport.exception.ResourceNotFoundException;
import com.psychsupport.webpsychologicalsupport.model.Appointment;
import com.psychsupport.webpsychologicalsupport.model.User;
import com.psychsupport.webpsychologicalsupport.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserService userService;
    private final AvailabilityService availabilityService;

    public AppointmentDto createAppointment(AppointmentDto appointmentDto) {
        validateAppointmentTime(appointmentDto);

        User psychologist = userService.getUserById(appointmentDto.getPsychologistId());
        User client = userService.getUserById(appointmentDto.getClientId());

        if (psychologist.getRole() != User.Role.PSYCHOLOGIST) {
            throw new InvalidAppointmentException("Selected user is not a psychologist");
        }

        if (!availabilityService.isTimeSlotAvailable(
                psychologist.getId(),
                appointmentDto.getStartTime(),
                appointmentDto.getEndTime())) {
            throw new InvalidAppointmentException("Selected time slot is not available");
        }

        Appointment appointment = new Appointment();
        appointment.setPsychologist(psychologist);
        appointment.setClient(client);
        appointment.setStartTime(appointmentDto.getStartTime());
        appointment.setEndTime(appointmentDto.getEndTime());
        appointment.setNotes(appointmentDto.getNotes());
        appointment.setStatus(Appointment.Status.SCHEDULED);

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return convertToDto(savedAppointment);
    }

    public List<AppointmentDto> getPsychologistAppointments(Long psychologistId) {
        User psychologist = userService.getUserById(psychologistId);
        return appointmentRepository.findByPsychologist(psychologist).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<AppointmentDto> getClientAppointments(Long clientId) {
        User client = userService.getUserById(clientId);
        return appointmentRepository.findByClient(client).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<AppointmentDto> getClientUpcomingAppointments(Long clientId) {
        User client = userService.getUserById(clientId);
        LocalDateTime now = LocalDateTime.now();
        return appointmentRepository.findByClient(client).stream()
                .filter(appointment -> appointment.getStartTime().isAfter(now))
                .filter(appointment -> appointment.getStatus() == Appointment.Status.SCHEDULED)
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public AppointmentDto getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
        return convertToDto(appointment);
    }

    public AppointmentDto updateAppointmentStatus(Long id, Appointment.Status status, String reason) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        appointment.setStatus(status);

        if (status == Appointment.Status.CANCELLED && reason != null && !reason.trim().isEmpty()) {
            String currentNotes = appointment.getNotes();
            String cancellationNote = "\nCancellation Reason: " + reason;
            appointment.setNotes(currentNotes != null ? currentNotes + cancellationNote : cancellationNote);
        }
        
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        return convertToDto(updatedAppointment);
    }

    public AppointmentDto updateAppointmentStatus(Long id, Appointment.Status status) {
        return updateAppointmentStatus(id, status, null);
    }

    public AppointmentDto updateAppointment(Long id, AppointmentDto appointmentDto) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        if (appointmentDto.getStartTime() != null && appointmentDto.getEndTime() != null) {
            validateAppointmentTime(appointmentDto);

            if (!availabilityService.isTimeSlotAvailable(
                    appointment.getPsychologist().getId(),
                    appointmentDto.getStartTime(),
                    appointmentDto.getEndTime())) {
                throw new InvalidAppointmentException("Selected time slot is not available");
            }

            appointment.setStartTime(appointmentDto.getStartTime());
            appointment.setEndTime(appointmentDto.getEndTime());
        }

        if (appointmentDto.getNotes() != null) {
            appointment.setNotes(appointmentDto.getNotes());
        }

        if (appointmentDto.getStatus() != null) {
            appointment.setStatus(appointmentDto.getStatus());
        }

        Appointment updatedAppointment = appointmentRepository.save(appointment);
        return convertToDto(updatedAppointment);
    }

    public void deleteAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
        appointmentRepository.delete(appointment);
    }

    public List<AppointmentDto> getAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private void validateAppointmentTime(AppointmentDto appointmentDto) {
        LocalDateTime now = LocalDateTime.now();

        if (appointmentDto.getStartTime().isBefore(now)) {
            throw new InvalidAppointmentException("Appointment cannot be scheduled in the past");
        }

        if (appointmentDto.getStartTime().isAfter(appointmentDto.getEndTime())) {
            throw new InvalidAppointmentException("Start time must be before end time");
        }
    }

    private AppointmentDto convertToDto(Appointment appointment) {
        AppointmentDto dto = new AppointmentDto();
        dto.setId(appointment.getId());
        dto.setPsychologistId(appointment.getPsychologist().getId());
        dto.setPsychologistName(appointment.getPsychologist().getFullName());
        dto.setClientId(appointment.getClient().getId());
        dto.setClientName(appointment.getClient().getFullName());
        dto.setStartTime(appointment.getStartTime());
        dto.setEndTime(appointment.getEndTime());
        dto.setNotes(appointment.getNotes());
        dto.setStatus(appointment.getStatus());
        dto.setCreatedAt(appointment.getCreatedAt());
        dto.setUpdatedAt(appointment.getUpdatedAt());
        return dto;
    }
}