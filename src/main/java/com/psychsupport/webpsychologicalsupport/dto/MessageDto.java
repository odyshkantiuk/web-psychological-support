package com.psychsupport.webpsychologicalsupport.dto;

import com.psychsupport.webpsychologicalsupport.model.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDto {
    private Long id;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private String content;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;

    public static MessageDto fromMessage(Message message) {
        return MessageDto.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .receiverId(message.getReceiver().getId())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .senderName(message.getSender().getUsername())
                .receiverName(message.getReceiver().getUsername())
                .build();
    }
}