package com.psychsupport.webpsychologicalsupport.service;

import com.psychsupport.webpsychologicalsupport.dto.MessageDto;
import com.psychsupport.webpsychologicalsupport.exception.ResourceNotFoundException;
import com.psychsupport.webpsychologicalsupport.model.Message;
import com.psychsupport.webpsychologicalsupport.model.User;
import com.psychsupport.webpsychologicalsupport.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserService userService;

    @Transactional
    public MessageDto sendMessage(MessageDto messageDto) {
        User sender = userService.getUserById(messageDto.getSenderId());
        User receiver = userService.getUserById(messageDto.getReceiverId());

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(messageDto.getContent());
        message.setSentAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);
        return convertToDto(savedMessage);
    }

    public List<MessageDto> getConversation(Long user1Id, Long user2Id) {
        User user1 = userService.getUserById(user1Id);
        User user2 = userService.getUserById(user2Id);

        return messageRepository.findConversation(user1, user2).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<MessageDto> getAllMessagesByUserId(Long userId) {
        return messageRepository.findBySenderIdOrReceiverId(userId, userId)
                .stream()
                .map(MessageDto::fromMessage)
                .collect(Collectors.toList());
    }

    public List<MessageDto> getUnreadMessages(Long userId) {
        User user = userService.getUserById(userId);

        return messageRepository.findByReceiverAndReadAtIsNull(user).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public long countUnreadMessages(Long userId) {
        User user = userService.getUserById(userId);
        return messageRepository.countByReceiverAndReadAtIsNull(user);
    }

    public long countUnreadMessagesFromSender(Long receiverId, Long senderId) {
        User receiver = userService.getUserById(receiverId);
        User sender = userService.getUserById(senderId);
        return messageRepository.countUnreadMessagesFromSender(sender, receiver);
    }

    @Transactional
    public void markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));

        message.setReadAt(LocalDateTime.now());
        messageRepository.save(message);
    }

    @Transactional
    public void markConversationAsRead(Long userId, Long otherUserId) {
        User user = userService.getUserById(userId);
        User otherUser = userService.getUserById(otherUserId);

        List<Message> unreadMessages = messageRepository.findByReceiverAndReadAtIsNull(user).stream()
                .filter(message -> message.getSender().getId().equals(otherUserId))
                .collect(Collectors.toList());

        for (Message message : unreadMessages) {
            message.setReadAt(LocalDateTime.now());
            messageRepository.save(message);
        }
    }

    public MessageDto getLatestMessage(Long user1Id, Long user2Id) {
        User user1 = userService.getUserById(user1Id);
        User user2 = userService.getUserById(user2Id);

        List<Message> latestMessages = messageRepository.findLatestMessage(user1, user2);
        if (latestMessages.isEmpty()) {
            return null;
        }

        return convertToDto(latestMessages.get(0));
    }

    @Transactional
    public long deleteConversation(Long user1Id, Long user2Id) {
        User user1 = userService.getUserById(user1Id);
        User user2 = userService.getUserById(user2Id);

        List<Message> messages = messageRepository.findConversation(user1, user2);
        long count = messages.size();

        messageRepository.deleteAll(messages);
        return count;
    }

    public long getConversationCount(Long user1Id, Long user2Id) {
        User user1 = userService.getUserById(user1Id);
        User user2 = userService.getUserById(user2Id);

        return messageRepository.findConversation(user1, user2).size();
    }

    public List<MessageDto> getUnreadMessagesSummary(Long userId) {
        User user = userService.getUserById(userId);

        return messageRepository.findByReceiverAndReadAtIsNull(user).stream()
                .collect(Collectors.groupingBy(Message::getSender))
                .entrySet().stream()
                .map(entry -> {
                    User sender = entry.getKey();
                    List<Message> messages = entry.getValue();

                    MessageDto dto = new MessageDto();
                    dto.setSenderId(sender.getId());
                    dto.setSenderName(sender.getFullName());
                    dto.setReceiverId(userId);
                    dto.setContent("You have " + messages.size() + " unread messages");

                    messages.stream()
                            .max((m1, m2) -> m1.getSentAt().compareTo(m2.getSentAt()))
                            .ifPresent(latestMessage -> dto.setSentAt(latestMessage.getSentAt()));

                    return dto;
                })
                .collect(Collectors.toList());
    }

    private MessageDto convertToDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getFullName());
        dto.setReceiverId(message.getReceiver().getId());
        dto.setReceiverName(message.getReceiver().getFullName());
        dto.setContent(message.getContent());
        dto.setSentAt(message.getSentAt());
        dto.setReadAt(message.getReadAt());
        return dto;
    }
}