package com.monday.app.vehicle.service;

import com.monday.app.vehicle.dto.OdometerReadingDto;
import com.monday.app.vehicle.dto.RecordOdometerRequest;
import com.monday.app.vehicle.entity.Vehicle;
import com.monday.app.vehicle.entity.VehicleOdometerReading;
import com.monday.app.vehicle.repository.VehicleOdometerReadingRepository;
import com.monday.app.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleOdometerServiceTest {

    private VehicleOdometerReadingRepository odometerRepository;
    private VehicleRepository vehicleRepository;
    private VehicleOdometerService odometerService;

    private UUID vehicleId;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        odometerRepository = mock(VehicleOdometerReadingRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        odometerService = new VehicleOdometerService(odometerRepository, vehicleRepository);

        vehicleId = UUID.randomUUID();
        vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setCurrentKm(10000);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
    }

    @Test
    void recordReading_validReading_updatesVehicleAndSaves() {
        VehicleOdometerReading latest = new VehicleOdometerReading(
                UUID.randomUUID(), vehicleId, LocalDate.now().minusDays(5), 10000, "MANUAL", false, null, null
        );
        when(odometerRepository.findLatestByVehicleId(vehicleId)).thenReturn(Optional.of(latest));
        when(odometerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecordOdometerRequest req = new RecordOdometerRequest(LocalDate.now(), 10500, "MANUAL", false, "Trip reading");
        OdometerReadingDto result = odometerService.recordReading(vehicleId, req);

        assertNotNull(result);
        assertEquals(10500, result.odometerKm());
        assertEquals(500, result.distanceDeltaFromPrevious());
        assertEquals(10500, vehicle.getCurrentKm());
        verify(vehicleRepository).update(vehicle);
    }

    @Test
    void recordReading_decreasingReadingWithoutCorrection_throwsIllegalArgumentException() {
        VehicleOdometerReading latest = new VehicleOdometerReading(
                UUID.randomUUID(), vehicleId, LocalDate.now().minusDays(5), 10000, "MANUAL", false, null, null
        );
        when(odometerRepository.findLatestByVehicleId(vehicleId)).thenReturn(Optional.of(latest));

        RecordOdometerRequest req = new RecordOdometerRequest(LocalDate.now(), 9500, "MANUAL", false, "Typo reading");

        assertThrows(IllegalArgumentException.class, () -> {
            odometerService.recordReading(vehicleId, req);
        });
        verify(vehicleRepository, never()).update(any());
    }

    @Test
    void recordReading_decreasingReadingWithCorrection_isAccepted() {
        VehicleOdometerReading latest = new VehicleOdometerReading(
                UUID.randomUUID(), vehicleId, LocalDate.now().minusDays(5), 10000, "MANUAL", false, null, null
        );
        when(odometerRepository.findLatestByVehicleId(vehicleId)).thenReturn(Optional.of(latest));
        when(odometerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecordOdometerRequest req = new RecordOdometerRequest(LocalDate.now(), 9500, "MANUAL", true, "Corrected mistaken entry");
        OdometerReadingDto result = odometerService.recordReading(vehicleId, req);

        assertNotNull(result);
        assertEquals(9500, result.odometerKm());
        assertTrue(result.isCorrection());
        assertEquals(9500, vehicle.getCurrentKm());
        verify(vehicleRepository).update(vehicle);
    }

    @Test
    void getReadings_computesDistanceDeltasBetweenEntries() {
        VehicleOdometerReading r1 = new VehicleOdometerReading(UUID.randomUUID(), vehicleId, LocalDate.now(), 12000, "MANUAL", false, null, null);
        VehicleOdometerReading r2 = new VehicleOdometerReading(UUID.randomUUID(), vehicleId, LocalDate.now().minusDays(10), 11500, "MANUAL", false, null, null);
        VehicleOdometerReading r3 = new VehicleOdometerReading(UUID.randomUUID(), vehicleId, LocalDate.now().minusDays(30), 10000, "MANUAL", false, null, null);

        when(odometerRepository.findByVehicleId(vehicleId)).thenReturn(List.of(r1, r2, r3));

        List<OdometerReadingDto> dtos = odometerService.getReadings(vehicleId);

        assertEquals(3, dtos.size());
        assertEquals(500, dtos.get(0).distanceDeltaFromPrevious()); // 12000 - 11500
        assertEquals(1500, dtos.get(1).distanceDeltaFromPrevious()); // 11500 - 10000
        assertNull(dtos.get(2).distanceDeltaFromPrevious()); // baseline
    }
}
