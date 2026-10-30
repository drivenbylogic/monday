package com.monday.app.administration.service;

import com.monday.app.administration.dto.CreateImportantDateRequest;
import com.monday.app.administration.dto.CreatePersonRequest;
import com.monday.app.administration.dto.ImportantDateDto;
import com.monday.app.administration.dto.PersonDto;
import com.monday.app.administration.entity.AdministrationImportantDate;
import com.monday.app.administration.entity.AdministrationPerson;
import com.monday.app.administration.repository.AdministrationImportantDateRepository;
import com.monday.app.administration.repository.AdministrationPersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdministrationPersonAndDateServiceTest {

    private AdministrationPersonRepository personRepository;
    private AdministrationImportantDateRepository dateRepository;
    private AdministrationPersonService personService;
    private AdministrationImportantDateService dateService;

    @BeforeEach
    void setUp() {
        personRepository = mock(AdministrationPersonRepository.class);
        dateRepository = mock(AdministrationImportantDateRepository.class);
        personService = new AdministrationPersonService(personRepository, dateRepository);
        dateService = new AdministrationImportantDateService(dateRepository);
    }

    @Test
    void testCreatePersonWithBirthdayAutoSyncsImportantDate() {
        LocalDate birthday = LocalDate.of(1992, 6, 14);
        CreatePersonRequest req = new CreatePersonRequest(
                "Eleanor Vance", "Spouse", birthday, "eleanor@example.com", "Notes"
        );

        when(personRepository.save(any(AdministrationPerson.class))).thenAnswer(inv -> {
            AdministrationPerson p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PersonDto person = personService.create(req);
        assertNotNull(person);
        assertEquals("Eleanor Vance", person.displayName());

        // Verify that dateRepository.save was called for the birthday occasion
        verify(dateRepository, times(1)).save(any(AdministrationImportantDate.class));
    }

    @Test
    void testGetUpcomingOccasionsSortedByDaysRemaining() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        // One occasion due in 5 days, one due in 20 days
        LocalDate today = LocalDate.now();
        AdministrationImportantDate d1 = new AdministrationImportantDate(
                id1, "Eleanor Birthday", null, today.plusDays(20), "BIRTHDAY", null,
                "ANNUAL", null, 7, null, Instant.now(), Instant.now()
        );
        AdministrationImportantDate d2 = new AdministrationImportantDate(
                id2, "Doctor Appointment Prep", null, today.plusDays(5), "PERSONAL_MILESTONE", null,
                "ONCE", null, 3, null, Instant.now(), Instant.now()
        );

        when(dateRepository.findAll(false)).thenReturn(List.of(d1, d2));

        List<ImportantDateDto> upcoming = dateService.getUpcomingOccasions(10);
        assertEquals(2, upcoming.size());
        // d2 (5 days) should be first, d1 (20 days) should be second
        assertEquals("Doctor Appointment Prep", upcoming.get(0).title());
        assertEquals("Eleanor Birthday", upcoming.get(1).title());
    }
}
