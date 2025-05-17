package com.psychsupport.webpsychologicalsupport.controller;

import com.psychsupport.webpsychologicalsupport.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/chat")
public class ChatStatusController {
    private static final Logger logger = Logger.getLogger(ChatStatusController.class.getName());
    
    @Autowired
    private ChatService chatService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getChatStatus(@RequestParam Long userId) {
        try {
            boolean isConnected = chatService.isUserConnected(userId);
            Map<String, Boolean> response = new HashMap<>();
            response.put("connected", isConnected);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting chat status", e);
            Map<String, Boolean> errorResponse = new HashMap<>();
            errorResponse.put("connected", false);
            return ResponseEntity.ok(errorResponse);
        }
    }

    @GetMapping("/client-status")
    public ResponseEntity<Map<String, String>> getClientStatus(
            @RequestParam Long psychologistId, 
            @RequestParam Long clientId) {
        try {
            ChatService.StatusType status = chatService.getClientStatus(psychologistId, clientId);
            String statusText = getClientStatusText(status);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", statusText);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting client status", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "Offline");
            return ResponseEntity.ok(errorResponse);
        }
    }

    @GetMapping("/psychologist-status")
    public ResponseEntity<Map<String, String>> getPsychologistStatus(
            @RequestParam Long clientId, 
            @RequestParam Long psychologistId) {
        try {
            ChatService.StatusType status = chatService.getPsychologistStatus(clientId, psychologistId);
            String statusText = getPsychologistStatusText(status);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", statusText);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting psychologist status", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "Offline");
            return ResponseEntity.ok(errorResponse);
        }
    }

    private String getClientStatusText(ChatService.StatusType status) {
        switch (status) {
            case WAITING_FOR_CLIENT:
                return "Waiting for client";
            case CONNECTED:
                return "Connected";
            case CONNECTED_FOR_APPOINTMENT:
                return "Connected for appointment";
            case OFFLINE:
                return "Offline";
            default:
                return "Unknown";
        }
    }

    private String getPsychologistStatusText(ChatService.StatusType status) {
        switch (status) {
            case WAITING_FOR_PSYCHOLOGIST:
                return "Waiting for psychologist";
            case CONNECTED:
                return "Connected";
            case CONNECTED_FOR_APPOINTMENT:
                return "Connected for appointment";  
            case OFFLINE:
                return "Offline";
            case BUSY:
                return "Busy";
            default:
                return "Unknown";
        }
    }
}