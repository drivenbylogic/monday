package com.monday.app.administration.service;

import com.monday.app.administration.dto.*;
import com.monday.app.administration.entity.AdministrationChecklist;
import com.monday.app.administration.entity.AdministrationChecklistItem;
import com.monday.app.administration.repository.AdministrationChecklistRepository;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdministrationChecklistService {

    private final AdministrationChecklistRepository repository;

    public AdministrationChecklistService(AdministrationChecklistRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ChecklistDto> getAllChecklists() {
        return repository.findAllChecklists().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChecklistDto getChecklistById(UUID id) {
        AdministrationChecklist c = repository.findChecklistById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist not found: " + id));
        return toDto(c);
    }

    @Transactional(readOnly = true)
    public List<ChecklistDto> getChecklistsForRecord(String recordType, UUID recordId) {
        return repository.findChecklistsByTarget(recordType.toUpperCase().trim(), recordId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChecklistDto> getChecklistsForRecordType(String recordType) {
        return repository.findChecklistsByRecordType(recordType.toUpperCase().trim()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ChecklistDto createChecklist(CreateChecklistRequest req) {
        AdministrationChecklist c = new AdministrationChecklist();
        c.setTitle(req.title().trim());
        c.setDescription(req.description());
        String recType = req.getRecordType();
        c.setAdministrationRecordType(recType != null ? recType.toUpperCase().trim() : null);
        c.setAdministrationRecordId(req.getRecordId());
        c.setStatus("ACTIVE");

        AdministrationChecklist saved = repository.saveChecklist(c);
        return toDto(saved);
    }

    @Transactional
    public ChecklistDto updateChecklist(UUID id, String title, String description, String status) {
        AdministrationChecklist c = repository.findChecklistById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist not found: " + id));

        if (title != null && !title.isBlank()) c.setTitle(title.trim());
        if (description != null) c.setDescription(description);
        if (status != null && !status.isBlank()) c.setStatus(status.toUpperCase().trim());

        AdministrationChecklist updated = repository.updateChecklist(c);
        return toDto(updated);
    }

    @Transactional
    public void deleteChecklist(UUID id) {
        repository.findChecklistById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist not found: " + id));
        repository.deleteChecklistById(id);
    }

    @Transactional
    public ChecklistItemDto addItem(UUID checklistId, CreateChecklistItemRequest req) {
        repository.findChecklistById(checklistId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist not found: " + checklistId));

        int position = req.position() != null ? req.position() : repository.getNextPosition(checklistId);

        AdministrationChecklistItem item = new AdministrationChecklistItem();
        item.setChecklistId(checklistId);
        item.setTitle(req.title().trim());
        item.setDescription(req.description());
        item.setPosition(position);
        item.setCompleted(false);

        AdministrationChecklistItem saved = repository.saveItem(item);
        return toItemDto(saved);
    }

    @Transactional
    public ChecklistItemDto updateItem(UUID checklistId, UUID itemId, UpdateChecklistItemRequest req) {
        AdministrationChecklistItem item = repository.findItemById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found: " + itemId));

        if (checklistId != null && !item.getChecklistId().equals(checklistId)) {
            throw new IllegalArgumentException("Item does not belong to specified checklist");
        }
        UUID effectiveChecklistId = checklistId != null ? checklistId : item.getChecklistId();

        if (req.title() != null && !req.title().isBlank()) item.setTitle(req.title().trim());
        if (req.description() != null) item.setDescription(req.description());
        if (req.position() != null) item.setPosition(req.position());
        if (req.resolveCompleted() != null) item.setCompleted(req.resolveCompleted());

        AdministrationChecklistItem updated = repository.updateItem(item);
        syncChecklistCompletion(effectiveChecklistId);
        return toItemDto(updated);
    }

    @Transactional
    public ChecklistItemDto toggleItemCompleted(UUID checklistId, UUID itemId) {
        AdministrationChecklistItem item = repository.findItemById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found: " + itemId));

        if (checklistId != null && !item.getChecklistId().equals(checklistId)) {
            throw new IllegalArgumentException("Item does not belong to specified checklist");
        }
        UUID effectiveChecklistId = checklistId != null ? checklistId : item.getChecklistId();

        boolean nextCompleted = !item.isCompleted();
        repository.setItemCompleted(itemId, nextCompleted);
        item.setCompleted(nextCompleted);

        syncChecklistCompletion(effectiveChecklistId);
        return toItemDto(item);
    }

    @Transactional
    public void deleteItem(UUID checklistId, UUID itemId) {
        AdministrationChecklistItem item = repository.findItemById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found: " + itemId));

        if (checklistId != null && !item.getChecklistId().equals(checklistId)) {
            throw new IllegalArgumentException("Item does not belong to specified checklist");
        }
        UUID effectiveChecklistId = checklistId != null ? checklistId : item.getChecklistId();

        repository.deleteItemById(itemId);
        syncChecklistCompletion(effectiveChecklistId);
    }

    @Transactional
    public ChecklistDto reorderItems(UUID checklistId, ReorderChecklistItemsRequest req) {
        repository.findChecklistById(checklistId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist not found: " + checklistId));

        repository.reorderItems(checklistId, req.itemIds());
        return getChecklistById(checklistId);
    }

    private void syncChecklistCompletion(UUID checklistId) {
        List<AdministrationChecklistItem> items = repository.findItemsByChecklistId(checklistId);
        if (items.isEmpty()) return;

        boolean allCompleted = items.stream().allMatch(AdministrationChecklistItem::isCompleted);
        repository.findChecklistById(checklistId).ifPresent(c -> {
            String newStatus = allCompleted ? "COMPLETED" : "ACTIVE";
            if (!newStatus.equals(c.getStatus())) {
                c.setStatus(newStatus);
                repository.updateChecklist(c);
            }
        });
    }

    public ChecklistDto toDto(AdministrationChecklist c) {
        List<AdministrationChecklistItem> items = repository.findItemsByChecklistId(c.getId());
        List<ChecklistItemDto> itemDtos = items.stream().map(this::toItemDto).toList();
        int total = itemDtos.size();
        int completed = (int) itemDtos.stream().filter(ChecklistItemDto::completed).count();
        int progress = total > 0 ? (completed * 100) / total : 0;

        return new ChecklistDto(
                c.getId(),
                c.getTitle(),
                c.getDescription(),
                c.getAdministrationRecordType(),
                c.getAdministrationRecordId(),
                c.getStatus(),
                itemDtos,
                completed,
                total,
                progress,
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    public ChecklistItemDto toItemDto(AdministrationChecklistItem i) {
        return new ChecklistItemDto(
                i.getId(),
                i.getChecklistId(),
                i.getTitle(),
                i.getDescription(),
                i.getPosition(),
                i.isCompleted(),
                i.getCompletedAt(),
                i.getCreatedAt(),
                i.getUpdatedAt()
        );
    }
}
