package com.monday.app.administration.controller;

import com.monday.app.administration.dto.CreatePersonRequest;
import com.monday.app.administration.dto.ImportantDateDto;
import com.monday.app.administration.dto.PersonDto;
import com.monday.app.administration.dto.UpdatePersonRequest;
import com.monday.app.administration.service.AdministrationImportantDateService;
import com.monday.app.administration.service.AdministrationPersonService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/administration/people", "/api/administration/people"})
public class AdministrationPersonController {

    private final AdministrationPersonService personService;
    private final AdministrationImportantDateService dateService;

    public AdministrationPersonController(AdministrationPersonService personService,
                                          AdministrationImportantDateService dateService) {
        this.personService = personService;
        this.dateService = dateService;
    }

    @GetMapping
    public ResponseEntity<List<PersonDto>> getPeople(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) Boolean includeArchived) {
        String searchQuery = (query != null && !query.isBlank()) ? query : search;
        Boolean isArchived = archived;
        if (isArchived == null && includeArchived != null && !includeArchived) {
            isArchived = false;
        }
        return ResponseEntity.ok(personService.getPeople(searchQuery, isArchived));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(personService.getById(id));
    }

    @GetMapping("/{id}/occasions")
    public ResponseEntity<List<ImportantDateDto>> getPersonOccasions(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        return ResponseEntity.ok(dateService.getByPersonId(id, includeArchived));
    }

    @PostMapping
    public ResponseEntity<PersonDto> create(@Valid @RequestBody CreatePersonRequest req) {
        PersonDto created = personService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePersonRequest req) {
        return ResponseEntity.ok(personService.update(id, req));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        personService.archive(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unarchive")
    public ResponseEntity<Void> unarchive(@PathVariable UUID id) {
        personService.unarchive(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        personService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
