package com.monday.app.vehicle.service;

import com.monday.app.vehicle.dto.CreateServiceItemRequest;
import com.monday.app.vehicle.dto.CreateServiceRecordRequest;
import com.monday.app.vehicle.dto.ServiceRecordDto;
import com.monday.app.vehicle.dto.UpdateServiceRecordRequest;
import com.monday.app.vehicle.entity.Vehicle;
import com.monday.app.vehicle.entity.VehicleOdometerReading;
import com.monday.app.vehicle.entity.VehicleServiceItem;
import com.monday.app.vehicle.entity.VehicleServiceRecord;
import com.monday.app.vehicle.repository.VehicleOdometerReadingRepository;
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
    private VehicleOdometerReadingRepository odometerRepository;
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
        odometerRepository = mock(VehicleOdometerReadingRepository.class);
        odometerService = mock(VehicleOdometerService.class);
        scheduleService = mock(VehicleMaintenanceScheduleService.class);
        jdbc = mock(NamedParameterJdbcTemplate.class);

        ledgerService = new VehicleServiceLedgerService(
                serviceRecordRepository,
                serviceItemRepository,
                vehicleRepository,
                odometerRepository,
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

        when(odometerRepository.findLatestByVehicleId(vehicleId)).thenReturn(Optional.of(
                new VehicleOdometerReading(UUID.randomUUID(), vehicleId, LocalDate.now(), 8000, "MANUAL", false, null, null)
        ));

        ServiceRecordDto result = ledgerService.createServiceRecord(vehicleId, req);

        assertNotNull(result);
        assertEquals(new BigDecimal("375.00"), result.cost()); // 225 + 150
        assertEquals(2, result.items().size());
        assertNotNull(result.linkedTransactionId());

        // Verify odometer was recorded since 8500 >= 8000
        verify(odometerService).recordReading(eq(vehicleId), any());

        // Verify maintenance schedule was advanced
        verify(scheduleService).advanceScheduleAfterService(eq(scheduleId), eq(req.serviceDate()), eq(8500));

        // Verify finance transaction was recorded
        verify(jdbc).update(anyString(), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class));
    }

    @Test
    void createServiceRecord_whenKmIsLessThanLatestOdometer_doesNotCallOdometerService() {
        // Vehicle currently at 8000 km, but logging historical service from 6500 km
        when(odometerRepository.findLatestByVehicleId(vehicleId)).thenReturn(Optional.of(
                new VehicleOdometerReading(UUID.randomUUID(), vehicleId, LocalDate.now().minusMonths(3), 8000, "MANUAL", false, null, null)
        ));

        CreateServiceRecordRequest req = new CreateServiceRecordRequest(
                LocalDate.now().minusMonths(3),
                6500, // < 8000 km
                "Brake Inspection",
                "Past brake fluid inspection",
                new BigDecimal("120.00"),
                null,
                null,
                "Brembo Workshop",
                "Pads OK",
                true,
                false,
                false,
                (UUID) null,
                null
        );

        when(serviceRecordRepository.save(any())).thenAnswer(inv -> {
            VehicleServiceRecord r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ServiceRecordDto result = ledgerService.createServiceRecord(vehicleId, req);

        assertNotNull(result);
        assertEquals(6500, result.kmAtService());
        // Verify odometerService was NOT called (preventing rollback-only exception in Spring transaction)
        verify(odometerService, never()).recordReading(any(), any());
    }

    @Test
    void createServiceRecord_withEmptyLinkedScheduleString_handlesGracefully() {
        // Dropdown "None / Standalone Service" gives empty string ""
        CreateServiceRecordRequest req = new CreateServiceRecordRequest(
                LocalDate.now(),
                8100,
                "Minor Check",
                null,
                new BigDecimal("50.00"),
                null,
                null,
                null,
                null,
                true,
                false,
                false,
                "", // empty schedule ID string
                null
        );

        when(serviceRecordRepository.save(any())).thenAnswer(inv -> {
            VehicleServiceRecord r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ServiceRecordDto result = ledgerService.createServiceRecord(vehicleId, req);

        assertNotNull(result);
        verify(scheduleService, never()).advanceScheduleAfterService(any(), any(), any());
    }

    @Test
    void updateServiceRecord_updatesCostAndItemsCleanly() {
        UUID recordId = UUID.randomUUID();
        VehicleServiceRecord existing = new VehicleServiceRecord(
                recordId,
                vehicleId,
                LocalDate.now().minusDays(5),
                8000,
                "Oil Service",
                "Original desc",
                new BigDecimal("200.00"),
                "Shop A",
                "Original note",
                null,
                null
        );

        when(serviceRecordRepository.findById(recordId)).thenReturn(Optional.of(existing));
        when(serviceRecordRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateServiceItemRequest newItem = new CreateServiceItemRequest("Brake Fluid", "FLUID", "Motul", new BigDecimal("1"), "Bottle", new BigDecimal("50.00"), null, null, null);
        UpdateServiceRecordRequest req = new UpdateServiceRecordRequest(
                null,
                8200,
                "Comprehensive Service",
                "Updated details",
                new BigDecimal("350.00"),
                null,
                null,
                "Shop B",
                "New note",
                true,
                false,
                false,
                "",
                List.of(newItem)
        );

        ServiceRecordDto result = ledgerService.updateServiceRecord(recordId, req);

        assertNotNull(result);
        assertEquals("Comprehensive Service", result.serviceType());
        assertEquals(new BigDecimal("350.00"), result.cost());
        assertEquals(8200, result.kmAtService());
        verify(serviceItemRepository).deleteByServiceRecordId(recordId);
        verify(serviceItemRepository).save(any());
        verify(serviceRecordRepository).update(existing);
    }
}
