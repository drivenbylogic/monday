package com.monday.app.agenda.dto;

import java.time.LocalDate;

public record GenerateAgendaRequest(
        LocalDate date,
        String timezone,
        Boolean force
) {}
