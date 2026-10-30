package com.monday.app.administration.service;

import com.monday.app.administration.dto.ChecklistDto;
import com.monday.app.administration.dto.ChecklistItemDto;
import com.monday.app.administration.dto.CreateChecklistItemRequest;
import com.monday.app.administration.dto.CreateChecklistRequest;
import com.monday.app.administration.entity.AdministrationChecklist;
import com.monday.app.administration.entity.AdministrationChecklistItem;
import com.monday.app.administration.repository.AdministrationChecklistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdministrationChecklistServiceTest {

    private AdministrationChecklistRepository repository;
    private AdministrationChecklistService service;

    @BeforeEach
    void setUp() {
        repository = mock(AdministrationChecklistRepository.class);
        service = new AdministrationChecklistService(repository);
    }

    @Test
    void testCreateChecklistIsolatedToAdministrationRecord() {
        UUID recordId = UUID.randomUUID();
        CreateChecklistRequest req = new CreateChecklistRequest(
                "Passport Renewal Prep", "Document checklist", "APPOINTMENT", recordId
        );

        when(repository.saveChecklist(any(AdministrationChecklist.class))).thenAnswer(inv -> {
            AdministrationChecklist c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });

        ChecklistDto dto = service.createChecklist(req);
        assertNotNull(dto);
        assertEquals("Passport Renewal Prep", dto.title());
        assertEquals("APPOINTMENT", dto.administrationRecordType());
        assertEquals(recordId, dto.administrationRecordId());
        assertEquals("ACTIVE", dto.status());
    }

    @Test
    void testToggleItemCompletionUpdatesChecklistStatus() {
        UUID checklistId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        AdministrationChecklist checklist = new AdministrationChecklist(
                checklistId, "Birthday Party", null, "IMPORTANT_DATE", UUID.randomUUID(),
                "ACTIVE", Instant.now(), Instant.now()
        );

        AdministrationChecklistItem item = new AdministrationChecklistItem(
                itemId, checklistId, "Order cake", null, 0, false, null, Instant.now(), Instant.now()
        );

        when(repository.findItemById(itemId)).thenReturn(Optional.of(item));
        when(repository.findChecklistById(checklistId)).thenReturn(Optional.of(checklist));
        when(repository.findItemsByChecklistId(checklistId)).thenReturn(List.of(item));

        ChecklistItemDto toggled = service.toggleItemCompleted(checklistId, itemId);
        assertTrue(toggled.completed());
        verify(repository, times(1)).setItemCompleted(itemId, true);
        verify(repository, times(1)).updateChecklist(any(AdministrationChecklist.class));
    }

    @Test
    void testAddItemAssignsSequentialPosition() {
        UUID checklistId = UUID.randomUUID();
        when(repository.findChecklistById(checklistId)).thenReturn(Optional.of(new AdministrationChecklist()));
        when(repository.getNextPosition(checklistId)).thenReturn(2);
        when(repository.saveItem(any(AdministrationChecklistItem.class))).thenAnswer(inv -> {
            AdministrationChecklistItem i = inv.getArgument(0);
            i.setId(UUID.randomUUID());
            return i;
        });

        CreateChecklistItemRequest req = new CreateChecklistItemRequest("New Item", "Description", null);
        ChecklistItemDto item = service.addItem(checklistId, req);

        assertEquals(2, item.position());
        assertEquals("New Item", item.title());
    }
}
