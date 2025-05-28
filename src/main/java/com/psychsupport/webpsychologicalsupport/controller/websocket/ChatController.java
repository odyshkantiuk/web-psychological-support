package com.psychsupport.webpsychologicalsupport.controller.websocket;

import com.psychsupport.webpsychologicalsupport.dto.MessageDto;
import com.psychsupport.webpsychologicalsupport.model.websocket.ChatMessage;
import com.psychsupport.webpsychologicalsupport.model.websocket.ChatNotification;
import com.psychsupport.webpsychologicalsupport.service.ChatService;
import com.psychsupport.webpsychologicalsupport.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());

        MessageDto messageDto = new MessageDto();
        messageDto.setSenderId(chatMessage.getSenderId());
        messageDto.setReceiverId(chatMessage.getReceiverId());
        messageDto.setContent(chatMessage.getContent());

        if (chatMessage.getEncrypted() != null && chatMessage.getEncrypted()) {
            messageDto.setEncrypted(true);
            messageDto.setEncryptionIv(chatMessage.getEncryptionIv());
            messageDto.setEncryptionHmac(chatMessage.getEncryptionHmac());
        }
        
        MessageDto savedMessage = messageService.sendMessage(messageDto);

        messagingTemplate.convertAndSendToUser(
                chatMessage.getReceiverId().toString(),
                "/queue/messages",
                chatMessage
        );

        ChatNotification notification = new ChatNotification(
                savedMessage.getId(),
                chatMessage.getSenderId(),
                chatMessage.getSenderName(),
                "New message"
        );

        messagingTemplate.convertAndSendToUser(
                chatMessage.getReceiverId().toString(),
                "/queue/notifications",
                notification
        );

        System.out.println("Message sent from " + chatMessage.getSenderName() +
                " to " + chatMessage.getReceiverName() +
                " (encrypted: " + (chatMessage.getEncrypted() != null ? chatMessage.getEncrypted() : false) + ")");
    }

    @MessageMapping("/chat.join")
    public void joinChat(@Payload ChatMessage chatMessage) {
        chatMessage.setType(ChatMessage.MessageType.JOIN);
        chatMessage.setTimestamp(LocalDateTime.now());

        chatService.userConnected(chatMessage.getSenderId());

        messagingTemplate.convertAndSend("/topic/public", chatMessage);

        System.out.println("User joined: " + chatMessage.getSenderName() + " (ID: " + chatMessage.getSenderId() + ")");
    }

    @MessageMapping("/chat.leave")
    public void leaveChat(@Payload ChatMessage chatMessage) {
        chatMessage.setType(ChatMessage.MessageType.LEAVE);
        chatMessage.setTimestamp(LocalDateTime.now());

        chatService.userDisconnected(chatMessage.getSenderId());

        messagingTemplate.convertAndSend("/topic/public", chatMessage);

        System.out.println("User left: " + chatMessage.getSenderName() + " (ID: " + chatMessage.getSenderId() + ")");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = (Long) headerAccessor.getHeader("userId");
        if (userId != null) {
            chatService.userDisconnected(userId);

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setType(ChatMessage.MessageType.LEAVE);
            chatMessage.setSenderId(userId);
            chatMessage.setTimestamp(LocalDateTime.now());
            
            messagingTemplate.convertAndSend("/topic/public", chatMessage);
            
            System.out.println("User disconnected: ID: " + userId);
        }
    }
}