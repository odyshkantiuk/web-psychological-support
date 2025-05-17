package com.psychsupport.webpsychologicalsupport.controller;

import com.psychsupport.webpsychologicalsupport.dto.UserDto;
import com.psychsupport.webpsychologicalsupport.dto.UserRegistrationDto;
import com.psychsupport.webpsychologicalsupport.dto.UserUpdateDto;
import com.psychsupport.webpsychologicalsupport.model.User;
import com.psychsupport.webpsychologicalsupport.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        User user = userService.registerUser(registrationDto);
        return new ResponseEntity<>(UserDto.fromUser(user), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserDto> userDtos = users.stream()
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    @GetMapping("/psychologists")
    public ResponseEntity<List<UserDto>> getAllPsychologists() {
        List<User> psychologists = userService.getPsychologists();
        List<UserDto> psychologistDtos = psychologists.stream()
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
        return ResponseEntity.ok(psychologistDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(UserDto.fromUser(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDto userUpdateDto) {
        User updatedUser = userService.updateUser(id, userUpdateDto);
        return ResponseEntity.ok(UserDto.fromUser(updatedUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cv")
    public ResponseEntity<UserDto> uploadCv(@PathVariable Long id, @RequestParam("cv") MultipartFile cv) {
        if (cv.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Path uploadPath = Paths.get("uploads");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = cv.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;

            Path filePath = uploadPath.resolve(filename);
            Files.copy(cv.getInputStream(), filePath);

            UserUpdateDto updateDto = new UserUpdateDto();
            updateDto.setCv("/uploads/" + filename);
            User updatedUser = userService.updateUser(id, updateDto);
            
            return ResponseEntity.ok(UserDto.fromUser(updatedUser));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}