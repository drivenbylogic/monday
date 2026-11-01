package com.monday.app.administration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.monday.app.administration.dto.*;
import com.monday.app.administration.service.*;
import com.monday.app.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdministrationControllerTest {

    private MockMvc mockMvc;
    private AdministrationObligationService obligationService;
    private AdministrationPersonService personService;
    private AdministrationImportantDateService dateService;
    private AdministrationGadgetService gadgetService;
    private AdministrationMaintenanceService maintenanceService;
    private AdministrationAppointmentService appointmentService;
    private AdministrationChecklistService checklistService;
    private AdministrationNoteService noteService;
    private AdministrationOverviewService overviewService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        obligationService = mock(AdministrationObligationService.class);
        personService = mock(AdministrationPersonService.class);
        dateService = mock(AdministrationImportantDateService.class);
        gadgetService = mock(AdministrationGadgetService.class);
        maintenanceService = mock(AdministrationMaintenanceService.class);
        appointmentService = mock(AdministrationAppointmentService.class);
        checklistService = mock(AdministrationChecklistService.class);
        noteService = mock(AdministrationNoteService.class);
        overviewService = mock(AdministrationOverviewService.class);

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdministrationObligationController(obligationService),
                        new AdministrationPersonController(personService, dateService),
                        new AdministrationImportantDateController(dateService),
                        new AdministrationGadgetController(gadgetService, maintenanceService),
                        new AdministrationAppointmentController(appointmentService),
                        new AdministrationChecklistController(checklistService),
                        new AdministrationNoteController(noteService),
                        new AdministrationOverviewController(overviewService)
                )
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetObligations() throws Exception {
        UUID id = UUID.randomUUID();
        when(obligationService.getObligations(null, null, null, null)).thenReturn(List.of(
                new ObligationDto(
                        id, "Netflix Premium", "Streaming", "Netflix", "STREAMING", "SUBSCRIPTION",
                        new BigDecimal("19.99"), "USD", "MONTHLY", null,
                        LocalDate.now(), LocalDate.now().plusDays(10), null, null, null,
                        true, "ACTIVE", 10, false, false, null, Instant.now(), Instant.now()
                )
        ));

        mockMvc.perform(get("/api/v1/administration/obligations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Netflix Premium"))
                .andExpect(jsonPath("$[0].expectedAmount").value(19.99));
    }

    @Test
    void testCreateObligation() throws Exception {
        UUID id = UUID.randomUUID();
        when(obligationService.create(any(CreateObligationRequest.class))).thenReturn(
                new ObligationDto(
                        id, "City Water & Power", "Utility bill", "City Utility", "UTILITIES", "BILL",
                        new BigDecimal("120.00"), "USD", "MONTHLY", null,
                        LocalDate.now(), LocalDate.now().plusDays(5), null, null, null,
                        false, "ACTIVE", 5, true, false, null, Instant.now(), Instant.now()
                )
        );

        CreateObligationRequest req = new CreateObligationRequest(
                "City Water & Power", "Utility bill", "City Utility", "UTILITIES", "BILL",
                new BigDecimal("120.00"), "USD", "MONTHLY", null,
                LocalDate.now(), LocalDate.now().plusDays(5), null, null, null,
                false, "ACTIVE"
        );

        mockMvc.perform(post("/api/v1/administration/obligations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("City Water & Power"));
    }

    @Test
    void testGetOverview() throws Exception {
        when(overviewService.getOverview()).thenReturn(
                new AdministrationOverviewDto(
                        5, 2, 0, new BigDecimal("250.00"), List.of(),
                        4, 3, List.of(),
                        3, 1, 2, List.of(),
                        2, List.of(),
                        2, 3, 2, List.of()
                )
        );

        mockMvc.perform(get("/api/v1/administration/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveObligations").value(5))
                .andExpect(jsonPath("$.totalPeopleCount").value(4))
                .andExpect(jsonPath("$.totalGadgetsCount").value(3));
    }

    @Test
    void testChecklistCrudEndpoints() throws Exception {
        UUID checklistId = UUID.randomUUID();
        when(checklistService.getAllChecklists()).thenReturn(List.of(
                new ChecklistDto(
                        checklistId, "House Move Checklist", "Tasks", null, null,
                        "ACTIVE", List.of(), 0, 0, 0, Instant.now(), Instant.now()
                )
        ));

        mockMvc.perform(get("/api/v1/administration/checklists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("House Move Checklist"));
    }

    @Test
    void testNotesEndpoint() throws Exception {
        UUID noteId = UUID.randomUUID();
        when(noteService.getNotes(null)).thenReturn(List.of(
                new NoteDto(noteId, "Paint Codes", "Living room Benjamin Moore", Instant.now(), Instant.now())
        ));

        mockMvc.perform(get("/api/v1/administration/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Paint Codes"));
    }

    @Test
    void testCreatePersonWithRelationshipAttribute() throws Exception {
        UUID personId = UUID.randomUUID();
        when(personService.create(any(CreatePersonRequest.class))).thenAnswer(inv -> {
            CreatePersonRequest req = inv.getArgument(0);
            return new PersonDto(
                    personId, req.displayName(), req.relationshipLabel(),
                    req.birthday(), req.contactInformation(), req.notes(),
                    0, null, Instant.now(), Instant.now()
            );
        });

        String jsonPayload = """
                {
                    "displayName": "Alex Turner",
                    "relationship": "Close Friend",
                    "birthday": "1990-01-15",
                    "contactInfo": "alex@example.com",
                    "generalNotes": "Met in college"
                }
                """;

        mockMvc.perform(post("/api/v1/administration/people")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value("Alex Turner"))
                .andExpect(jsonPath("$.relationshipLabel").value("Close Friend"))
                .andExpect(jsonPath("$.relationship").value("Close Friend"))
                .andExpect(jsonPath("$.contactInformation").value("alex@example.com"))
                .andExpect(jsonPath("$.contactInfo").value("alex@example.com"))
                .andExpect(jsonPath("$.notes").value("Met in college"))
                .andExpect(jsonPath("$.generalNotes").value("Met in college"));
    }

    @Test
    void testUpdatePersonWithRelationshipAttribute() throws Exception {
        UUID personId = UUID.randomUUID();
        when(personService.update(any(UUID.class), any(UpdatePersonRequest.class))).thenAnswer(inv -> {
            UpdatePersonRequest req = inv.getArgument(1);
            return new PersonDto(
                    personId, req.displayName() != null ? req.displayName() : "Alex Turner",
                    req.relationshipLabel(),
                    req.birthday(), req.contactInformation(), req.notes(),
                    0, null, Instant.now(), Instant.now()
            );
        });

        String jsonPayload = """
                {
                    "relationship": "Business Partner"
                }
                """;

        mockMvc.perform(put("/api/v1/administration/people/" + personId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relationshipLabel").value("Business Partner"))
                .andExpect(jsonPath("$.relationship").value("Business Partner"));
    }
}
