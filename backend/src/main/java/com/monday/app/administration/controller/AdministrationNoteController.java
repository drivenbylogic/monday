package com.monday.app.administration.controller;

import com.monday.app.administration.dto.CreateNoteRequest;
import com.monday.app.administration.dto.NoteDto;
import com.monday.app.administration.dto.UpdateNoteRequest;
import com.monday.app.administration.service.AdministrationNoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/administration/notes", "/api/administration/notes"})
public class AdministrationNoteController {

    private final AdministrationNoteService service;

    public AdministrationNoteController(AdministrationNoteService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<NoteDto>> getNotes(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(service.getNotes(query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<NoteDto> create(@Valid @RequestBody CreateNoteRequest req) {
        NoteDto created = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNoteRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
