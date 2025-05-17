package com.psychsupport.webpsychologicalsupport.service;

import com.psychsupport.webpsychologicalsupport.dto.TimeSlotDto;
import com.psychsupport.webpsychologicalsupport.model.Appointment;
import com.psychsupport.webpsychologicalsupport.model.User;
import com.psychsupport.webpsychologicalsupport.model.Availability;
import com.psychsupport.webpsychologicalsupport.repository.AppointmentRepository;
import com.psychsupport.webpsychologicalsupport.repository.AvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserService userService;

    private static final int DEFAULT_APPOINTMENT_DURATION = 60;
    private static final int DEFAULT_BUFFER_TIME = 15;

    public List<TimeSlotDto> getAvailableTimeSlots(Long psychologistId, LocalDate date) {
        User psychologist = userService.getUserById(psychologistId);

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<Availability> availabilityList = availabilityRepository.findByPsychologistAndDayOfWeek(psychologist, dayOfWeek);

        if (availabilityList.isEmpty()) {
            return new ArrayList<>();
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        List<Appointment> existingAppointments = appointmentRepository.findByPsychologistAndStartTimeBetween(
                psychologist, startOfDay, endOfDay);

        List<TimeSlotDto> availableSlots = new ArrayList<>();

        for (Availability availability : availabilityList) {
            LocalTime startTime = availability.getStartTime();
            LocalTime endTime = availability.getEndTime();

            LocalDateTime currentSlotStart = LocalDateTime.of(date, startTime);
            LocalDateTime availabilityEnd = LocalDateTime.of(date, endTime);

            LocalDateTime now = LocalDateTime.now();
            if (currentSlotStart.isBefore(now)) {
                currentSlotStart = now.plusMinutes(DEFAULT_BUFFER_TIME);
                int minute = currentSlotStart.getMinute();
                if (minute % 30 != 0) {
                    currentSlotStart = currentSlotStart.plusMinutes(30 - (minute % 30));
                }
            }

            while (currentSlotStart.plusMinutes(DEFAULT_APPOINTMENT_DURATION).isBefore(availabilityEnd) ||
                    currentSlotStart.plusMinutes(DEFAULT_APPOINTMENT_DURATION).equals(availabilityEnd)) {

                LocalDateTime slotEnd = currentSlotStart.plusMinutes(DEFAULT_APPOINTMENT_DURATION);

                boolean isAvailable = true;
                for (Appointment appointment : existingAppointments) {
                    if (appointment.getStatus() == Appointment.Status.SCHEDULED ||
                            appointment.getStatus() == Appointment.Status.COMPLETED) {

                        if (currentSlotStart.isBefore(appointment.getEndTime()) &&
                                appointment.getStartTime().isBefore(slotEnd)) {
                            isAvailable = false;
                            break;
                        }
                    }
                }

                if (isAvailable) {
                    TimeSlotDto slot = new TimeSlotDto();
                    slot.setStartTime(currentSlotStart);
                    slot.setEndTime(slotEnd);
                    slot.setAvailable(true);
                    availableSlots.add(slot);
                }

                currentSlotStart = currentSlotStart.plusMinutes(DEFAULT_APPOINTMENT_DURATION + DEFAULT_BUFFER_TIME);
            }
        }

        return availableSlots;
    }

    public void saveAvailability(Availability availability) {
        availabilityRepository.save(availability);
    }

    public List<Availability> getPsychologistAvailability(Long psychologistId) {
        User psychologist = userService.getUserById(psychologistId);
        return availabilityRepository.findByPsychologist(psychologist);
    }

    public void deleteAvailability(Long availabilityId) {
        availabilityRepository.deleteById(availabilityId);
    }

    public void clearDayAvailability(Long psychologistId, DayOfWeek dayOfWeek) {
        User psychologist = userService.getUserById(psychologistId);
        List<Availability> availabilityList = availabilityRepository.findByPsychologistAndDayOfWeek(psychologist, dayOfWeek);
        availabilityRepository.deleteAll(availabilityList);
    }

    public void setDefaultAvailability(Long psychologistId) {
        User psychologist = userService.getUserById(psychologistId);

        availabilityRepository.deleteAll(availabilityRepository.findByPsychologist(psychologist));

        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(17, 0);

        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                continue;
            }

            Availability availability = new Availability();
            availability.setPsychologist(psychologist);
            availability.setDayOfWeek(day);
            availability.setStartTime(startTime);
            availability.setEndTime(endTime);

            availabilityRepository.save(availability);
        }
    }

    public boolean isTimeSlotAvailable(Long psychologistId, LocalDateTime startTime, LocalDateTime endTime) {
        User psychologist = userService.getUserById(psychologistId);

        DayOfWeek dayOfWeek = startTime.getDayOfWeek();
        List<Availability> availabilityList = availabilityRepository.findByPsychologistAndDayOfWeek(psychologist, dayOfWeek);

        boolean withinAvailability = false;
        for (Availability availability : availabilityList) {
            LocalTime availStartTime = availability.getStartTime();
            LocalTime availEndTime = availability.getEndTime();

            LocalTime timeStart = startTime.toLocalTime();
            LocalTime timeEnd = endTime.toLocalTime();

            if ((timeStart.equals(availStartTime) || timeStart.isAfter(availStartTime)) &&
                    (timeEnd.equals(availEndTime) || timeEnd.isBefore(availEndTime))) {
                withinAvailability = true;
                break;
            }
        }

        if (!withinAvailability) {
            return false;
        }

        List<Appointment> existingAppointments = appointmentRepository.findByPsychologistAndStartTimeBetween(
                psychologist,
                startTime.toLocalDate().atStartOfDay(),
                startTime.toLocalDate().atTime(23, 59, 59));

        for (Appointment appointment : existingAppointments) {
            if (appointment.getStatus() == Appointment.Status.SCHEDULED ||
                    appointment.getStatus() == Appointment.Status.COMPLETED) {

                if (startTime.isBefore(appointment.getEndTime()) &&
                        appointment.getStartTime().isBefore(endTime)) {
                    return false;
                }
            }
        }

        return true;
    }
}