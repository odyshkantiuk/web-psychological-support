package com.psychsupport.webpsychologicalsupport.controller.web;

import com.psychsupport.webpsychologicalsupport.dto.AppointmentDto;
import com.psychsupport.webpsychologicalsupport.dto.UserDto;
import com.psychsupport.webpsychologicalsupport.dto.UserRegistrationDto;
import com.psychsupport.webpsychologicalsupport.dto.UserUpdateDto;
import com.psychsupport.webpsychologicalsupport.model.User;
import com.psychsupport.webpsychologicalsupport.model.Appointment;
import com.psychsupport.webpsychologicalsupport.service.AppointmentService;
import com.psychsupport.webpsychologicalsupport.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final AppointmentService appointmentService;

    @ModelAttribute
    public void addAttributes(Model model, HttpServletRequest request) {
        String currentPath = request.getRequestURI();
        model.addAttribute("currentPath", currentPath);
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        List<UserDto> users = userService.getAllUsers().stream()
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
        model.addAttribute("totalUsers", users.size());

        long clientCount = users.stream()
                .filter(u -> u.getRole() == User.Role.CLIENT)
                .count();
        model.addAttribute("clientCount", clientCount);

        long psychologistCount = users.stream()
                .filter(u -> u.getRole() == User.Role.PSYCHOLOGIST)
                .count();
        model.addAttribute("psychologistCount", psychologistCount);

        List<UserDto> recentRegistrations = userService.getRecentRegistrations().stream()
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
        model.addAttribute("recentRegistrations", recentRegistrations);

        List<AppointmentDto> allAppointments = appointmentService.getAllAppointments();
        model.addAttribute("totalAppointments", allAppointments.size());
        
        List<AppointmentDto> recentAppointments = allAppointments.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("recentAppointments", recentAppointments);

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String userManagement(Model model, Principal principal,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(defaultValue = "created_desc") String sort) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        long totalUsers = userService.getAllUsers().size();
        int totalPages = (int) Math.ceil((double) totalUsers / size);

        List<UserDto> users = userService.getAllUsers().stream()
                .map(UserDto::fromUser)
                .sorted((a, b) -> {
                    switch (sort) {
                        case "created_asc":
                            return a.getCreatedAt().compareTo(b.getCreatedAt());
                        case "name_asc":
                            return a.getFullName().compareTo(b.getFullName());
                        case "name_desc":
                            return b.getFullName().compareTo(a.getFullName());
                        case "created_desc":
                        default:
                            return b.getCreatedAt().compareTo(a.getCreatedAt());
                    }
                })
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());

        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("currentSort", sort);

        return "admin/users";
    }

    @GetMapping("/users/view/{id}")
    public String viewUserDetails(@PathVariable Long id, Model model, Principal principal) {
        User admin = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(admin));

        User userToView = userService.getUserById(id);
        model.addAttribute("viewUser", UserDto.fromUser(userToView));

        return "admin/view-user";
    }

    @GetMapping("/users/add")
    public String showAddUserForm(Model model, Principal principal) {
        User admin = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(admin));

        model.addAttribute("newUser", new UserRegistrationDto());
        return "admin/add-user";
    }

    @PostMapping("/users/add")
    public String addUser(@Valid @ModelAttribute("newUser") UserRegistrationDto registrationDto,
                          BindingResult result, Model model, Principal principal) {
        if (result.hasErrors()) {
            User admin = userService.getUserByUsername(principal.getName());
            model.addAttribute("user", UserDto.fromUser(admin));
            return "admin/add-user";
        }

        try {
            userService.registerUser(registrationDto);
            return "redirect:/admin/users?success";
        } catch (Exception e) {
            User admin = userService.getUserByUsername(principal.getName());
            model.addAttribute("user", UserDto.fromUser(admin));
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/add-user";
        }
    }

    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model, Principal principal) {
        User admin = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(admin));

        User userToEdit = userService.getUserById(id);
        UserDto userDto = UserDto.fromUser(userToEdit);
        model.addAttribute("editUser", userDto);

        return "admin/edit-user";
    }

    @PostMapping("/users/edit/{id}")
    public String updateUser(@PathVariable Long id, @Valid @ModelAttribute("editUser") UserDto userDto, BindingResult result, Model model, Principal principal) {
        if (result.hasErrors()) {
            User admin = userService.getUserByUsername(principal.getName());
            model.addAttribute("user", UserDto.fromUser(admin));
            return "admin/edit-user";
        }

        try {
            UserUpdateDto updateDto = new UserUpdateDto();
            updateDto.setFullName(userDto.getFullName());
            updateDto.setEmail(userDto.getEmail());
            updateDto.setPhoneNumber(userDto.getPhoneNumber());
            updateDto.setBio(userDto.getBio());
            updateDto.setProfilePicture(userDto.getProfilePicture());

            userService.updateUser(id, updateDto);
            return "redirect:/admin/users/view/" + id + "?success";
        } catch (Exception e) {
            User admin = userService.getUserByUsername(principal.getName());
            model.addAttribute("user", UserDto.fromUser(admin));
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/edit-user";
        }
    }

    @GetMapping("/appointments")
    public String allAppointments(Model model, Principal principal,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(defaultValue = "created_desc") String sort) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        List<AppointmentDto> allAppointments = userService.getAllUsers().stream()
                .filter(u -> u.getRole() == User.Role.PSYCHOLOGIST)
                .flatMap(psychologist -> appointmentService.getPsychologistAppointments(psychologist.getId()).stream())
                .collect(Collectors.toList());

        allAppointments.sort((a, b) -> {
            switch (sort) {
                case "created_asc":
                    return a.getCreatedAt().compareTo(b.getCreatedAt());
                case "date_asc":
                    return a.getStartTime().compareTo(b.getStartTime());
                case "date_desc":
                    return b.getStartTime().compareTo(a.getStartTime());
                case "created_desc":
                default:
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
            }
        });

        int totalAppointments = allAppointments.size();
        int totalPages = (int) Math.ceil((double) totalAppointments / size);

        List<AppointmentDto> appointments = allAppointments.stream()
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());

        model.addAttribute("appointments", appointments);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalAppointments", totalAppointments);
        model.addAttribute("currentSort", sort);

        return "admin/appointments";
    }

    @GetMapping("/appointments/{id}")
    public String viewAppointment(@PathVariable Long id, Model model, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));
        
        AppointmentDto appointment = appointmentService.getAppointmentById(id);
        model.addAttribute("appointment", appointment);
        
        return "admin/view-appointment";
    }

    @GetMapping("/appointments/export")
    public void exportAppointments(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(defaultValue = "all") String dateRange,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "true") boolean includeIds,
            @RequestParam(defaultValue = "true") boolean includeNames,
            @RequestParam(defaultValue = "true") boolean includeDateTime,
            @RequestParam(defaultValue = "true") boolean includeStatus,
            @RequestParam(defaultValue = "false") boolean includeNotes,
            HttpServletResponse response) throws IOException {

        List<AppointmentDto> appointments = appointmentService.getAllAppointments();
        LocalDateTime now = LocalDateTime.now();
        
        switch (dateRange) {
            case "current_month":
                appointments = appointments.stream()
                    .filter(a -> a.getStartTime().getMonth() == now.getMonth() && 
                               a.getStartTime().getYear() == now.getYear())
                    .collect(Collectors.toList());
                break;
            case "last_month":
                LocalDateTime lastMonth = now.minusMonths(1);
                appointments = appointments.stream()
                    .filter(a -> a.getStartTime().getMonth() == lastMonth.getMonth() && 
                               a.getStartTime().getYear() == lastMonth.getYear())
                    .collect(Collectors.toList());
                break;
            case "last_3_months":
                LocalDateTime threeMonthsAgo = now.minusMonths(3);
                appointments = appointments.stream()
                    .filter(a -> a.getStartTime().isAfter(threeMonthsAgo))
                    .collect(Collectors.toList());
                break;
            case "last_6_months":
                LocalDateTime sixMonthsAgo = now.minusMonths(6);
                appointments = appointments.stream()
                    .filter(a -> a.getStartTime().isAfter(sixMonthsAgo))
                    .collect(Collectors.toList());
                break;
            case "last_year":
                LocalDateTime oneYearAgo = now.minusYears(1);
                appointments = appointments.stream()
                    .filter(a -> a.getStartTime().isAfter(oneYearAgo))
                    .collect(Collectors.toList());
                break;
            case "custom":
                if (startDate != null && endDate != null) {
                    LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
                    LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
                    appointments = appointments.stream()
                        .filter(a -> a.getStartTime().isAfter(start) && a.getStartTime().isBefore(end))
                        .collect(Collectors.toList());
                }
                break;
        }

        String filename = "appointments_" + LocalDate.now() + "." + format;
        response.setContentType(getContentType(format));
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        switch (format.toLowerCase()) {
            case "csv":
                exportToCsv(appointments, response.getWriter(), includeIds, includeNames, includeDateTime, includeStatus, includeNotes);
                break;
            case "excel":
                exportToExcel(appointments, response.getOutputStream(), includeIds, includeNames, includeDateTime, includeStatus, includeNotes);
                break;
            case "pdf":
                exportToPdf(appointments, response.getOutputStream(), includeIds, includeNames, includeDateTime, includeStatus, includeNotes);
                break;
            default:
                throw new IllegalArgumentException("Unsupported export format: " + format);
        }
    }

    private String getContentType(String format) {
        return switch (format.toLowerCase()) {
            case "csv" -> "text/csv";
            case "excel" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }

    private void exportToCsv(List<AppointmentDto> appointments, Writer writer, 
                           boolean includeIds, boolean includeNames, boolean includeDateTime, 
                           boolean includeStatus, boolean includeNotes) throws IOException {
        CSVWriter csvWriter = new CSVWriter(writer);

        List<String> header = new ArrayList<>();
        if (includeIds) header.add("ID");
        if (includeNames) {
            header.add("Psychologist");
            header.add("Client");
        }
        if (includeDateTime) {
            header.add("Date");
            header.add("Time");
        }
        if (includeStatus) header.add("Status");
        if (includeNotes) header.add("Notes");
        csvWriter.writeNext(header.toArray(new String[0]));

        for (AppointmentDto appointment : appointments) {
            List<String> row = new ArrayList<>();
            if (includeIds) row.add(appointment.getId().toString());
            if (includeNames) {
                row.add(appointment.getPsychologistName());
                row.add(appointment.getClientName());
            }
            if (includeDateTime) {
                row.add(appointment.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                row.add(appointment.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                       appointment.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            if (includeStatus) row.add(appointment.getStatus().toString());
            if (includeNotes) row.add(appointment.getNotes() != null ? appointment.getNotes() : "");
            csvWriter.writeNext(row.toArray(new String[0]));
        }

        csvWriter.close();
    }

    private void exportToExcel(List<AppointmentDto> appointments, OutputStream outputStream,
                             boolean includeIds, boolean includeNames, boolean includeDateTime,
                             boolean includeStatus, boolean includeNotes) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Appointments");

        Row headerRow = sheet.createRow(0);
        int columnIndex = 0;
        if (includeIds) headerRow.createCell(columnIndex++).setCellValue("ID");
        if (includeNames) {
            headerRow.createCell(columnIndex++).setCellValue("Psychologist");
            headerRow.createCell(columnIndex++).setCellValue("Client");
        }
        if (includeDateTime) {
            headerRow.createCell(columnIndex++).setCellValue("Date");
            headerRow.createCell(columnIndex++).setCellValue("Time");
        }
        if (includeStatus) headerRow.createCell(columnIndex++).setCellValue("Status");
        if (includeNotes) headerRow.createCell(columnIndex++).setCellValue("Notes");

        int rowIndex = 1;
        for (AppointmentDto appointment : appointments) {
            Row row = sheet.createRow(rowIndex++);
            columnIndex = 0;
            
            if (includeIds) row.createCell(columnIndex++).setCellValue(appointment.getId());
            if (includeNames) {
                row.createCell(columnIndex++).setCellValue(appointment.getPsychologistName());
                row.createCell(columnIndex++).setCellValue(appointment.getClientName());
            }
            if (includeDateTime) {
                row.createCell(columnIndex++).setCellValue(appointment.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                row.createCell(columnIndex++).setCellValue(appointment.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                                                         appointment.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            if (includeStatus) row.createCell(columnIndex++).setCellValue(appointment.getStatus().toString());
            if (includeNotes) row.createCell(columnIndex++).setCellValue(appointment.getNotes() != null ? appointment.getNotes() : "");
        }

        for (int i = 0; i < columnIndex; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(outputStream);
        workbook.close();
    }

    private void exportToPdf(List<AppointmentDto> appointments, OutputStream outputStream,
                           boolean includeIds, boolean includeNames, boolean includeDateTime,
                           boolean includeStatus, boolean includeNotes) throws IOException {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Appointments Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            int columns = 0;
            if (includeIds) columns++;
            if (includeNames) columns += 2;
            if (includeDateTime) columns += 2;
            if (includeStatus) columns++;
            if (includeNotes) columns++;

            PdfPTable table = new PdfPTable(columns);
            table.setWidthPercentage(100);

            if (includeIds) table.addCell("ID");
            if (includeNames) {
                table.addCell("Psychologist");
                table.addCell("Client");
            }
            if (includeDateTime) {
                table.addCell("Date");
                table.addCell("Time");
            }
            if (includeStatus) table.addCell("Status");
            if (includeNotes) table.addCell("Notes");

            for (AppointmentDto appointment : appointments) {
                if (includeIds) table.addCell(appointment.getId().toString());
                if (includeNames) {
                    table.addCell(appointment.getPsychologistName());
                    table.addCell(appointment.getClientName());
                }
                if (includeDateTime) {
                    table.addCell(appointment.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    table.addCell(appointment.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                                 appointment.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                }
                if (includeStatus) table.addCell(appointment.getStatus().toString());
                if (includeNotes) table.addCell(appointment.getNotes() != null ? appointment.getNotes() : "");
            }

            document.add(table);
            document.close();
        } catch (DocumentException e) {
            throw new IOException("Failed to generate PDF document", e);
        }
    }

    @GetMapping("/psychologists")
    public String managePsychologists(Model model, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", UserDto.fromUser(user));

        List<UserDto> allPsychologists = userService.getPsychologists().stream()
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
        model.addAttribute("psychologists", allPsychologists);

        List<UserDto> pendingPsychologists = userService.getPendingVerificationPsychologists().stream()
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
        model.addAttribute("pendingPsychologists", pendingPsychologists);

        List<UserDto> verifiedPsychologists = userService.getVerifiedPsychologists().stream()
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
        model.addAttribute("verifiedPsychologists", verifiedPsychologists);

        return "admin/psychologists";
    }

    @GetMapping("/profile")
    public String profilePage(Model model, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        UserDto userDto = UserDto.fromUser(user);
        model.addAttribute("user", userDto);
        return "admin/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute("user") UserDto userDto, Principal principal) {
        User admin = userService.getUserByUsername(principal.getName());
        
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setFullName(userDto.getFullName());
        updateDto.setEmail(userDto.getEmail());
        updateDto.setPhoneNumber(userDto.getPhoneNumber());
        updateDto.setBio(userDto.getBio());
        
        userService.updateUser(admin.getId(), updateDto);
        return "redirect:/admin/profile?success";
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
                return "redirect:/admin/profile";
            }

            User admin = userService.getUserByUsername(principal.getName());
            userService.changePassword(admin.getId(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/profile";
    }

    @PostMapping("/profile/photo")
    public String uploadProfilePhoto(@RequestParam("profilePhoto") MultipartFile file,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload.");
                return "redirect:/admin/profile";
            }

            if (!file.getContentType().startsWith("image/")) {
                redirectAttributes.addFlashAttribute("error", "Please upload an image file.");
                return "redirect:/admin/profile";
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                redirectAttributes.addFlashAttribute("error", "File size should not exceed 5MB.");
                return "redirect:/admin/profile";
            }

            User admin = userService.getUserByUsername(principal.getName());
            String fileName = "profile_" + admin.getId() + "_" + System.currentTimeMillis() + 
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
            userService.updateUser(admin.getId(), updateDto);

            redirectAttributes.addFlashAttribute("success", "Profile photo uploaded successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload profile photo: " + e.getMessage());
        }
        return "redirect:/admin/profile";
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/users?deleted";
    }

    @PostMapping("/psychologists/{id}/verify")
    public String verifyPsychologist(@PathVariable Long id, @RequestParam String verificationNotes) {
        userService.verifyPsychologist(id, verificationNotes);
        return "redirect:/admin/psychologists?verified";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id,
                                  @RequestParam String cancelReason,
                                  RedirectAttributes redirectAttributes) {
        try {
            appointmentService.updateAppointmentStatus(id, Appointment.Status.CANCELLED, cancelReason);
            redirectAttributes.addFlashAttribute("success", "Appointment cancelled successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to cancel appointment: " + e.getMessage());
        }
        return "redirect:/admin/appointments";
    }
}