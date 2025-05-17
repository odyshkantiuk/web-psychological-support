package com.psychsupport.webpsychologicalsupport.security;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MIN_UPPERCASE = 1;
    private static final int MIN_LOWERCASE = 1;
    private static final int MIN_DIGITS = 1;
    private static final int MIN_SPECIAL = 1;
    
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[^A-Za-z0-9]");
    
    public List<String> validate(String password) {
        List<String> validationErrors = new ArrayList<>();
        
        if (password == null || password.length() < MIN_LENGTH) {
            validationErrors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }
        
        if (countMatches(UPPERCASE_PATTERN, password) < MIN_UPPERCASE) {
            validationErrors.add("Password must contain at least " + MIN_UPPERCASE + " uppercase letter(s)");
        }
        
        if (countMatches(LOWERCASE_PATTERN, password) < MIN_LOWERCASE) {
            validationErrors.add("Password must contain at least " + MIN_LOWERCASE + " lowercase letter(s)");
        }
        
        if (countMatches(DIGIT_PATTERN, password) < MIN_DIGITS) {
            validationErrors.add("Password must contain at least " + MIN_DIGITS + " numeric character(s)");
        }
        
        if (countMatches(SPECIAL_PATTERN, password) < MIN_SPECIAL) {
            validationErrors.add("Password must contain at least " + MIN_SPECIAL + " special character(s)");
        }
        
        return validationErrors;
    }
    
    public boolean isValid(String password) {
        return validate(password).isEmpty();
    }
    
    private int countMatches(Pattern pattern, String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        var matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}