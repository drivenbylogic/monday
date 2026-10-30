package com.monday.app.vehicle.service;

import com.monday.app.vehicle.dto.CreateServiceItemRequest;
import com.monday.app.vehicle.dto.CreateServiceRecordRequest;
import com.monday.app.vehicle.dto.ServiceRecordDto;
import com.monday.app.vehicle.entity.Vehicle;
import com.monday.app.vehicle.entity.VehicleServiceItem;
import com.monday.app.vehicle.entity.VehicleServiceRecord;
import com.monday.app.vehicle.repository.VehicleRepository;
import com.monday.app.vehicle.repository.VehicleServiceItemRepository;
import com.monday.app.vehicle.repository.VehicleServiceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleServiceLedgerServiceTest {

    private VehicleServiceRecordRepository serviceRecordRepository;
    private VehicleServiceItemRepository serviceItemRepository;
    private VehicleRepository vehicleRepository;
    private VehicleOdometerService odometerService;
    private VehicleMaintenanceScheduleService scheduleService;
    private NamedParameterJdbcTemplate jdbc;
    private VehicleServiceLedgerService ledgerService;

    private UUID vehicleId;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        serviceRecordRepository = mock(VehicleServiceRecordRepository.class);
        serviceItemRepository = mock(VehicleServiceItemRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        odometerService = mock(VehicleOdometerService.class);
        scheduleService = mock(VehicleMaintenanceScheduleService.class);
        jdbc = mock(NamedParameterJdbcTemplate.class);

        ledgerService = new VehicleServiceLedgerService(
                serviceRecordRepository,
                serviceItemRepository,
                vehicleRepository,
                odometerService,
                scheduleService,
                jdbc
        );

        vehicleId = UUID.randomUUID();
        vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setName("Porsche GT3 RS");
        vehicle.setCurrentKm(8000);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
    }

    @Test
    void createServiceRecord_withConsumables_calculatesCostAndSavesItems() {
        UUID scheduleId = UUID.randomUUID();
        CreateServiceItemRequest item1 = new CreateServiceItemRequest("Mobil 1 0W-40", "ENGINE_OIL", "Mobil 1", new BigDecimal("4.5"), "L", new BigDecimal("180"), 12, 10000, null);
        CreateServiceItemRequest item2 = new CreateServiceItemRequest("OEM Oil Filter", "FILTER", "Porsche", new BigDecimal("1"), "Piece", new BigDecimal("45"), 12, 10000, null);

        CreateServiceRecordRequest req = new CreateServiceRecordRequest(
                LocalDate.now(),
                8500,
                "Oil & Filter Service",
                "Annual engine oil replacement",
                null, // total cost calculated from parts + labor
                new BigDecimal("225.00"), // parts
                new BigDecimal("150.00"), // labor
                "Apex Motoworks",
                "Fresh oil and filter",
                true,
                false,
                true, // syncToFinance
                scheduleId,
                List.of(item1, item2)
        );

        when(serviceRecordRepository.save(any())).thenAnswer(inv -> {
            VehicleServiceRecord r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        when(serviceItemRepository.save(any())).thenAnswer(inv -> {
            VehicleServiceItem item = inv.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        ServiceRecordDto result = ledgerService.createServiceRecord(vehicleId, req);

        assertNotNull(result);
        assertEquals(new BigDecimal("375.00"), result.cost()); // 225 + 150
        assertEquals(2, result.items().size());
        assertNotNull(result.linkedTransactionId());

        // Verify odometer was recorded
        verify(odometerService).recordReading(eq(vehicleId), any());

        // Verify maintenance schedule was advanced
        verify(scheduleService).advanceScheduleAfterService(eq(scheduleId), eq(req.serviceDate()), eq(8500));

        // Verify finance transaction was recorded
        verify(jdbc).update(anyString(), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class));
    }
}
