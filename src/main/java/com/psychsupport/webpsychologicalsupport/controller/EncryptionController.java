package com.psychsupport.webpsychologicalsupport.controller;

import com.psychsupport.webpsychologicalsupport.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/encryption")
@RequiredArgsConstructor
public class EncryptionController {

    private final EncryptionService encryptionService;

    @GetMapping("/salt")
    public ResponseEntity<Map<String, String>> getConversationSalt(
            @RequestParam Long user1,
            @RequestParam Long user2,
            Authentication authentication) {
        
        try {
            String currentUsername = authentication.getName();

            if (!encryptionService.isAuthorizedConversation(user1, user2)) {
                return ResponseEntity.badRequest().build();
            }

            String salt = encryptionService.getConversationSalt(user1, user2);
            
            Map<String, String> response = new HashMap<>();
            response.put("salt", salt);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/journal-salt")
    public ResponseEntity<Map<String, String>> getJournalSalt(
            @RequestParam Long userId,
            Authentication authentication) {
        
        try {
            String currentUsername = authentication.getName();
            
            String salt = encryptionService.getJournalSalt(userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("salt", salt);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/shared-journal-salt")
    public ResponseEntity<Map<String, String>> getSharedJournalSalt(
            @RequestParam Long userId,
            @RequestParam Long psychologistId,
            Authentication authentication) {
        
        try {
            String currentUsername = authentication.getName();
            
            String salt = encryptionService.getSharedJournalSalt(userId, psychologistId);
            
            Map<String, String> response = new HashMap<>();
            response.put("salt", salt);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getEncryptionHealth(Authentication authentication) {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "healthy");
            health.put("encryption", "active");

            if (authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                health.putAll(encryptionService.getCacheStats());
            }
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("encryption", "inactive");
            return ResponseEntity.internalServerError().body(error);
        }
    }
}