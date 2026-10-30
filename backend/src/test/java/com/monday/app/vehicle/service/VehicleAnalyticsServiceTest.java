package com.monday.app.vehicle.service;

import com.monday.app.vehicle.dto.VehicleAnalyticsDto;
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

class VehicleAnalyticsServiceTest {

    private VehicleRepository vehicleRepository;
    private VehicleOdometerReadingRepository odometerRepository;
    private VehicleFuelLogRepository fuelLogRepository;
    private VehicleServiceRecordRepository serviceRecordRepository;
    private VehicleInsuranceRepository insuranceRepository;
    private VehicleObligationRepository obligationRepository;
    private VehicleMaintenanceScheduleRepository scheduleRepository;

    private VehicleAnalyticsService analyticsService;
    private UUID vehicleId;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        vehicleRepository = mock(VehicleRepository.class);
        odometerRepository = mock(VehicleOdometerReadingRepository.class);
        fuelLogRepository = mock(VehicleFuelLogRepository.class);
        serviceRecordRepository = mock(VehicleServiceRecordRepository.class);
        insuranceRepository = mock(VehicleInsuranceRepository.class);
        obligationRepository = mock(VehicleObligationRepository.class);
        scheduleRepository = mock(VehicleMaintenanceScheduleRepository.class);

        analyticsService = new VehicleAnalyticsService(
                vehicleRepository,
                odometerRepository,
                fuelLogRepository,
                serviceRecordRepository,
                insuranceRepository,
                obligationRepository,
                scheduleRepository
        );

        vehicleId = UUID.randomUUID();
        vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setCurrentKm(25000);
        vehicle.setPurchaseAmount(new BigDecimal("80000.00"));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
    }

    @Test
    void getAnalytics_calculatesFuelEfficiencyAndCostBreakdown() {
        LocalDate now = LocalDate.now();

        // Odometer history: 25000 km now, 23000 km 2 weeks ago
        VehicleOdometerReading o1 = new VehicleOdometerReading(UUID.randomUUID(), vehicleId, now, 25000, "MANUAL", false, null, null);
        VehicleOdometerReading o2 = new VehicleOdometerReading(UUID.randomUUID(), vehicleId, now.minusDays(14), 23000, "MANUAL", false, null, null);
        when(odometerRepository.findByVehicleId(vehicleId)).thenReturn(List.of(o1, o2));

        // Fuel logs: 50 L for 100 USD at 25000 km with tripEfficiency 15.0 km/L
        VehicleFuelLog f1 = new VehicleFuelLog(UUID.randomUUID(), vehicleId, now, "Shell", "Shell", "Downtown", "Downtown", "V-Power", true, new BigDecimal("50.00"), new BigDecimal("100.00"), new BigDecimal("2.00"), 25000, new BigDecimal("15.00"), null);
        VehicleFuelLog f2 = new VehicleFuelLog(UUID.randomUUID(), vehicleId, now.minusDays(14), "Shell", "Shell", "Downtown", "Downtown", "V-Power", true, new BigDecimal("50.00"), new BigDecimal("95.00"), new BigDecimal("1.90"), 24250, new BigDecimal("14.50"), null);
        when(fuelLogRepository.findByVehicleId(vehicleId)).thenReturn(List.of(f1, f2));

        // Service records
        VehicleServiceRecord s1 = new VehicleServiceRecord(UUID.randomUUID(), vehicleId, now.minusMonths(1), 22000, "Oil Service", "Desc", new BigDecimal("350.00"), "Shop", null, null, null);
        when(serviceRecordRepository.findByVehicleId(vehicleId)).thenReturn(List.of(s1));

        // Insurance
        VehicleInsurance ins = new VehicleInsurance(UUID.randomUUID(), vehicleId, null, "Policy", "Allianz", "POL-1", "Scope", new BigDecimal("70000"), new BigDecimal("1200.00"), now.plusMonths(4), "ACTIVE", null, false, null, null);
        when(insuranceRepository.findByVehicleId(vehicleId)).thenReturn(List.of(ins));

        // Maintenance schedule: next at 30,000 km
        VehicleMaintenanceSchedule sched = new VehicleMaintenanceSchedule(UUID.randomUUID(), vehicleId, "Tire Rotation", 6, 10000, now.minusMonths(2), 20000, now.plusMonths(4), 30000, "OK", null, null);
        when(scheduleRepository.findByVehicleId(vehicleId)).thenReturn(List.of(sched));

        VehicleAnalyticsDto dto = analyticsService.getAnalytics(vehicleId, "ALL");

        assertNotNull(dto);
        // Usage
        assertEquals(25000, dto.usage().currentOdometerKm());
        assertEquals(5000, dto.usage().estimatedKmToNextMaintenance()); // 30000 - 25000

        // Fuel Efficiency
        assertTrue(dto.fuelEfficiency().hasSufficientData());
        assertEquals(15.0, dto.fuelEfficiency().latestEfficiencyKmPerL());
        assertEquals(new BigDecimal("195.00"), dto.fuelEfficiency().totalFuelCost());

        // Costs
        assertEquals(new BigDecimal("80000.00"), dto.costs().purchaseAmount());
        assertTrue(dto.costs().totalCumulativeOperatingCost().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(new BigDecimal("195.00"), dto.costs().categoryBreakdown().get("fuel"));
        assertEquals(new BigDecimal("350.00"), dto.costs().categoryBreakdown().get("servicing"));
        assertEquals(new BigDecimal("1200.00"), dto.costs().categoryBreakdown().get("insurance"));
    }
}
