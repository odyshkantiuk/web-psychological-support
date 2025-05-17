package com.psychsupport.webpsychologicalsupport.model.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatNotification {
    private Long id;
    private Long senderId;
    private String senderName;
    private String message;
}