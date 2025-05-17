package com.psychsupport.webpsychologicalsupport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDto {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String password;
    private String bio;
    private String profilePicture;
    private String cv;
}
