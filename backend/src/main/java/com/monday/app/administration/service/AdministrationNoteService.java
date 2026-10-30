package com.monday.app.administration.service;

import com.monday.app.administration.dto.CreateNoteRequest;
import com.monday.app.administration.dto.NoteDto;
import com.monday.app.administration.dto.UpdateNoteRequest;
import com.monday.app.administration.entity.AdministrationNote;
import com.monday.app.administration.repository.AdministrationNoteRepository;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdministrationNoteService {

    private final AdministrationNoteRepository repository;

    public AdministrationNoteService(AdministrationNoteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<NoteDto> getNotes(String query) {
        return repository.search(query).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public NoteDto getById(UUID id) {
        AdministrationNote n = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration note not found: " + id));
        return toDto(n);
    }

    @Transactional
    public NoteDto create(CreateNoteRequest req) {
        AdministrationNote n = new AdministrationNote();
        n.setTitle(req.title() != null ? req.title().trim() : "");
        n.setBody(req.body() != null ? req.body() : "");

        AdministrationNote saved = repository.save(n);
        return toDto(saved);
    }

    @Transactional
    public NoteDto update(UUID id, UpdateNoteRequest req) {
        AdministrationNote n = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration note not found: " + id));

        if (req.title() != null) n.setTitle(req.title().trim());
        if (req.body() != null) n.setBody(req.body());

        AdministrationNote updated = repository.update(n);
        return toDto(updated);
    }

    @Transactional
    public void delete(UUID id) {
        repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration note not found: " + id));
        repository.deleteById(id);
    }

    private NoteDto toDto(AdministrationNote n) {
        return new NoteDto(
                n.getId(),
                n.getTitle(),
                n.getBody(),
                n.getCreatedAt(),
                n.getUpdatedAt()
        );
    }
}
