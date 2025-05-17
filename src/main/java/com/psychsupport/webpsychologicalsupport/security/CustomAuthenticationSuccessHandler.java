package com.psychsupport.webpsychologicalsupport.security;

import com.psychsupport.webpsychologicalsupport.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        UserService userService = applicationContext.getBean(UserService.class);

        loginAttemptService.loginSucceeded(authentication.getName());

        userService.updateLastLogin(authentication.getName());

        super.onAuthenticationSuccess(request, response, authentication);
    }
}