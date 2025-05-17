package com.psychsupport.webpsychologicalsupport.controller;

import com.psychsupport.webpsychologicalsupport.dto.AppointmentDto;
import com.psychsupport.webpsychologicalsupport.dto.JournalDto;
import com.psychsupport.webpsychologicalsupport.model.User;
import com.psychsupport.webpsychologicalsupport.service.AppointmentService;
import com.psychsupport.webpsychologicalsupport.service.JournalService;
import com.psychsupport.webpsychologicalsupport.service.MessageService;
import com.psychsupport.webpsychologicalsupport.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diagnostic")
@RequiredArgsConstructor
public class DiagnosticController {

    private final UserService userService;
    private final AppointmentService appointmentService;
    private final JournalService journalService;
    private final MessageService messageService;

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userService.getUserByUsername(principal.getName());
            response.put("userId", user.getId());
            response.put("userRole", user.getRole());
            response.put("username", user.getUsername());

            List<AppointmentDto> allAppointments = appointmentService.getClientAppointments(user.getId());
            response.put("allAppointments", allAppointments);
            response.put("totalAppointments", allAppointments.size());

            List<AppointmentDto> upcomingAppointments = appointmentService.getClientUpcomingAppointments(user.getId());
            response.put("upcomingAppointments", upcomingAppointments);
            response.put("upcomingAppointmentsCount", upcomingAppointments.size());

            int completedSessions = (int) allAppointments.stream()
                    .filter(a -> a.getStatus() != null && a.getStatus().name().equals("COMPLETED"))
                    .count();
            response.put("completedSessions", completedSessions);

            List<JournalDto> journals = journalService.getUserJournals(user.getId());
            response.put("journalEntries", journals);
            response.put("journalEntriesCount", journals.size());

            long unreadMessages = messageService.countUnreadMessages(user.getId());
            response.put("unreadMessages", unreadMessages);
            
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(response);
    }
}
