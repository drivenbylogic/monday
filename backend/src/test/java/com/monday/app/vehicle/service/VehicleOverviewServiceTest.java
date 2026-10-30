package com.monday.app.vehicle.service;

import com.monday.app.vehicle.dto.VehicleOverviewDto;
import com.monday.app.vehicle.entity.*;
import com.monday.app.vehicle.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehicleOverviewServiceTest {

    private VehicleRepository vehicleRepository;
    private VehicleDocumentRepository documentRepository;
    private VehicleInsuranceRepository insuranceRepository;
    private VehicleServiceRecordRepository serviceRecordRepository;
    private VehicleMaintenanceScheduleRepository scheduleRepository;
    private VehicleFuelLogRepository fuelLogRepository;
    private VehicleTaskRepository taskRepository;
    private VehicleObligationRepository obligationRepository;
    private VehicleTimelineService timelineService;

    private VehicleOverviewService overviewService;
    private UUID vehicleId;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        vehicleRepository = mock(VehicleRepository.class);
        documentRepository = mock(VehicleDocumentRepository.class);
        insuranceRepository = mock(VehicleInsuranceRepository.class);
        serviceRecordRepository = mock(VehicleServiceRecordRepository.class);
        scheduleRepository = mock(VehicleMaintenanceScheduleRepository.class);
        fuelLogRepository = mock(VehicleFuelLogRepository.class);
        taskRepository = mock(VehicleTaskRepository.class);
        obligationRepository = mock(VehicleObligationRepository.class);
        timelineService = mock(VehicleTimelineService.class);

        overviewService = new VehicleOverviewService(
                vehicleRepository,
                documentRepository,
                insuranceRepository,
                serviceRecordRepository,
                scheduleRepository,
                fuelLogRepository,
                taskRepository,
                obligationRepository,
                timelineService
        );

        vehicleId = UUID.randomUUID();
        vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setName("Mercedes-Maybach S 680");
        vehicle.setCurrentKm(24000);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
    }

    @Test
    void getOverview_aggregatesActionableCardsAndNeedsAttentionItems() {
        LocalDate now = LocalDate.now();

        // Overdue maintenance schedule
        VehicleMaintenanceSchedule overdueSched = new VehicleMaintenanceSchedule(
                UUID.randomUUID(), vehicleId, "Transmission Fluid Flush", 12, 20000,
                now.minusMonths(14), 18000, now.minusDays(10), 22000, "OVERDUE", null, null
        );
        when(scheduleRepository.findByVehicleId(vehicleId)).thenReturn(List.of(overdueSched));

        // Expiring document
        VehicleDocument expDoc = new VehicleDocument(
                UUID.randomUUID(), vehicleId, "PUC", "Emission Certificate", "PUC-99", "Authority",
                now.plusDays(5).toString(), null, null, now.plusDays(5), null, true, "ACTIVE", null, null, null, null, null
        );
        when(documentRepository.findByVehicleId(vehicleId)).thenReturn(List.of(expDoc));

        // Insurance due in 15 days
        VehicleInsurance ins = new VehicleInsurance(
                UUID.randomUUID(), vehicleId, null, "Maybach Shield", "Allianz", "POL-99",
                "Full", new BigDecimal("200000"), new BigDecimal("4500"), now.plusDays(15), "ACTIVE", null, false, null, null
        );
        when(insuranceRepository.findByVehicleId(vehicleId)).thenReturn(List.of(ins));

        // Tasks & Obligations
        VehicleTask pendingTask = new VehicleTask(UUID.randomUUID(), vehicleId, "Check tire tread depth", "Tire check", false, "HIGH", "VEHICLES", null, null, null);
        when(taskRepository.findByVehicleId(vehicleId)).thenReturn(List.of(pendingTask));

        VehicleOverviewDto overview = overviewService.getOverview(vehicleId);

        assertNotNull(overview);
        assertNotNull(overview.vehicle());
        assertEquals("Mercedes-Maybach S 680", overview.vehicle().name());

        // Metric Card assertions
        assertEquals(1, overview.metrics().maintenanceOverdueCount());
        assertEquals(1, overview.metrics().documentsExpiringCount());
        assertTrue(overview.metrics().insuranceRenewalDue());
        assertEquals(1, overview.metrics().pendingTasksCount());

        // Needs Attention assertions
        assertFalse(overview.needsAttention().isEmpty());
        assertTrue(overview.needsAttention().stream().anyMatch(a -> a.type().equals("MAINTENANCE") && a.severity().equals("HIGH")));
        assertTrue(overview.needsAttention().stream().anyMatch(a -> a.type().equals("DOCUMENT")));
        assertTrue(overview.needsAttention().stream().anyMatch(a -> a.type().equals("INSURANCE")));
    }
}
