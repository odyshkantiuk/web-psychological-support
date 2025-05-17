package com.psychsupport.webpsychologicalsupport.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        
        String username = request.getParameter("username");
        loginAttemptService.loginFailed(username);
        
        if (loginAttemptService.isBlocked(username)) {
            exception = new LockedException("Your account has been locked due to " + loginAttemptService.getMaxAttempts() + 
                                          " failed attempts. It will be unlocked after " + 
                                          loginAttemptService.getBlockDurationMinutes() + " minutes.");
        } else {
            int remainingAttempts = loginAttemptService.getRemainingAttempts(username);
            String baseMessage = "Invalid username or password";
            
            if (remainingAttempts > 0 && remainingAttempts < loginAttemptService.getMaxAttempts()) {
                baseMessage += ". Remaining attempts: " + remainingAttempts;
            }
            
            if (exception.getMessage() == null || exception.getMessage().isEmpty() || 
                exception.getMessage().contains("Bad credentials")) {
                exception = new AuthenticationException(baseMessage) {};
            }
        }

        String errorMessage = exception.getMessage();

        String encodedErrorMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8.name());
        setDefaultFailureUrl("/login?error=" + encodedErrorMessage);
        
        super.onAuthenticationFailure(request, response, exception);
    }
}