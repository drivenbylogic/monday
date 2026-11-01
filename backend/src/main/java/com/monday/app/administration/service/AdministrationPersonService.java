package com.monday.app.administration.service;

import com.monday.app.administration.dto.CreatePersonRequest;
import com.monday.app.administration.dto.PersonDto;
import com.monday.app.administration.dto.UpdatePersonRequest;
import com.monday.app.administration.entity.AdministrationImportantDate;
import com.monday.app.administration.entity.AdministrationPerson;
import com.monday.app.administration.repository.AdministrationImportantDateRepository;
import com.monday.app.administration.repository.AdministrationPersonRepository;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdministrationPersonService {

    private final AdministrationPersonRepository personRepository;
    private final AdministrationImportantDateRepository dateRepository;

    public AdministrationPersonService(AdministrationPersonRepository personRepository,
                                       AdministrationImportantDateRepository dateRepository) {
        this.personRepository = personRepository;
        this.dateRepository = dateRepository;
    }

    @Transactional(readOnly = true)
    public List<PersonDto> getPeople(String query, Boolean archived) {
        return personRepository.search(query, archived).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonDto getById(UUID id) {
        AdministrationPerson person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration person not found: " + id));
        return toDto(person);
    }

    @Transactional
    public PersonDto create(CreatePersonRequest req) {
        AdministrationPerson p = new AdministrationPerson();
        p.setDisplayName(req.displayName().trim());
        p.setRelationshipLabel(req.relationshipLabel() != null ? req.relationshipLabel().trim() : null);
        p.setBirthday(req.birthday());
        p.setContactInformation(req.contactInformation());
        p.setNotes(req.notes());

        AdministrationPerson saved = personRepository.save(p);

        // If birthday provided, create an automatic ImportantDate occasion for this person
        if (req.birthday() != null) {
            AdministrationImportantDate bday = new AdministrationImportantDate();
            bday.setTitle(saved.getDisplayName() + "'s Birthday");
            bday.setDescription("Annual birthday occasion for " + saved.getDisplayName());
            bday.setDate(req.birthday());
            bday.setDateType("BIRTHDAY");
            bday.setPersonId(saved.getId());
            bday.setRecurrenceType("ANNUAL");
            bday.setReminderLeadDays(3);
            dateRepository.save(bday);
        }

        return toDto(saved);
    }

    @Transactional
    public PersonDto update(UUID id, UpdatePersonRequest req) {
        AdministrationPerson p = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration person not found: " + id));

        if (req.displayName() != null && !req.displayName().isBlank()) p.setDisplayName(req.displayName().trim());
        if (req.relationshipLabel() != null) p.setRelationshipLabel(req.relationshipLabel().trim());
        if (req.birthday() != null) p.setBirthday(req.birthday());
        if (req.contactInformation() != null) p.setContactInformation(req.contactInformation());
        if (req.notes() != null) p.setNotes(req.notes());

        AdministrationPerson updated = personRepository.update(p);
        return toDto(updated);
    }

    @Transactional
    public void archive(UUID id) {
        personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration person not found: " + id));
        personRepository.archive(id);
    }

    @Transactional
    public void unarchive(UUID id) {
        personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration person not found: " + id));
        personRepository.unarchive(id);
    }

    @Transactional
    public void delete(UUID id) {
        personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration person not found: " + id));
        personRepository.deleteById(id);
    }

    private PersonDto toDto(AdministrationPerson p) {
        int occasions = personRepository.countOccasions(p.getId());
        return new PersonDto(
                p.getId(),
                p.getDisplayName(),
                p.getRelationshipLabel(),
                p.getBirthday(),
                p.getContactInformation(),
                p.getNotes(),
                occasions,
                p.getArchivedAt(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
