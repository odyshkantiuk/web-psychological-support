package com.psychsupport.webpsychologicalsupport.service;

import com.psychsupport.webpsychologicalsupport.dto.UserRegistrationDto;
import com.psychsupport.webpsychologicalsupport.dto.UserUpdateDto;
import com.psychsupport.webpsychologicalsupport.exception.InvalidPasswordException;
import com.psychsupport.webpsychologicalsupport.exception.ResourceNotFoundException;
import com.psychsupport.webpsychologicalsupport.exception.UserAlreadyExistsException;
import com.psychsupport.webpsychologicalsupport.model.User;
import com.psychsupport.webpsychologicalsupport.repository.AppointmentRepository;
import com.psychsupport.webpsychologicalsupport.repository.AvailabilityRepository;
import com.psychsupport.webpsychologicalsupport.repository.JournalRepository;
import com.psychsupport.webpsychologicalsupport.repository.MessageRepository;
import com.psychsupport.webpsychologicalsupport.repository.UserRepository;
import com.psychsupport.webpsychologicalsupport.security.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JournalRepository journalRepository;
    private final AppointmentRepository appointmentRepository;
    private final MessageRepository messageRepository;
    private final AvailabilityRepository availabilityRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    public User registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        List<String> passwordErrors = passwordValidator.validate(registrationDto.getPassword());
        if (!passwordErrors.isEmpty()) {
            throw new InvalidPasswordException("Password does not meet security requirements", passwordErrors);
        }

        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setEmail(registrationDto.getEmail());
        user.setFullName(registrationDto.getFullName());
        user.setPhoneNumber(registrationDto.getPhoneNumber());
        user.setRole(registrationDto.getRole());
        user.setCreatedAt(LocalDateTime.now());

        if (registrationDto.getBio() != null && !registrationDto.getBio().trim().isEmpty()) {
            user.setBio(registrationDto.getBio());
        }

        user.setVerified(registrationDto.isVerified());
        if (registrationDto.isVerified()) {
            user.setVerifiedAt(LocalDateTime.now());
        }

        return userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getPsychologists() {
        return userRepository.findByRole(User.Role.PSYCHOLOGIST);
    }

    public List<User> getVerifiedPsychologists() {
        return userRepository.findByRoleAndIsVerifiedTrue(User.Role.PSYCHOLOGIST);
    }

    public List<User> getPendingVerificationPsychologists() {
        return userRepository.findByRoleAndIsVerifiedFalse(User.Role.PSYCHOLOGIST);
    }

    public List<User> getRecentRegistrations() {
        return userRepository.findTop5ByOrderByCreatedAtDesc();
    }

    public void verifyPsychologist(Long id, String verificationNotes) {
        User psychologist = getUserById(id);
        if (psychologist.getRole() != User.Role.PSYCHOLOGIST) {
            throw new IllegalArgumentException("User is not a psychologist");
        }
        
        if (psychologist.isVerified()) {
            throw new IllegalStateException("Psychologist is already verified");
        }
        
        psychologist.setVerified(true);
        psychologist.setVerifiedAt(LocalDateTime.now());
        psychologist.setVerificationNotes(verificationNotes);
        userRepository.save(psychologist);
    }

    public User updateUser(Long id, UserUpdateDto updateDto) {
        User user = getUserById(id);

        if (updateDto.getFullName() != null) {
            user.setFullName(updateDto.getFullName());
        }

        if (updateDto.getEmail() != null && !updateDto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(updateDto.getEmail())) {
                throw new UserAlreadyExistsException("Email already exists");
            }
            user.setEmail(updateDto.getEmail());
        }

        if (updateDto.getPhoneNumber() != null) {
            user.setPhoneNumber(updateDto.getPhoneNumber());
        }

        if (updateDto.getBio() != null) {
            user.setBio(updateDto.getBio());
        }

        if (updateDto.getProfilePicture() != null) {
            user.setProfilePicture(updateDto.getProfilePicture());
        }

        if (updateDto.getCv() != null) {
            user.setCv(updateDto.getCv());
        }

        if (updateDto.getPassword() != null && !updateDto.getPassword().isEmpty()) {
            List<String> passwordErrors = passwordValidator.validate(updateDto.getPassword());
            if (!passwordErrors.isEmpty()) {
                throw new InvalidPasswordException("Password does not meet security requirements", passwordErrors);
            }
            
            user.setPassword(passwordEncoder.encode(updateDto.getPassword()));
        }

        return userRepository.save(user);
    }

    public User updateUserAsAdmin(Long id, UserUpdateDto updateDto) {
        User user = getUserById(id);

        if (updateDto.getFullName() != null) {
            user.setFullName(updateDto.getFullName());
        }

        if (updateDto.getEmail() != null && !updateDto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(updateDto.getEmail())) {
                throw new UserAlreadyExistsException("Email already exists");
            }
            user.setEmail(updateDto.getEmail());
        }

        if (updateDto.getPhoneNumber() != null) {
            user.setPhoneNumber(updateDto.getPhoneNumber());
        }

        if (updateDto.getBio() != null) {
            user.setBio(updateDto.getBio());
        }

        if (updateDto.getProfilePicture() != null) {
            user.setProfilePicture(updateDto.getProfilePicture());
        }

        if (updateDto.getCv() != null) {
            user.setCv(updateDto.getCv());
        }

        if (updateDto.getRole() != null) {
            user.setRole(updateDto.getRole());
        }

        if (updateDto.getIsVerified() != null) {
            user.setVerified(updateDto.getIsVerified());
            if (updateDto.getIsVerified()) {
                user.setVerifiedAt(LocalDateTime.now());
            } else {
                user.setVerifiedAt(null);
            }
        }

        if (updateDto.getPassword() != null && !updateDto.getPassword().isEmpty()) {
            List<String> passwordErrors = passwordValidator.validate(updateDto.getPassword());
            if (!passwordErrors.isEmpty()) {
                throw new InvalidPasswordException("Password does not meet security requirements", passwordErrors);
            }
            
            user.setPassword(passwordEncoder.encode(updateDto.getPassword()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);

        journalRepository.clearSharedWithPsychologistByUserId(id);

        messageRepository.deleteAllByUserId(id);

        appointmentRepository.deleteAllByUserId(id);

        if (user.getRole() == User.Role.PSYCHOLOGIST) {
            availabilityRepository.deleteAllByPsychologistId(id);
        }

        userRepository.delete(user);
    }

    public void updateLastLogin(String username) {
        User user = getUserByUsername(username);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect.");
        }

        List<String> passwordErrors = passwordValidator.validate(newPassword);
        if (!passwordErrors.isEmpty()) {
            throw new InvalidPasswordException("Password does not meet security requirements", passwordErrors);
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public Long getUserIdByUsername(String username) {
        User user = userRepository.findByEmail(username)
                .orElse(null);
        return user != null ? user.getId() : null;
    }
}