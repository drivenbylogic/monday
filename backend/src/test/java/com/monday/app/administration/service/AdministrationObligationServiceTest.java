package com.monday.app.administration.service;

import com.monday.app.administration.dto.CreateObligationRequest;
import com.monday.app.administration.dto.ObligationDto;
import com.monday.app.administration.dto.UpdateObligationRequest;
import com.monday.app.administration.entity.AdministrationObligation;
import com.monday.app.administration.repository.AdministrationObligationRepository;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdministrationObligationServiceTest {

    private AdministrationObligationRepository repository;
    private AdministrationObligationService service;

    @BeforeEach
    void setUp() {
        repository = mock(AdministrationObligationRepository.class);
        service = new AdministrationObligationService(repository);
    }

    @Test
    void testCreateObligation() {
        CreateObligationRequest req = new CreateObligationRequest(
                "Netflix Premium", "Streaming subscription", "Netflix Inc",
                "STREAMING", "SUBSCRIPTION", new BigDecimal("19.99"), "USD",
                "MONTHLY", null, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 15),
                null, null, null, true, "ACTIVE"
        );

        when(repository.save(any(AdministrationObligation.class))).thenAnswer(inv -> {
            AdministrationObligation arg = inv.getArgument(0);
            arg.setId(UUID.randomUUID());
            arg.setCreatedAt(Instant.now());
            arg.setUpdatedAt(Instant.now());
            return arg;
        });

        ObligationDto result = service.create(req);
        assertNotNull(result);
        assertEquals("Netflix Premium", result.name());
        assertEquals(new BigDecimal("19.99"), result.expectedAmount());
        assertEquals("MONTHLY", result.billingFrequency());
        assertTrue(result.autoRenew());
        verify(repository, times(1)).save(any(AdministrationObligation.class));
    }

    @Test
    void testAdvanceRecurrenceHonorsEndDate() {
        UUID id = UUID.randomUUID();
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate due = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 15);

        AdministrationObligation obl = new AdministrationObligation(
                id, "Gym Promo", null, null, "MEMBERSHIP", "MEMBERSHIP",
                new BigDecimal("50.00"), "USD", "MONTHLY", null,
                start, due, end, null, null, true, "ACTIVE", null, Instant.now(), Instant.now()
        );

        when(repository.findById(id)).thenReturn(Optional.of(obl));
        when(repository.update(any(AdministrationObligation.class))).thenAnswer(inv -> inv.getArgument(0));

        // Next due date would be June 1, which exceeds end date May 15
        ObligationDto advanced = service.advanceRecurrence(id);
        assertEquals("EXPIRED", advanced.status());
    }

    @Test
    void testGetByIdNotFoundThrowsException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(id));
    }

    @Test
    void testSearchAndFiltering() {
        when(repository.search("netflix", "STREAMING", "ACTIVE", false)).thenReturn(List.of(
                new AdministrationObligation(
                        UUID.randomUUID(), "Netflix Premium", null, "Netflix", "STREAMING",
                        "SUBSCRIPTION", new BigDecimal("19.99"), "USD", "MONTHLY", null,
                        LocalDate.now(), LocalDate.now().plusDays(5), null, null, null,
                        true, "ACTIVE", null, Instant.now(), Instant.now()
                )
        ));

        List<ObligationDto> list = service.getObligations("netflix", "STREAMING", "ACTIVE", false);
        assertEquals(1, list.size());
        assertEquals("Netflix Premium", list.get(0).name());
        assertTrue(list.get(0).isDueSoon());
    }
}
