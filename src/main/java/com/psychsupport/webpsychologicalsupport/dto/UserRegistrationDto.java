package com.psychsupport.webpsychologicalsupport.dto;

import com.psychsupport.webpsychologicalsupport.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDto {
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String phoneNumber;
    private User.Role role;
}
