package com.psychsupport.webpsychologicalsupport.controller.web;

import com.psychsupport.webpsychologicalsupport.dto.AppointmentDto;
import com.psychsupport.webpsychologicalsupport.dto.JournalDto;
import com.psychsupport.webpsychologicalsupport.dto.MessageDto;
import com.psychsupport.webpsychologicalsupport.dto.UserDto;
import com.psychsupport.webpsychologicalsupport.dto.UserUpdateDto;
import com.psychsupport.webpsychologicalsupport.model.User;
import com.psychsupport.webpsychologicalsupport.model.Availability;
import com.psychsupport.webpsychologicalsupport.service.AppointmentService;
import com.psychsupport.webpsychologicalsupport.service.JournalService;
import com.psychsupport.webpsychologicalsupport.service.MessageService;
import com.psychsupport.webpsychologicalsupport.service.UserService;
import com.psychsupport.webpsychologicalsupport.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.File;
import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/psychologist")
@RequiredArgsConstructor
public class PsychologistController {

    private final UserService userService;
    private final AppointmentService appointmentService;
    private final JournalService journalService;
    private final MessageService messageService;
    private final AvailabilityService availabilityService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        UserDto userDto = UserDto.fromUser(user);
        model.addAttribute("user", userDto);

        List<AppointmentDto> upcomingAppointments = appointmentService.getPsychologistAppointments(user.getId());
        model.addAttribute("appointments", upcomingAppointments);

        List<JournalDto> sharedJournals = journalService.getJournalsSharedWithPsychologist(user.getId());
        model.addAttribute("journals", sharedJournals);

        return "psychologist/dashboard";
    }

    @GetMapping("/clients")
    public String viewClients(Model model, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        List<AppointmentDto> appointments = appointmentService.getPsychologistAppointments(user.getId());
        List<Long> clientIds = appointments.stream()
                .map(AppointmentDto::getClientId)
                .distinct()
                .toList();

        List<Map<String, Object>> clientInfos = clientIds.stream().map(clientId -> {
            User client = userService.getUserById(clientId);
            UserDto clientDto = UserDto.fromUser(client);
            List<AppointmentDto> clientAppointments = appointments.stream()
                    .filter(a -> a.getClientId().equals(clientId))
                    .sorted(Comparator.comparing(AppointmentDto::getStartTime))
                    .toList();

            AppointmentDto lastSession = clientAppointments.stream()
                    .filter(a -> a.getStartTime().isBefore(java.time.LocalDateTime.now()))
                    .filter(a -> a.getStatus().name().equals("COMPLETED"))
                    .max(Comparator.comparing(AppointmentDto::getStartTime))
                    .orElse(null);

            AppointmentDto nextAppointment = clientAppointments.stream()
                    .filter(a -> a.getStartTime().isAfter(java.time.LocalDateTime.now()))
                    .filter(a -> a.getStatus().name().equals("SCHEDULED"))
                    .min(Comparator.comparing(AppointmentDto::getStartTime))
                    .orElse(null);

            int totalSessions = clientAppointments.size();
            int completed = (int) clientAppointments.stream().filter(a -> a.getStatus().name().equals("COMPLETED")).count();
            int cancelled = (int) clientAppointments.stream().filter(a -> a.getStatus().name().equals("CANCELLED")).count();
            int noShow = (int) clientAppointments.stream().filter(a -> a.getStatus().name().equals("NO_SHOW")).count();

            Map<String, Object> info = new java.util.HashMap<>();
            info.put("client", clientDto);
            info.put("clientSince", client.getCreatedAt());
            info.put("lastSession", lastSession != null ? lastSession.getStartTime() : null);
            info.put("nextAppointment", nextAppointment != null ? nextAppointment.getStartTime() : null);
            info.put("totalSessions", totalSessions);
            info.put("completed", completed);
            info.put("cancelled", cancelled);
            info.put("noShow", noShow);
            return info;
        }).toList();

        model.addAttribute("clientInfos", clientInfos);
        return "psychologist/clients";
    }

    @GetMapping("/clients/{id}")
    public String viewClientProfile(@PathVariable Long id, Model model, Principal principal) {
        User psychologist = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(psychologist));

        User client = userService.getUserById(id);
        if (client == null || client.getRole() != User.Role.CLIENT) {
            return "redirect:/psychologist/clients";
        }

        List<AppointmentDto> appointments = appointmentService.getPsychologistAppointments(psychologist.getId());
        boolean hasAppointments = appointments.stream()
                .anyMatch(appointment -> appointment.getClientId().equals(id));
        
        if (!hasAppointments) {
            return "redirect:/psychologist/clients";
        }

        model.addAttribute("client", UserDto.fromUser(client));

        List<AppointmentDto> clientAppointments = appointments.stream()
                .filter(appointment -> appointment.getClientId().equals(id))
                .toList();
        model.addAttribute("appointments", clientAppointments);

        List<JournalDto> sharedJournals = journalService.getJournalsSharedWithPsychologist(psychologist.getId()).stream()
                .filter(journal -> journal.getUserId().equals(id))
                .toList();
        model.addAttribute("journals", sharedJournals);

        return "psychologist/client-profile";
    }

    @GetMapping("/appointments")
    public String appointmentsPage(Model model, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        List<AppointmentDto> appointments = appointmentService.getPsychologistAppointments(user.getId());
        model.addAttribute("appointments", appointments);

        return "psychologist/appointments";
    }

    @GetMapping("/appointment/{id}")
    public String appointmentDetailsPage(@PathVariable Long id, Model model, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        AppointmentDto appointment = appointmentService.getAppointmentById(id);
        if (!appointment.getPsychologistId().equals(user.getId())) {
            return "redirect:/psychologist/appointments";
        }

        model.addAttribute("appointment", appointment);

        User client = userService.getUserById(appointment.getClientId());
        model.addAttribute("client", UserDto.fromUser(client));

        List<AppointmentDto> allAppointments = appointmentService.getPsychologistAppointments(user.getId());
        List<AppointmentDto> clientAppointments = allAppointments.stream()
                .filter(a -> a.getClientId().equals(client.getId()))
                .sorted(Comparator.comparing(AppointmentDto::getStartTime))
                .toList();

        int totalSessions = clientAppointments.size();
        int completedSessions = (int) clientAppointments.stream()
                .filter(a -> a.getStatus().name().equals("COMPLETED"))
                .count();
        int cancelledSessions = (int) clientAppointments.stream()
                .filter(a -> a.getStatus().name().equals("CANCELLED"))
                .count();
        int noShowSessions = (int) clientAppointments.stream()
                .filter(a -> a.getStatus().name().equals("NO_SHOW"))
                .count();

        AppointmentDto lastSession = clientAppointments.stream()
                .filter(a -> a.getStatus().name().equals("COMPLETED"))
                .max(Comparator.comparing(AppointmentDto::getStartTime))
                .orElse(null);

        model.addAttribute("clientAppointments", clientAppointments);
        model.addAttribute("totalSessions", totalSessions);
        model.addAttribute("completedSessions", completedSessions);
        model.addAttribute("cancelledSessions", cancelledSessions);
        model.addAttribute("noShowSessions", noShowSessions);
        model.addAttribute("lastSession", lastSession);

        List<JournalDto> recentJournals = journalService.getJournalsSharedWithPsychologist(user.getId()).stream()
                .filter(journal -> journal.getUserId().equals(client.getId()))
                .sorted(Comparator.comparing(JournalDto::getCreatedAt).reversed())
                .limit(2)
                .toList();
        model.addAttribute("recentJournals", recentJournals);

        return "psychologist/appointment-details";
    }

    @GetMapping("/journals")
    public String journalsPage(Model model, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        List<JournalDto> sharedJournals = journalService.getJournalsSharedWithPsychologist(user.getId());
        model.addAttribute("journals", sharedJournals);

        List<UserDto> clientsWithJournals = sharedJournals.stream()
                .map(journal -> userService.getUserById(journal.getUserId()))
                .distinct()
                .map(UserDto::fromUser)
                .toList();
        model.addAttribute("clients", clientsWithJournals);

        Map<Integer, Long> moodDistribution = sharedJournals.stream()
                .filter(journal -> journal.getMoodRating() != null)
                .collect(Collectors.groupingBy(
                        JournalDto::getMoodRating,
                        Collectors.counting()
                ));
        model.addAttribute("moodDistribution", moodDistribution);

        long totalMoodRatings = sharedJournals.stream()
                .filter(journal -> journal.getMoodRating() != null)
                .count();
        model.addAttribute("totalMoodRatings", totalMoodRatings);

        Map<Long, List<JournalDto>> journalsByClient = sharedJournals.stream()
                .collect(Collectors.groupingBy(JournalDto::getUserId));

        List<Map<String, Object>> clientActivity = journalsByClient.entrySet().stream()
                .map(entry -> {
                    User client = userService.getUserById(entry.getKey());
                    List<JournalDto> clientJournals = entry.getValue();

                    double avgMood = clientJournals.stream()
                            .filter(j -> j.getMoodRating() != null)
                            .mapToInt(JournalDto::getMoodRating)
                            .average()
                            .orElse(0.0);

                    JournalDto latestJournal = clientJournals.stream()
                            .max(Comparator.comparing(JournalDto::getCreatedAt))
                            .orElse(null);

                    Map<String, Object> activity = new HashMap<>();
                    activity.put("client", UserDto.fromUser(client));
                    activity.put("totalJournals", clientJournals.size());
                    activity.put("sharedJournals", clientJournals.size());
                    activity.put("lastEntry", latestJournal != null ? latestJournal.getCreatedAt() : null);
                    activity.put("averageMood", avgMood);
                    return activity;
                })
                .collect(Collectors.toList());

        model.addAttribute("clientActivity", clientActivity);

        List<Map<String, Object>> moodTrends = sharedJournals.stream()
                .filter(journal -> journal.getMoodRating() != null)
                .sorted(Comparator.comparing(JournalDto::getCreatedAt).reversed())
                .limit(8)
                .map(journal -> {
                    Map<String, Object> trend = new HashMap<>();
                    trend.put("date", journal.getCreatedAt().toString());
                    trend.put("mood", journal.getMoodRating());
                    return trend;
                })
                .collect(Collectors.toList());

        model.addAttribute("moodTrends", moodTrends);

        return "psychologist/journals";
    }

    @GetMapping("/journal/{id}")
    public String viewJournal(@PathVariable Long id, Model model, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        JournalDto journal = journalService.getJournalById(id);
        if (journal.getSharedWithPsychologistId() == null || !journal.getSharedWithPsychologistId().equals(user.getId())) {
            return "redirect:/psychologist/journals";
        }

        model.addAttribute("journal", journal);

        User client = userService.getUserById(journal.getUserId());
        model.addAttribute("client", UserDto.fromUser(client));

        return "psychologist/view-journal";
    }

    @GetMapping("/messages")
    public String messagesPage(Model model, Principal principal, @RequestParam(required = false) Long clientId) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        Set<Long> clientIds = new HashSet<>();

        List<AppointmentDto> appointments = appointmentService.getPsychologistAppointments(user.getId());
        clientIds.addAll(appointments.stream()
                .map(AppointmentDto::getClientId)
                .collect(Collectors.toList()));

        List<MessageDto> messages = messageService.getAllMessagesByUserId(user.getId());
        clientIds.addAll(messages.stream()
                .map(msg -> msg.getSenderId().equals(user.getId()) ? msg.getReceiverId() : msg.getSenderId())
                .collect(Collectors.toList()));

        List<UserDto> clients = clientIds.stream()
                .map(userService::getUserById)
                .filter(client -> client.getRole() == User.Role.CLIENT)
                .map(UserDto::fromUser)
                .collect(Collectors.toList());

        model.addAttribute("clients", clients);

        UserDto selectedClient = null;
        if (clientId != null) {
            selectedClient = clients.stream()
                    .filter(client -> client.getId().equals(clientId))
                    .findFirst()
                    .orElse(null);
        }
        model.addAttribute("selectedClient", selectedClient);

        return "psychologist/messages";
    }

    @GetMapping("/profile")
    public String profilePage(Model model, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        List<Availability> availabilities = availabilityService.getPsychologistAvailability(user.getId());
        model.addAttribute("availabilities", availabilities);

        List<AppointmentDto> appointments = appointmentService.getPsychologistAppointments(user.getId());
        model.addAttribute("appointments", appointments);
        
        return "psychologist/profile";
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
                return "redirect:/psychologist/profile";
            }

            User psychologist = userService.getUserByUsername(principal.getName());
            userService.changePassword(psychologist.getId(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/psychologist/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute("user") UserDto userDto, 
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            User psychologist = userService.getUserByUsername(principal.getName());
            
            UserUpdateDto updateDto = new UserUpdateDto();
            updateDto.setFullName(userDto.getFullName());
            updateDto.setEmail(userDto.getEmail());
            updateDto.setPhoneNumber(userDto.getPhoneNumber());
            updateDto.setBio(userDto.getBio());
            
            userService.updateUser(psychologist.getId(), updateDto);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/psychologist/profile";
    }

    @PostMapping("/profile/photo")
    public String uploadProfilePhoto(@RequestParam("profilePhoto") MultipartFile file,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload.");
                return "redirect:/psychologist/profile";
            }

            if (!file.getContentType().startsWith("image/")) {
                redirectAttributes.addFlashAttribute("error", "Please upload an image file.");
                return "redirect:/psychologist/profile";
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                redirectAttributes.addFlashAttribute("error", "File size should not exceed 5MB.");
                return "redirect:/psychologist/profile";
            }

            User psychologist = userService.getUserByUsername(principal.getName());
            String fileName = "profile_" + psychologist.getId() + "_" + System.currentTimeMillis() + 
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
            userService.updateUser(psychologist.getId(), updateDto);

            redirectAttributes.addFlashAttribute("success", "Profile photo uploaded successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload profile photo: " + e.getMessage());
        }
        return "redirect:/psychologist/profile";
    }
}