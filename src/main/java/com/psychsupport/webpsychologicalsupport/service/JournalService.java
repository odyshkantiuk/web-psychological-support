package com.psychsupport.webpsychologicalsupport.service;

import com.psychsupport.webpsychologicalsupport.dto.JournalDto;
import com.psychsupport.webpsychologicalsupport.exception.ResourceNotFoundException;
import com.psychsupport.webpsychologicalsupport.model.Journal;
import com.psychsupport.webpsychologicalsupport.model.User;
import com.psychsupport.webpsychologicalsupport.repository.JournalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalRepository journalRepository;
    private final UserService userService;

    public JournalDto createJournal(JournalDto journalDto) {
        User user = userService.getUserById(journalDto.getUserId());

        Journal journal = new Journal();
        journal.setUser(user);
        journal.setTitle(journalDto.getTitle());
        journal.setContent(journalDto.getContent());
        journal.setMoodRating(journalDto.getMoodRating());

        if (journalDto.getSharedWithPsychologistId() != null) {
            User psychologist = userService.getUserById(journalDto.getSharedWithPsychologistId());
            if (psychologist.getRole() != User.Role.PSYCHOLOGIST) {
                throw new IllegalArgumentException("Can only share with a psychologist");
            }
            journal.setSharedWithPsychologist(psychologist);
        }

        if (journalDto.getEncrypted() != null && journalDto.getEncrypted()) {
            journal.setEncrypted(true);
            journal.setEncryptionIv(journalDto.getEncryptionIv());
            journal.setEncryptionHmac(journalDto.getEncryptionHmac());
        } else {
            journal.setEncrypted(false);
        }

        Journal savedJournal = journalRepository.save(journal);
        return convertToDto(savedJournal);
    }

    public JournalDto getJournalById(Long id) {
        Journal journal = journalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found with id: " + id));
        return convertToDto(journal);
    }

    public List<JournalDto> getUserJournals(Long userId) {
        User user = userService.getUserById(userId);
        return journalRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<JournalDto> getJournalsSharedWithPsychologist(Long psychologistId) {
        User psychologist = userService.getUserById(psychologistId);
        if (psychologist.getRole() != User.Role.PSYCHOLOGIST) {
            throw new IllegalArgumentException("User is not a psychologist");
        }

        return journalRepository.findBySharedWithPsychologist(psychologist).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public JournalDto updateJournal(Long id, JournalDto journalDto) {
        Journal journal = journalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found with id: " + id));

        if (journalDto.getTitle() != null) {
            journal.setTitle(journalDto.getTitle());
        }

        if (journalDto.getContent() != null) {
            journal.setContent(journalDto.getContent());
        }

        if (journalDto.getMoodRating() != null) {
            journal.setMoodRating(journalDto.getMoodRating());
        }

        if (journalDto.getEncrypted() != null && journalDto.getEncrypted()) {
            journal.setEncrypted(true);
            journal.setEncryptionIv(journalDto.getEncryptionIv());
            journal.setEncryptionHmac(journalDto.getEncryptionHmac());
        } else if (journalDto.getEncrypted() != null && !journalDto.getEncrypted()) {
            journal.setEncrypted(false);
            journal.setEncryptionIv(null);
            journal.setEncryptionHmac(null);
        }

        if (journalDto.getSharedWithPsychologistId() != null) {
            User psychologist = userService.getUserById(journalDto.getSharedWithPsychologistId());
            if (psychologist.getRole() != User.Role.PSYCHOLOGIST) {
                throw new IllegalArgumentException("Can only share with a psychologist");
            }
            journal.setSharedWithPsychologist(psychologist);
        } else if (journalDto.getSharedWithPsychologistId() == null && journalDto.isRemoveSharing()) {
            journal.setSharedWithPsychologist(null);
        }

        Journal updatedJournal = journalRepository.save(journal);
        return convertToDto(updatedJournal);
    }

    public void deleteJournal(Long id) {
        Journal journal = journalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found with id: " + id));
        journalRepository.delete(journal);
    }

    private JournalDto convertToDto(Journal journal) {
        JournalDto dto = new JournalDto();
        dto.setId(journal.getId());
        dto.setUserId(journal.getUser().getId());
        dto.setUserName(journal.getUser().getFullName());
        dto.setTitle(journal.getTitle());
        dto.setContent(journal.getContent());
        dto.setCreatedAt(journal.getCreatedAt());
        dto.setUpdatedAt(journal.getUpdatedAt());
        dto.setMoodRating(journal.getMoodRating());
        dto.setEncrypted(journal.getEncrypted() != null ? journal.getEncrypted() : false);
        dto.setEncryptionIv(journal.getEncryptionIv());
        dto.setEncryptionHmac(journal.getEncryptionHmac());

        if (journal.getSharedWithPsychologist() != null) {
            dto.setSharedWithPsychologistId(journal.getSharedWithPsychologist().getId());
            dto.setSharedWithPsychologistName(journal.getSharedWithPsychologist().getFullName());
        }

        return dto;
    }
}