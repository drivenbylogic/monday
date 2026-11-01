package com.monday.app.vehicle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.shared.exception.GlobalExceptionHandler;
import com.monday.app.vehicle.dto.*;
import com.monday.app.vehicle.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VehicleControllerTest {

    private MockMvc mockMvc;
    private VehicleService vehicleService;
    private VehicleOverviewService overviewService;
    private VehicleDocumentService documentService;
    private VehicleOdometerService odometerService;
    private VehicleFuelService fuelService;
    private VehicleServiceLedgerService serviceLedgerService;
    private VehicleMaintenanceScheduleService scheduleService;
    private VehicleAnalyticsService analyticsService;
    private VehicleTimelineService timelineService;
    private VehicleTaskService taskService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        vehicleService = mock(VehicleService.class);
        overviewService = mock(VehicleOverviewService.class);
        documentService = mock(VehicleDocumentService.class);
        odometerService = mock(VehicleOdometerService.class);
        fuelService = mock(VehicleFuelService.class);
        serviceLedgerService = mock(VehicleServiceLedgerService.class);
        scheduleService = mock(VehicleMaintenanceScheduleService.class);
        analyticsService = mock(VehicleAnalyticsService.class);
        timelineService = mock(VehicleTimelineService.class);
        taskService = mock(VehicleTaskService.class);

        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new VehicleController(
                vehicleService,
                overviewService,
                documentService,
                odometerService,
                fuelService,
                serviceLedgerService,
                scheduleService,
                analyticsService,
                timelineService,
                taskService
        ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void setPrimaryVehicle_togglesPrimaryVehicle() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        Map<String, Object> resp = Map.of(
                "vehicleId", vehicleId.toString(),
                "name", "XPulse 200 4V Rally Edition",
                "isPrimary", true,
                "previousPrimaryId", "veh-bmw-g310",
                "updatedAt", "2026-10-14T08:30:00Z"
        );

        when(vehicleService.setPrimaryVehicle(eq(vehicleId), eq(true))).thenReturn(resp);

        mockMvc.perform(put("/api/v1/vehicles/" + vehicleId + "/primary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isPrimary\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(vehicleId.toString()))
                .andExpect(jsonPath("$.name").value("XPulse 200 4V Rally Edition"))
                .andExpect(jsonPath("$.isPrimary").value(true))
                .andExpect(jsonPath("$.previousPrimaryId").value("veh-bmw-g310"));

        verify(vehicleService).setPrimaryVehicle(vehicleId, true);
    }

    @Test
    void logFuel_withLegacyOrNewEndpoint_delegatesToFuelService() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        when(vehicleService.logFuel(eq(vehicleId), any())).thenReturn(Map.of("id", "fuel-1", "odometerKm", 14320));

        String payload = """
                {
                    "vehicleId": "%s",
                    "odometerKm": 14320,
                    "volumeLiters": 11.2,
                    "totalCost": 1280.00,
                    "fuelBrand": "Shell",
                    "fuelGrade": "V-POWER_95",
                    "stationLocation": "Shell Autocity Station, Outer Ring Road",
                    "fullTank": true
                }
                """.formatted(vehicleId);

        mockMvc.perform(post("/api/v1/vehicles/" + vehicleId + "/fuel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    void getOverview_returnsCompleteGarageDashboard() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        VehicleSummaryDto summary = new VehicleSummaryDto(
                vehicleId, "Porsche 911 GT3 RS", "Weissach Package", "Coupe", "Car",
                "S-GO-9923", true, 8450, LocalDate.now().minusYears(1), new BigDecimal("250000"),
                "ACTIVE", null, "WP0ZZZ99ZTS12345", "Euro 6", "Owner", "Stuttgart DMV"
        );
        MetricCardDto metrics = new MetricCardDto(1, 0, 1, 0, false, 45, new BigDecimal("450.00"), new BigDecimal("1200.00"), 8450, 8.5, 2, 1);
        VehicleOverviewDto overview = new VehicleOverviewDto(summary, metrics, List.of(), List.of(), List.of());

        when(overviewService.getOverview(vehicleId)).thenReturn(overview);

        mockMvc.perform(get("/api/v1/vehicles/" + vehicleId + "/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicle.name").value("Porsche 911 GT3 RS"))
                .andExpect(jsonPath("$.metrics.currentOdometerKm").value(8450))
                .andExpect(jsonPath("$.metrics.fuelSpendingThisMonth").value(450.00));
    }

    @Test
    void getDocuments_returnsEnrichedDocuments() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        VehicleDocumentDto doc = new VehicleDocumentDto(
                UUID.randomUUID(), vehicleId, "INSURANCE", "Hiscox Policy", "POL-1", "Hiscox",
                "2027-04-15", LocalDate.now(), LocalDate.now(), LocalDate.now().plusMonths(6),
                "Direct", true, "ACTIVE", null, "Notes", null, 180L, false, false, false, "VALID",
                UUID.randomUUID(), "Hiscox Policy", "Hiscox", "POL-1", "Comprehensive",
                new BigDecimal("200000"), new BigDecimal("3500"), LocalDate.now().plusMonths(6), false, null, null
        );

        when(documentService.getDocuments(eq(vehicleId), any(), any(), any())).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/v1/vehicles/" + vehicleId + "/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents[0].docType").value("INSURANCE"))
                .andExpect(jsonPath("$.documents[0].policyNumber").value("POL-1"))
                .andExpect(jsonPath("$.allValid").value(true));
    }

    @Test
    void recordOdometerReading_returnsSavedReading() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        OdometerReadingDto dto = new OdometerReadingDto(
                UUID.randomUUID(), vehicleId, LocalDate.now(), 9200, "MANUAL", false, "Highway run", 750, null
        );

        when(odometerService.recordReading(eq(vehicleId), any())).thenReturn(dto);

        String payload = """
                {
                    "readingDate": "2026-10-28",
                    "odometerKm": 9200,
                    "source": "MANUAL",
                    "notes": "Highway run"
                }
                """;

        mockMvc.perform(post("/api/v1/vehicles/" + vehicleId + "/odometer-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.odometerKm").value(9200))
                .andExpect(jsonPath("$.distanceDeltaFromPrevious").value(750));
    }

    @Test
    void getTimeline_returnsUnifiedTimelineList() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        VehicleTimelineItemDto item = new VehicleTimelineItemDto(
                "srv-1", "SERVICE", "Track Brake Fluid Flush", "Flushed with Castrol SRF",
                LocalDate.now().minusDays(3), 8200, new BigDecimal("450.00"), "rec-1", "Service Visit", "build"
        );

        when(timelineService.getTimeline(eq(vehicleId), eq(50))).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/vehicles/" + vehicleId + "/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("SERVICE"))
                .andExpect(jsonPath("$[0].title").value("Track Brake Fluid Flush"));
    }

    @Test
    void createServiceRecord_withAliasesAndEmptySchedule_succeeds() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        ServiceRecordDto recordDto = new ServiceRecordDto(
                recordId, vehicleId, LocalDate.of(2026, 10, 20), 8500, "Oil & Filter Service",
                "Periodic maintenance", new BigDecimal("350.00"), new BigDecimal("200.00"),
                new BigDecimal("150.00"), "Porsche Centre", "Clean inspection", true, false,
                "tx-123", List.of(), null, null
        );

        when(serviceLedgerService.createServiceRecord(eq(vehicleId), any())).thenReturn(recordDto);

        // Payload with aliases ("date", "odometerKm", "amount", "serviceProvider", "linkedScheduleId": "")
        String payload = """
                {
                    "date": "2026-10-20",
                    "odometerKm": 8500,
                    "serviceType": "Oil & Filter Service",
                    "description": "Periodic maintenance",
                    "amount": 350.00,
                    "partsCost": 200.00,
                    "laborCost": 150.00,
                    "serviceProvider": "Porsche Centre",
                    "notes": "Clean inspection",
                    "verified": true,
                    "isUpcoming": false,
                    "syncToFinance": true,
                    "linkedScheduleId": "",
                    "items": [
                        {
                            "name": "Engine Oil 0W-40",
                            "category": "OIL",
                            "price": 120.00,
                            "quantity": 4.5
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/v1/vehicles/" + vehicleId + "/service-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(recordId.toString()))
                .andExpect(jsonPath("$.serviceType").value("Oil & Filter Service"))
                .andExpect(jsonPath("$.cost").value(350.00));

        verify(serviceLedgerService).createServiceRecord(eq(vehicleId), any());
    }

    @Test
    void updateServiceRecord_succeeds() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        ServiceRecordDto recordDto = new ServiceRecordDto(
                recordId, vehicleId, LocalDate.of(2026, 10, 25), 8900, "Major Inspection",
                "Updated details", new BigDecimal("600.00"), new BigDecimal("400.00"),
                new BigDecimal("200.00"), "Specialist Workshop", "Done", true, false,
                "tx-456", List.of(), null, null
        );

        when(serviceLedgerService.updateServiceRecord(eq(recordId), any())).thenReturn(recordDto);

        String payload = """
                {
                    "serviceType": "Major Inspection",
                    "kmAtService": 8900,
                    "cost": 600.00,
                    "linkedScheduleId": ""
                }
                """;

        mockMvc.perform(put("/api/v1/vehicles/" + vehicleId + "/service-records/" + recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(recordId.toString()))
                .andExpect(jsonPath("$.serviceType").value("Major Inspection"))
                .andExpect(jsonPath("$.kmAtService").value(8900));

        verify(serviceLedgerService).updateServiceRecord(eq(recordId), any());
    }
}
