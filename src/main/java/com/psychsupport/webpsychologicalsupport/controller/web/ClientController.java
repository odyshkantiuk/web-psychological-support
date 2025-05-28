package com.psychsupport.webpsychologicalsupport.controller.web;

import com.psychsupport.webpsychologicalsupport.dto.AppointmentDto;
import com.psychsupport.webpsychologicalsupport.dto.JournalDto;
import com.psychsupport.webpsychologicalsupport.dto.MessageDto;
import com.psychsupport.webpsychologicalsupport.dto.TimeSlotDto;
import com.psychsupport.webpsychologicalsupport.dto.UserDto;
import com.psychsupport.webpsychologicalsupport.dto.UserUpdateDto;
import com.psychsupport.webpsychologicalsupport.model.Appointment;
import com.psychsupport.webpsychologicalsupport.model.User;
import com.psychsupport.webpsychologicalsupport.service.AppointmentService;
import com.psychsupport.webpsychologicalsupport.service.JournalService;
import com.psychsupport.webpsychologicalsupport.service.MessageService;
import com.psychsupport.webpsychologicalsupport.service.UserService;
import com.psychsupport.webpsychologicalsupport.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientController {

    private final UserService userService;
    private final AppointmentService appointmentService;
    private final JournalService journalService;
    private final MessageService messageService;
    private final AvailabilityService availabilityService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        try {
            User user = userService.getUserByUsername(principal.getName());
            UserDto userDto = UserDto.fromUser(user);
            model.addAttribute("user", userDto);

            List<AppointmentDto> upcomingAppointments = appointmentService.getClientUpcomingAppointments(user.getId());
            model.addAttribute("appointments", upcomingAppointments);

            List<AppointmentDto> allAppointments = appointmentService.getClientAppointments(user.getId());
            int totalAppointments = allAppointments.size();
            int completedSessions = (int) allAppointments.stream()
                    .filter(a -> a.getStatus() != null && a.getStatus().name().equals("COMPLETED"))
                    .count();

            List<JournalDto> journals = journalService.getUserJournals(user.getId());
            int journalEntries = journals.size();

            long unreadMessages = messageService.countUnreadMessages(user.getId());

            model.addAttribute("totalAppointments", totalAppointments);
            model.addAttribute("completedSessions", completedSessions);
            model.addAttribute("journalEntries", journalEntries);
            model.addAttribute("unreadMessages", unreadMessages);
        } catch (Exception e) {
            System.err.println("ERROR in dashboard: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("totalAppointments", 0);
            model.addAttribute("completedSessions", 0);
            model.addAttribute("journalEntries", 0);
            model.addAttribute("unreadMessages", 0);
        }

        return "client/dashboard";
    }

    @GetMapping("/psychologists")
    public String viewPsychologists(Model model, Principal principal,
                                    @RequestParam(required = false, defaultValue = "false") boolean showOnlyAvailable) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        List<UserDto> psychologists = userService.getVerifiedPsychologists().stream()
                .map(UserDto::fromUser)
                .toList();

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        Map<Long, Boolean> availabilityMap = new HashMap<>();
        for (UserDto psychologist : psychologists) {
            boolean isCurrentlyAvailable = false;

            List<AppointmentDto> currentAppointments = appointmentService.getPsychologistAppointments(psychologist.getId())
                    .stream()
                    .filter(apt -> apt.getStatus() == Appointment.Status.SCHEDULED)
                    .filter(apt -> !apt.getStartTime().isAfter(now) && !apt.getEndTime().isBefore(now))
                    .toList();

            if (!currentAppointments.isEmpty()) {
                availabilityMap.put(psychologist.getId(), false);
                continue;
            }

            LocalDateTime nearFuture = now.plusHours(2);
            List<TimeSlotDto> nearFutureSlots = availabilityService.getAvailableTimeSlots(
                    psychologist.getId(), now.toLocalDate());

            isCurrentlyAvailable = nearFutureSlots.stream()
                    .anyMatch(slot -> slot.getStartTime().isAfter(now.plusMinutes(15)) && 
                                    slot.getStartTime().isBefore(nearFuture));

            if (!isCurrentlyAvailable) {
                List<TimeSlotDto> tomorrowSlots = availabilityService.getAvailableTimeSlots(
                        psychologist.getId(), now.toLocalDate().plusDays(1));
                isCurrentlyAvailable = !tomorrowSlots.isEmpty();
            }
            
            availabilityMap.put(psychologist.getId(), isCurrentlyAvailable);
        }
        model.addAttribute("availabilityMap", availabilityMap);

        if (showOnlyAvailable) {
            List<UserDto> availablePsychologists = psychologists.stream()
                    .filter(psychologist -> availabilityMap.get(psychologist.getId()))
                    .toList();

            model.addAttribute("psychologists", availablePsychologists);
            model.addAttribute("showingAvailable", true);
        } else {
            model.addAttribute("psychologists", psychologists);
            model.addAttribute("showingAvailable", false);
        }

        String psychologistId = model.asMap().get("selectedPsychologistId") != null
                ? model.asMap().get("selectedPsychologistId").toString()
                : null;
        model.addAttribute("selectedPsychologistId", psychologistId);

        return "client/psychologists";
    }

    @GetMapping("/appointments")
    public String appointmentsPage(Model model, Principal principal,
                                   @RequestParam(required = false) Long psychologistId,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String dateFrom,
                                   @RequestParam(required = false) String dateTo,
                                   @RequestParam(required = false) String view) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        List<AppointmentDto> allAppointments = appointmentService.getClientAppointments(user.getId());

        List<AppointmentDto> filteredAppointments = allAppointments;

        if (status != null && !status.isEmpty()) {
            filteredAppointments = filteredAppointments.stream()
                    .filter(apt -> apt.getStatus().name().equals(status))
                    .collect(Collectors.toList());
        }

        if (psychologistId != null) {
            filteredAppointments = filteredAppointments.stream()
                    .filter(apt -> apt.getPsychologistId().equals(psychologistId))
                    .collect(Collectors.toList());

            model.addAttribute("selectedPsychologistId", psychologistId);
        }

        if (dateFrom != null && !dateFrom.isEmpty()) {
            LocalDate fromDate = LocalDate.parse(dateFrom);
            filteredAppointments = filteredAppointments.stream()
                    .filter(apt -> !apt.getStartTime().toLocalDate().isBefore(fromDate))
                    .collect(Collectors.toList());

            model.addAttribute("selectedDateFrom", dateFrom);
        }

        if (dateTo != null && !dateTo.isEmpty()) {
            LocalDate toDate = LocalDate.parse(dateTo);
            filteredAppointments = filteredAppointments.stream()
                    .filter(apt -> !apt.getStartTime().toLocalDate().isAfter(toDate))
                    .collect(Collectors.toList());

            model.addAttribute("selectedDateTo", dateTo);
        }

        if (view != null) {
            LocalDate today = LocalDate.now();

            if ("upcoming".equals(view)) {
                filteredAppointments = filteredAppointments.stream()
                        .filter(apt -> !apt.getStartTime().toLocalDate().isBefore(today)
                                && apt.getStatus().name().equals("SCHEDULED"))
                        .collect(Collectors.toList());
                model.addAttribute("activeView", "upcoming");
            } else if ("past".equals(view)) {
                filteredAppointments = filteredAppointments.stream()
                        .filter(apt -> apt.getStartTime().toLocalDate().isBefore(today)
                                || !apt.getStatus().name().equals("SCHEDULED"))
                        .collect(Collectors.toList());
                model.addAttribute("activeView", "past");
            } else {
                model.addAttribute("activeView", "all");
            }
        } else {
            model.addAttribute("activeView", "all");
        }

        model.addAttribute("appointments", filteredAppointments);
        model.addAttribute("psychologists", userService.getVerifiedPsychologists());

        if (status != null && !status.isEmpty()) {
            model.addAttribute("selectedStatus", status);
        }

        if (psychologistId != null) {
            model.addAttribute("selectedPsychologistId", psychologistId);
            model.addAttribute("openAppointmentModal", true);
        }

        return "client/appointments-fixed";
    }

    @GetMapping("/appointments/{id}")
    public String viewAppointment(@PathVariable Long id, Model model, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        AppointmentDto appointment = appointmentService.getAppointmentById(id);
        if (!appointment.getClientId().equals(user.getId())) {
            throw new AccessDeniedException("You don't have permission to view this appointment");
        }

        model.addAttribute("appointment", appointment);
        return "client/view-appointment";
    }

    @GetMapping("/appointments/cancel")
    public String cancelAppointment(@RequestParam Long id,
                                    @RequestParam(required = false) Long psychologistId,
                                    RedirectAttributes redirectAttributes,
                                    Principal principal) {
        try {
            User user = userService.getUserByUsername(principal.getName());
            AppointmentDto appointment = appointmentService.getAppointmentById(id);

            if (!appointment.getClientId().equals(user.getId())) {
                throw new AccessDeniedException("You don't have permission to cancel this appointment");
            }

            appointmentService.updateAppointmentStatus(id, Appointment.Status.CANCELLED, "Cancelled by client");

            redirectAttributes.addFlashAttribute("success", "Appointment cancelled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to cancel appointment: " + e.getMessage());
        }

        return "redirect:/client/appointments";
    }

    @GetMapping("/journal")
    public String journalPage(Model model, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        List<JournalDto> journals = journalService.getUserJournals(user.getId());
        model.addAttribute("journals", journals);
        model.addAttribute("newJournal", new JournalDto());

        List<UserDto> psychologists = userService.getVerifiedPsychologists().stream()
                .map(UserDto::fromUser)
                .toList();
        model.addAttribute("psychologists", psychologists);

        return "client/journal";
    }

    @GetMapping("/journal/{id}")
    public String viewJournal(@PathVariable Long id, Model model, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        JournalDto journal = journalService.getJournalById(id);
        if (!journal.getUserId().equals(user.getId())) {
            return "redirect:/client/journal";
        }

        model.addAttribute("journal", journal);

        List<UserDto> psychologists = userService.getVerifiedPsychologists().stream()
                .map(UserDto::fromUser)
                .toList();
        model.addAttribute("psychologists", psychologists);

        return "client/view-journal";
    }

    @GetMapping("/messages")
    public String messagesPage(Model model, Principal principal, @RequestParam(required = false) Long psychologistId) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        Set<Long> psychologistIds = new HashSet<>();

        List<AppointmentDto> appointments = appointmentService.getClientAppointments(user.getId());
        psychologistIds.addAll(appointments.stream()
                .map(AppointmentDto::getPsychologistId)
                .collect(Collectors.toList()));

        List<MessageDto> messages = messageService.getAllMessagesByUserId(user.getId());
        psychologistIds.addAll(messages.stream()
                .map(msg -> msg.getSenderId().equals(user.getId()) ? msg.getReceiverId() : msg.getSenderId())
                .collect(Collectors.toList()));

        if (psychologistId != null) {
            User psychologist = userService.getUserById(psychologistId);
            if (psychologist.getRole() == User.Role.PSYCHOLOGIST && psychologist.isVerified()) {
                psychologistIds.add(psychologistId);
            }
        }

        List<UserDto> psychologists = psychologistIds.stream()
                .map(userService::getUserById)
                .filter(psychologist -> psychologist.getRole() == User.Role.PSYCHOLOGIST && psychologist.isVerified())
                .map(UserDto::fromUser)
                .collect(Collectors.toList());

        model.addAttribute("psychologists", psychologists);

        if (psychologistId != null) {
            Optional<UserDto> selectedPsychologist = psychologists.stream()
                    .filter(psych -> psych.getId().equals(psychologistId))
                    .findFirst();

            if (selectedPsychologist.isPresent()) {
                model.addAttribute("selectedPsychologist", selectedPsychologist.get());

                messageService.markConversationAsRead(user.getId(), psychologistId);
            } else {
                return "redirect:/client/messages";
            }
        }

        return "client/messages";
    }

    @GetMapping("/profile")
    public String profilePage(Model model, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        UserDto userDto = UserDto.fromUser(user);
        model.addAttribute("user", userDto);

        List<AppointmentDto> allAppointments = appointmentService.getClientAppointments(user.getId());
        int totalAppointments = allAppointments.size();
        int completedSessions = (int) allAppointments.stream()
                .filter(a -> a.getStatus() != null && a.getStatus().name().equals("COMPLETED"))
                .count();

        List<JournalDto> journals = journalService.getUserJournals(user.getId());
        int journalEntries = journals.size();

        model.addAttribute("totalAppointments", totalAppointments);
        model.addAttribute("completedSessions", completedSessions);
        model.addAttribute("journalEntries", journalEntries);

        return "client/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute("user") UserDto userDto,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            User client = userService.getUserByUsername(principal.getName());

            UserUpdateDto updateDto = new UserUpdateDto();
            updateDto.setFullName(userDto.getFullName());
            updateDto.setEmail(userDto.getEmail());
            updateDto.setPhoneNumber(userDto.getPhoneNumber());
            updateDto.setBio(userDto.getBio());

            userService.updateUser(client.getId(), updateDto);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/client/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "New password and confirm password do not match.");
                return "redirect:/client/profile";
            }

            User client = userService.getUserByUsername(principal.getName());
            userService.changePassword(client.getId(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/client/profile";
    }

    @PostMapping("/profile/photo")
    public String uploadProfilePhoto(@RequestParam("profilePhoto") MultipartFile file,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload.");
                return "redirect:/client/profile";
            }

            if (!file.getContentType().startsWith("image/")) {
                redirectAttributes.addFlashAttribute("error", "Please upload an image file.");
                return "redirect:/client/profile";
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                redirectAttributes.addFlashAttribute("error", "File size should not exceed 5MB.");
                return "redirect:/client/profile";
            }

            User client = userService.getUserByUsername(principal.getName());
            String fileName = "profile_" + client.getId() + "_" + System.currentTimeMillis() +
                    file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));

            String uploadDir = "uploads/profile-photos";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File destFile = new File(dir.getAbsolutePath() + File.separator + fileName);
            file.transferTo(destFile);

            UserUpdateDto updateDto = new UserUpdateDto();
            updateDto.setProfilePicture("/uploads/profile-photos/" + fileName);
            userService.updateUser(client.getId(), updateDto);

            redirectAttributes.addFlashAttribute("success", "Profile photo uploaded successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload profile photo: " + e.getMessage());
        }
        return "redirect:/client/profile";
    }

    @PostMapping("/appointments/create")
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
}