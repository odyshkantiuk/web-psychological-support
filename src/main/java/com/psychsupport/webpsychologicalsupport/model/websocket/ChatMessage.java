package com.psychsupport.webpsychologicalsupport.model.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private MessageType type;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private String content;
    private LocalDateTime timestamp;
    private Boolean encrypted;
    private String encryptionIv;
    private String encryptionHmac;

    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE
    }
}