package com.monday.app.administration.service;

import com.monday.app.administration.dto.*;
import com.monday.app.administration.entity.AdministrationGadget;
import com.monday.app.administration.entity.AdministrationMaintenance;
import com.monday.app.administration.repository.AdministrationGadgetRepository;
import com.monday.app.administration.repository.AdministrationMaintenanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdministrationGadgetAndMaintenanceServiceTest {

    private AdministrationGadgetRepository gadgetRepository;
    private AdministrationMaintenanceRepository maintenanceRepository;
    private AdministrationGadgetService gadgetService;
    private AdministrationMaintenanceService maintenanceService;

    @BeforeEach
    void setUp() {
        gadgetRepository = mock(AdministrationGadgetRepository.class);
        maintenanceRepository = mock(AdministrationMaintenanceRepository.class);
        gadgetService = new AdministrationGadgetService(gadgetRepository);
        maintenanceService = new AdministrationMaintenanceService(maintenanceRepository, gadgetRepository);
    }

    @Test
    void testCreateGadgetAndComputeWarranty() {
        LocalDate today = LocalDate.now();
        CreateGadgetRequest req = new CreateGadgetRequest(
                "LG OLED TV", "ELECTRONICS", "LG", "OLED65", "SN123",
                today.minusMonths(6), new BigDecimal("1500.00"),
                today.minusMonths(6), today.plusDays(20), "Costco", "Living room"
        );

        when(gadgetRepository.save(any(AdministrationGadget.class))).thenAnswer(inv -> {
            AdministrationGadget g = inv.getArgument(0);
            g.setId(UUID.randomUUID());
            return g;
        });

        GadgetDto dto = gadgetService.create(req);
        assertNotNull(dto);
        assertEquals("LG OLED TV", dto.name());
        assertEquals("EXPIRING_SOON", dto.warrantyStatus());
        assertEquals(20L, dto.warrantyDaysRemaining());
    }

    @Test
    void testCompleteRecurringMaintenanceSchedulesNextOccurrence() {
        UUID gadgetId = UUID.randomUUID();
        UUID maintId = UUID.randomUUID();

        AdministrationMaintenance m = new AdministrationMaintenance(
                maintId, gadgetId, "AC Deep Clean", null, "SERVICING",
                LocalDate.of(2026, 10, 1), "EVERY_6_MONTHS", null,
                new BigDecimal("80.00"), "UPCOMING", "CoolAir", null,
                Instant.now(), Instant.now()
        );

        when(maintenanceRepository.findById(maintId)).thenReturn(Optional.of(m));
        when(maintenanceRepository.update(any(AdministrationMaintenance.class))).thenAnswer(inv -> inv.getArgument(0));

        CompleteMaintenanceRequest compReq = new CompleteMaintenanceRequest(
                LocalDate.of(2026, 10, 5), new BigDecimal("85.00"), "CoolAir Inc", "Filter changed", true
        );

        MaintenanceDto completedDto = maintenanceService.complete(maintId, compReq);
        assertEquals("COMPLETED", completedDto.status());
        assertEquals(LocalDate.of(2026, 10, 5), completedDto.lastPerformedAt());

        // Verify that the next occurrence was scheduled (+6 months -> 2027-04-05)
        verify(maintenanceRepository, times(1)).save(argThat(next ->
                next.getDueDate().equals(LocalDate.of(2027, 4, 5)) &&
                next.getStatus().equals("UPCOMING")
        ));
    }
}
