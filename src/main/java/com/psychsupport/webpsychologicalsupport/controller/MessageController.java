package com.psychsupport.webpsychologicalsupport.controller;

import com.psychsupport.webpsychologicalsupport.dto.MessageDto;
import com.psychsupport.webpsychologicalsupport.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageDto> sendMessage(@Valid @RequestBody MessageDto messageDto) {
        MessageDto sentMessage = messageService.sendMessage(messageDto);
        return new ResponseEntity<>(sentMessage, HttpStatus.CREATED);
    }

    @GetMapping("/conversation")
    public ResponseEntity<List<MessageDto>> getConversation(
            @RequestParam Long user1Id,
            @RequestParam Long user2Id) {
        List<MessageDto> conversation = messageService.getConversation(user1Id, user2Id);
        return ResponseEntity.ok(conversation);
    }

    @GetMapping("/unread/{userId}")
    public ResponseEntity<List<MessageDto>> getUnreadMessages(@PathVariable Long userId) {
        List<MessageDto> unreadMessages = messageService.getUnreadMessages(userId);
        return ResponseEntity.ok(unreadMessages);
    }

    @GetMapping("/unread/count/{userId}")
    public ResponseEntity<Map<String, Long>> getUnreadMessageCount(@PathVariable Long userId) {
        long count = messageService.countUnreadMessages(userId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread/count/from")
    public ResponseEntity<Map<String, Long>> getUnreadMessageCountFromSender(
            @RequestParam Long receiverId,
            @RequestParam Long senderId) {
        long count = messageService.countUnreadMessagesFromSender(receiverId, senderId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/latest")
    public ResponseEntity<MessageDto> getLatestMessage(
            @RequestParam Long user1Id,
            @RequestParam Long user2Id) {
        MessageDto latestMessage = messageService.getLatestMessage(user1Id, user2Id);
        if (latestMessage == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(latestMessage);
    }

    @PatchMapping("/{messageId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long messageId) {
        messageService.markAsRead(messageId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/conversation/read")
    public ResponseEntity<Void> markConversationAsRead(
            @RequestParam Long userId,
            @RequestParam Long otherUserId) {
        messageService.markConversationAsRead(userId, otherUserId);
        return ResponseEntity.noContent().build();
    }
}