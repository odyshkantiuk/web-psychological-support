package com.psychsupport.webpsychologicalsupport.controller;

import com.psychsupport.webpsychologicalsupport.dto.JournalDto;
import com.psychsupport.webpsychologicalsupport.service.JournalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;

    @PostMapping
    public ResponseEntity<JournalDto> createJournal(@Valid @RequestBody JournalDto journalDto) {
        JournalDto createdJournal = journalService.createJournal(journalDto);
        return new ResponseEntity<>(createdJournal, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JournalDto> getJournalById(@PathVariable Long id) {
        JournalDto journal = journalService.getJournalById(id);
        return ResponseEntity.ok(journal);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<JournalDto>> getUserJournals(@PathVariable Long userId) {
        List<JournalDto> journals = journalService.getUserJournals(userId);
        return ResponseEntity.ok(journals);
    }

    @GetMapping("/shared-with/{psychologistId}")
    public ResponseEntity<List<JournalDto>> getJournalsSharedWithPsychologist(@PathVariable Long psychologistId) {
        List<JournalDto> sharedJournals = journalService.getJournalsSharedWithPsychologist(psychologistId);
        return ResponseEntity.ok(sharedJournals);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JournalDto> updateJournal(@PathVariable Long id, @Valid @RequestBody JournalDto journalDto) {
        JournalDto updatedJournal = journalService.updateJournal(id, journalDto);
        return ResponseEntity.ok(updatedJournal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJournal(@PathVariable Long id) {
        journalService.deleteJournal(id);
        return ResponseEntity.noContent().build();
    }
}