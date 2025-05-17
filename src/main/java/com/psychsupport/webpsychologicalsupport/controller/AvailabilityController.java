package com.psychsupport.webpsychologicalsupport.controller;

import com.psychsupport.webpsychologicalsupport.dto.AvailabilityDto;
import com.psychsupport.webpsychologicalsupport.model.Availability;
import com.psychsupport.webpsychologicalsupport.model.User;
import com.psychsupport.webpsychologicalsupport.service.AvailabilityService;
import com.psychsupport.webpsychologicalsupport.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
@Slf4j
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final UserService userService;

    @GetMapping("/psychologist/{psychologistId}")
    public ResponseEntity<List<Availability>> getPsychologistAvailability(@PathVariable Long psychologistId) {
        List<Availability> availabilities = availabilityService.getPsychologistAvailability(psychologistId);
        return ResponseEntity.ok(availabilities);
    }

    @PostMapping
    public ResponseEntity<?> saveAvailability(@RequestBody AvailabilityDto availabilityDto, Principal principal) {
        try {
            User psychologist = userService.getUserByUsername(principal.getName());

            for (Map<String, Object> daySchedule : availabilityDto.getWeeklySchedule()) {
                DayOfWeek dayOfWeek = DayOfWeek.of((Integer) daySchedule.get("dayOfWeek"));
                boolean available = (Boolean) daySchedule.get("available");

                availabilityService.clearDayAvailability(psychologist.getId(), dayOfWeek);

                if (available) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> timeSlots = (List<Map<String, String>>) daySchedule.get("timeSlots");

                    for (Map<String, String> slot : timeSlots) {
                        Availability availability = new Availability();
                        availability.setPsychologist(psychologist);
                        availability.setDayOfWeek(dayOfWeek);
                        availability.setStartTime(LocalTime.parse(slot.get("startTime")));
                        availability.setEndTime(LocalTime.parse(slot.get("endTime")));

                        availabilityService.saveAvailability(availability);
                    }
                }
            }


            return ResponseEntity.ok(Map.of("success", true, "message", "Availability saved successfully"));
        } catch (Exception e) {
            log.error("Error saving availability", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/default")
    public ResponseEntity<?> setDefaultAvailability(Principal principal) {
        try {
            User psychologist = userService.getUserByUsername(principal.getName());
            availabilityService.setDefaultAvailability(psychologist.getId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Default availability set successfully"));
        } catch (Exception e) {
            log.error("Error setting default availability", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{availabilityId}")
    public ResponseEntity<?> deleteAvailability(@PathVariable Long availabilityId, Principal principal) {
        try {
            availabilityService.deleteAvailability(availabilityId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Availability deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting availability", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}