package com.monday.app.vehicle.service;

import com.monday.app.shared.exception.ResourceNotFoundException;
import com.monday.app.vehicle.dto.*;
import com.monday.app.vehicle.entity.Vehicle;
import com.monday.app.vehicle.entity.VehicleServiceItem;
import com.monday.app.vehicle.entity.VehicleServiceRecord;
import com.monday.app.vehicle.repository.VehicleRepository;
import com.monday.app.vehicle.repository.VehicleServiceItemRepository;
import com.monday.app.vehicle.repository.VehicleServiceRecordRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleServiceLedgerService {

    private final VehicleServiceRecordRepository serviceRecordRepository;
    private final VehicleServiceItemRepository serviceItemRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleOdometerService odometerService;
    private final VehicleMaintenanceScheduleService scheduleService;
    private final NamedParameterJdbcTemplate jdbc;

    public VehicleServiceLedgerService(VehicleServiceRecordRepository serviceRecordRepository,
                                       VehicleServiceItemRepository serviceItemRepository,
                                       VehicleRepository vehicleRepository,
                                       VehicleOdometerService odometerService,
                                       VehicleMaintenanceScheduleService scheduleService,
                                       NamedParameterJdbcTemplate jdbc) {
        this.serviceRecordRepository = serviceRecordRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.vehicleRepository = vehicleRepository;
        this.odometerService = odometerService;
        this.scheduleService = scheduleService;
        this.jdbc = jdbc;
    }

    public List<ServiceRecordDto> getServiceRecords(UUID vehicleId, String typeFilter, int limit) {
        vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        List<VehicleServiceRecord> records = serviceRecordRepository.findByVehicleId(vehicleId);

        return records.stream()
                .filter(r -> {
                    if ("completed".equalsIgnoreCase(typeFilter)) return !Boolean.TRUE.equals(r.getIsUpcoming());
                    if ("upcoming".equalsIgnoreCase(typeFilter)) return Boolean.TRUE.equals(r.getIsUpcoming());
                    return true;
                })
                .limit(limit > 0 ? limit : 50)
                .map(this::enrichWithItems)
                .collect(Collectors.toList());
    }

    public ServiceRecordDto getServiceRecord(UUID id) {
        VehicleServiceRecord record = serviceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleServiceRecord", id));
        return enrichWithItems(record);
    }

    @Transactional
    public ServiceRecordDto createServiceRecord(UUID vehicleId, CreateServiceRecordRequest req) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        BigDecimal partsCost = req.partsCost() != null ? req.partsCost() : BigDecimal.ZERO;
        BigDecimal laborCost = req.laborCost() != null ? req.laborCost() : BigDecimal.ZERO;
        BigDecimal totalCost = req.cost();
        if (totalCost == null || totalCost.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal sum = partsCost.add(laborCost);
            totalCost = sum.compareTo(BigDecimal.ZERO) > 0 ? sum : BigDecimal.ZERO;
        }

        LocalDate serviceDate = req.serviceDate() != null ? req.serviceDate() : LocalDate.now();

        String txId = null;
        if (Boolean.TRUE.equals(req.syncToFinance()) && totalCost.compareTo(BigDecimal.ZERO) > 0) {
            txId = UUID.randomUUID().toString();
        }

        VehicleServiceRecord record = new VehicleServiceRecord(
                null,
                vehicleId,
                serviceDate,
                req.kmAtService(),
                req.serviceType() != null ? req.serviceType() : "Scheduled Service",
                req.description(),
                totalCost,
                req.performedBy(),
                req.notes(),
                null,
                null
        );
        record.setPartsCost(partsCost);
        record.setLaborCost(laborCost);
        record.setVerified(Boolean.TRUE.equals(req.verified()));
        record.setIsUpcoming(Boolean.TRUE.equals(req.isUpcoming()));
        record.setLinkedTransactionId(txId);

        VehicleServiceRecord saved = serviceRecordRepository.save(record);

        // Save service consumable items if present
        List<ServiceItemDto> savedItems = new ArrayList<>();
        if (req.items() != null && !req.items().isEmpty()) {
            for (CreateServiceItemRequest itemReq : req.items()) {
                VehicleServiceItem item = new VehicleServiceItem(
                        null,
                        saved.getId(),
                        itemReq.itemName(),
                        itemReq.category(),
                        itemReq.brandSpec(),
                        itemReq.quantity(),
                        itemReq.unit(),
                        itemReq.itemCost(),
                        itemReq.replacementIntervalMonths(),
                        itemReq.replacementIntervalKm(),
                        itemReq.notes(),
                        null
                );
                VehicleServiceItem savedItem = serviceItemRepository.save(item);
                savedItems.add(toItemDto(savedItem));
            }
        }

        // Update odometer reading if km is recorded and not upcoming
        if (req.kmAtService() != null && req.kmAtService() > 0 && !Boolean.TRUE.equals(req.isUpcoming())) {
            try {
                odometerService.recordReading(vehicleId, new RecordOdometerRequest(
                        serviceDate,
                        req.kmAtService(),
                        "SERVICE",
                        false,
                        "Recorded at service: " + saved.getServiceType()
                ));
            } catch (Exception ignored) {
                // Ignore if odometer is already newer
            }
        }

        // Advance maintenance schedule if linked
        if (req.linkedScheduleId() != null && !Boolean.TRUE.equals(req.isUpcoming())) {
            scheduleService.advanceScheduleAfterService(req.linkedScheduleId(), serviceDate, req.kmAtService());
        }

        // Create transaction in personal finance if synced
        if (txId != null) {
            String desc = "Service: " + saved.getServiceType() + " (" + vehicle.getName() + ")";
            String txSql = """
                    INSERT INTO transactions (id, transaction_type, amount, transaction_at, description, linked_module, linked_entity_id, linked_entity_type)
                    VALUES (:txId::uuid, 'EXPENSE', :amount, CURRENT_TIMESTAMP, :desc, 'VEHICLES', :vId, 'SERVICE_RECORD')
                    """;
            jdbc.update(txSql, new MapSqlParameterSource()
                    .addValue("txId", txId)
                    .addValue("amount", totalCost)
                    .addValue("desc", desc)
                    .addValue("vId", vehicleId.toString()));
        }

        return toDto(saved, savedItems);
    }

    @Transactional
    public ServiceRecordDto updateServiceRecord(UUID id, UpdateServiceRecordRequest req) {
        VehicleServiceRecord existing = serviceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleServiceRecord", id));

        if (req.serviceDate() != null) existing.setServiceDate(req.serviceDate());
        if (req.kmAtService() != null) existing.setKmAtService(req.kmAtService());
        if (req.serviceType() != null) existing.setServiceType(req.serviceType());
        if (req.description() != null) existing.setDescription(req.description());
        if (req.partsCost() != null) existing.setPartsCost(req.partsCost());
        if (req.laborCost() != null) existing.setLaborCost(req.laborCost());

        BigDecimal cost = req.cost();
        if (cost == null && (req.partsCost() != null || req.laborCost() != null)) {
            BigDecimal p = existing.getPartsCost() != null ? existing.getPartsCost() : BigDecimal.ZERO;
            BigDecimal l = existing.getLaborCost() != null ? existing.getLaborCost() : BigDecimal.ZERO;
            cost = p.add(l);
        }
        if (cost != null) existing.setCost(cost);

        if (req.performedBy() != null) existing.setPerformedBy(req.performedBy());
        if (req.notes() != null) existing.setNotes(req.notes());
        if (req.verified() != null) existing.setVerified(req.verified());
        if (req.isUpcoming() != null) existing.setIsUpcoming(req.isUpcoming());

        VehicleServiceRecord updated = serviceRecordRepository.update(existing);

        // Update items if supplied
        if (req.items() != null) {
            serviceItemRepository.deleteByServiceRecordId(id);
            for (CreateServiceItemRequest itemReq : req.items()) {
                VehicleServiceItem item = new VehicleServiceItem(
                        null,
                        id,
                        itemReq.itemName(),
                        itemReq.category(),
                        itemReq.brandSpec(),
                        itemReq.quantity(),
                        itemReq.unit(),
                        itemReq.itemCost(),
                        itemReq.replacementIntervalMonths(),
                        itemReq.replacementIntervalKm(),
                        itemReq.notes(),
                        null
                );
                serviceItemRepository.save(item);
            }
        }

        return enrichWithItems(updated);
    }

    @Transactional
    public void deleteServiceRecord(UUID id) {
        serviceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleServiceRecord", id));
        serviceRecordRepository.deleteById(id);
    }

    private ServiceRecordDto enrichWithItems(VehicleServiceRecord r) {
        List<VehicleServiceItem> items = serviceItemRepository.findByServiceRecordId(r.getId());
        List<ServiceItemDto> itemDtos = items.stream().map(this::toItemDto).toList();
        return toDto(r, itemDtos);
    }

    private ServiceRecordDto toDto(VehicleServiceRecord r, List<ServiceItemDto> items) {
        return new ServiceRecordDto(
                r.getId(),
                r.getVehicleId(),
                r.getServiceDate(),
                r.getKmAtService(),
                r.getServiceType(),
                r.getDescription(),
                r.getCost(),
                r.getPartsCost(),
                r.getLaborCost(),
                r.getPerformedBy(),
                r.getNotes(),
                r.getVerified(),
                r.getIsUpcoming(),
                r.getLinkedTransactionId(),
                items,
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private ServiceItemDto toItemDto(VehicleServiceItem item) {
        return new ServiceItemDto(
                item.getId(),
                item.getServiceRecordId(),
                item.getItemName(),
                item.getCategory(),
                item.getBrandSpec(),
                item.getQuantity(),
                item.getUnit(),
                item.getItemCost(),
                item.getReplacementIntervalMonths(),
                item.getReplacementIntervalKm(),
                item.getNotes(),
                item.getCreatedAt()
        );
    }
}
