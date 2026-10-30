package com.monday.app.administration.service;

import com.monday.app.administration.dto.CreateGadgetRequest;
import com.monday.app.administration.dto.GadgetDto;
import com.monday.app.administration.dto.UpdateGadgetRequest;
import com.monday.app.administration.entity.AdministrationGadget;
import com.monday.app.administration.repository.AdministrationGadgetRepository;
import com.monday.app.administration.util.AdministrationDateUtils;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AdministrationGadgetService {

    private final AdministrationGadgetRepository repository;

    public AdministrationGadgetService(AdministrationGadgetRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<GadgetDto> getGadgets(String query, String category, Boolean archived) {
        LocalDate today = LocalDate.now();
        return repository.search(query, category, archived).stream()
                .map(g -> toDto(g, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public GadgetDto getById(UUID id) {
        AdministrationGadget gadget = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration gadget not found: " + id));
        return toDto(gadget, LocalDate.now());
    }

    @Transactional
    public GadgetDto create(CreateGadgetRequest req) {
        AdministrationGadget g = new AdministrationGadget();
        g.setName(req.name().trim());
        g.setCategory(req.category().toUpperCase().trim());
        g.setBrand(req.brand());
        g.setModel(req.model());
        g.setSerialNumber(req.serialNumber());
        g.setPurchaseDate(req.purchaseDate());
        g.setPurchasePrice(req.purchasePrice());
        g.setWarrantyStartDate(req.warrantyStartDate());
        g.setWarrantyEndDate(req.warrantyEndDate());
        g.setWarrantyProvider(req.warrantyProvider());
        g.setNotes(req.notes());

        AdministrationGadget saved = repository.save(g);
        return toDto(saved, LocalDate.now());
    }

    @Transactional
    public GadgetDto update(UUID id, UpdateGadgetRequest req) {
        AdministrationGadget g = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration gadget not found: " + id));

        if (req.name() != null && !req.name().isBlank()) g.setName(req.name().trim());
        if (req.category() != null && !req.category().isBlank()) g.setCategory(req.category().toUpperCase().trim());
        if (req.brand() != null) g.setBrand(req.brand());
        if (req.model() != null) g.setModel(req.model());
        if (req.serialNumber() != null) g.setSerialNumber(req.serialNumber());
        if (req.purchaseDate() != null) g.setPurchaseDate(req.purchaseDate());
        if (req.purchasePrice() != null) g.setPurchasePrice(req.purchasePrice());
        if (req.warrantyStartDate() != null) g.setWarrantyStartDate(req.warrantyStartDate());
        if (req.warrantyEndDate() != null) g.setWarrantyEndDate(req.warrantyEndDate());
        if (req.warrantyProvider() != null) g.setWarrantyProvider(req.warrantyProvider());
        if (req.notes() != null) g.setNotes(req.notes());

        AdministrationGadget updated = repository.update(g);
        return toDto(updated, LocalDate.now());
    }

    @Transactional
    public void archive(UUID id) {
        repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration gadget not found: " + id));
        repository.archive(id);
    }

    @Transactional
    public void unarchive(UUID id) {
        repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration gadget not found: " + id));
        repository.unarchive(id);
    }

    @Transactional
    public void delete(UUID id) {
        repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration gadget not found: " + id));
        repository.deleteById(id);
    }

    public GadgetDto toDto(AdministrationGadget g, LocalDate today) {
        String warrantyStatus = AdministrationDateUtils.computeWarrantyStatus(
                g.getWarrantyStartDate(), g.getWarrantyEndDate(), today);
        Long warrantyDaysRemaining = g.getWarrantyEndDate() != null
                ? AdministrationDateUtils.calculateDaysRemaining(g.getWarrantyEndDate(), today)
                : null;
        int maintenanceCount = repository.countMaintenance(g.getId());

        return new GadgetDto(
                g.getId(),
                g.getName(),
                g.getCategory(),
                g.getBrand(),
                g.getModel(),
                g.getSerialNumber(),
                g.getPurchaseDate(),
                g.getPurchasePrice(),
                g.getWarrantyStartDate(),
                g.getWarrantyEndDate(),
                g.getWarrantyProvider(),
                warrantyStatus,
                warrantyDaysRemaining,
                g.getNotes(),
                maintenanceCount,
                g.getArchivedAt(),
                g.getCreatedAt(),
                g.getUpdatedAt()
        );
    }
}
