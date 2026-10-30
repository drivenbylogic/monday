package com.monday.app.administration.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateNoteRequest(
        String title,
        @NotBlank(message = "Note body cannot be empty") String body
) {}
