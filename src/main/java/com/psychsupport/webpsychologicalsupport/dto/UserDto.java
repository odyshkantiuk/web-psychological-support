package com.psychsupport.webpsychologicalsupport.dto;

import com.psychsupport.webpsychologicalsupport.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private User.Role role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private String bio;
    private String profilePicture;
    private boolean isVerified;
    private LocalDateTime verifiedAt;
    private String verificationNotes;
    private String cv;

    public boolean hasProfilePicture() {
        return profilePicture != null && !profilePicture.isEmpty();
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        this.isVerified = verified;
    }

    public boolean getVerified() {
        return isVerified;
    }

    public static UserDto fromUser(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLogin(user.getLastLogin());
        dto.setBio(user.getBio());
        dto.setProfilePicture(user.getProfilePicture());
        dto.setVerified(user.isVerified());
        dto.setVerifiedAt(user.getVerifiedAt());
        dto.setVerificationNotes(user.getVerificationNotes());
        dto.setCv(user.getCv());
        return dto;
    }
}