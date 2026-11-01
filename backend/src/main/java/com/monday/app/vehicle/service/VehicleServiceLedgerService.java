package com.monday.app.vehicle.service;

import com.monday.app.shared.exception.ResourceNotFoundException;
import com.monday.app.vehicle.dto.*;
import com.monday.app.vehicle.entity.Vehicle;
import com.monday.app.vehicle.entity.VehicleOdometerReading;
import com.monday.app.vehicle.entity.VehicleServiceItem;
import com.monday.app.vehicle.entity.VehicleServiceRecord;
import com.monday.app.vehicle.repository.VehicleOdometerReadingRepository;
import com.monday.app.vehicle.repository.VehicleRepository;
import com.monday.app.vehicle.repository.VehicleServiceItemRepository;
import com.monday.app.vehicle.repository.VehicleServiceRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleServiceLedgerService {

    private final VehicleServiceRecordRepository serviceRecordRepository;
    private final VehicleServiceItemRepository serviceItemRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleOdometerReadingRepository odometerRepository;
    private final VehicleOdometerService odometerService;
    private final VehicleMaintenanceScheduleService scheduleService;
    private final NamedParameterJdbcTemplate jdbc;

    @Autowired
    public VehicleServiceLedgerService(VehicleServiceRecordRepository serviceRecordRepository,
                                       VehicleServiceItemRepository serviceItemRepository,
                                       VehicleRepository vehicleRepository,
                                       VehicleOdometerReadingRepository odometerRepository,
                                       VehicleOdometerService odometerService,
                                       VehicleMaintenanceScheduleService scheduleService,
                                       NamedParameterJdbcTemplate jdbc) {
        this.serviceRecordRepository = serviceRecordRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.vehicleRepository = vehicleRepository;
        this.odometerRepository = odometerRepository;
        this.odometerService = odometerService;
        this.scheduleService = scheduleService;
        this.jdbc = jdbc;
    }

    public VehicleServiceLedgerService(VehicleServiceRecordRepository serviceRecordRepository,
                                       VehicleServiceItemRepository serviceItemRepository,
                                       VehicleRepository vehicleRepository,
                                       VehicleOdometerService odometerService,
                                       VehicleMaintenanceScheduleService scheduleService,
                                       NamedParameterJdbcTemplate jdbc) {
        this(serviceRecordRepository, serviceItemRepository, vehicleRepository, null, odometerService, scheduleService, jdbc);
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

        BigDecimal partsCost = req.partsCost() != null && req.partsCost().compareTo(BigDecimal.ZERO) >= 0 ? req.partsCost() : BigDecimal.ZERO;
        BigDecimal laborCost = req.laborCost() != null && req.laborCost().compareTo(BigDecimal.ZERO) >= 0 ? req.laborCost() : BigDecimal.ZERO;
        BigDecimal totalCost = req.cost();
        if (totalCost == null || totalCost.compareTo(BigDecimal.ZERO) < 0) {
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
                req.kmAtService() != null && req.kmAtService() >= 0 ? req.kmAtService() : null,
                req.serviceType() != null && !req.serviceType().isBlank() ? req.serviceType().trim() : "Scheduled Service",
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
                if (itemReq == null) continue;
                String itemName = itemReq.itemName() != null && !itemReq.itemName().isBlank() ? itemReq.itemName().trim() : "Consumable / Part";
                BigDecimal qty = itemReq.quantity() != null && itemReq.quantity().compareTo(BigDecimal.ZERO) > 0 ? itemReq.quantity() : BigDecimal.ONE;
                BigDecimal itemCost = itemReq.itemCost() != null && itemReq.itemCost().compareTo(BigDecimal.ZERO) >= 0 ? itemReq.itemCost() : BigDecimal.ZERO;
                Integer months = itemReq.replacementIntervalMonths() != null && itemReq.replacementIntervalMonths() > 0 ? itemReq.replacementIntervalMonths() : null;
                Integer km = itemReq.replacementIntervalKm() != null && itemReq.replacementIntervalKm() > 0 ? itemReq.replacementIntervalKm() : null;

                VehicleServiceItem item = new VehicleServiceItem(
                        null,
                        saved.getId(),
                        itemName,
                        itemReq.category() != null && !itemReq.category().isBlank() ? itemReq.category().trim() : "PART",
                        itemReq.brandSpec(),
                        qty,
                        itemReq.unit() != null ? itemReq.unit() : "pcs",
                        itemCost,
                        months,
                        km,
                        itemReq.notes(),
                        null
                );
                VehicleServiceItem savedItem = serviceItemRepository.save(item);
                savedItems.add(toItemDto(savedItem));
            }
        }

        // Safely update odometer reading if km is recorded and non-decreasing
        recordOdometerIfApplicable(vehicleId, req.kmAtService(), serviceDate, saved.getServiceType(), Boolean.TRUE.equals(req.isUpcoming()));

        // Advance maintenance schedule if linked
        UUID scheduleId = req.getParsedLinkedScheduleId();
        advanceScheduleIfApplicable(scheduleId, serviceDate, req.kmAtService(), Boolean.TRUE.equals(req.isUpcoming()));

        // Create transaction in personal finance if synced
        if (txId != null && totalCost.compareTo(BigDecimal.ZERO) > 0) {
            try {
                String vehicleName = vehicle.getName() != null ? vehicle.getName() : "Vehicle";
                String serviceTypeName = saved.getServiceType() != null ? saved.getServiceType() : "Service";
                String desc = "Service: " + serviceTypeName + " (" + vehicleName + ")";
                if (desc.length() > 255) {
                    desc = desc.substring(0, 255);
                }
                String txSql = """
                        INSERT INTO transactions (id, transaction_type, amount, transaction_at, description, linked_module, linked_entity_id, linked_entity_type)
                        VALUES (:txId::uuid, 'EXPENSE', :amount, CURRENT_TIMESTAMP, :desc, 'VEHICLES', :vId, 'SERVICE_RECORD')
                        """;
                jdbc.update(txSql, new MapSqlParameterSource()
                        .addValue("txId", txId)
                        .addValue("amount", totalCost)
                        .addValue("desc", desc)
                        .addValue("vId", vehicleId.toString()));
            } catch (Exception e) {
                // If finance transaction insertion fails, clear txId on the record to avoid dangling reference
                saved.setLinkedTransactionId(null);
                serviceRecordRepository.update(saved);
            }
        }

        return toDto(saved, savedItems);
    }

    @Transactional
    public ServiceRecordDto updateServiceRecord(UUID id, UpdateServiceRecordRequest req) {
        VehicleServiceRecord existing = serviceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleServiceRecord", id));

        if (req.serviceDate() != null) existing.setServiceDate(req.serviceDate());
        if (req.kmAtService() != null) {
            existing.setKmAtService(req.kmAtService() >= 0 ? req.kmAtService() : null);
        }
        if (req.serviceType() != null && !req.serviceType().isBlank()) {
            existing.setServiceType(req.serviceType().trim());
        }
        if (req.description() != null) existing.setDescription(req.description());
        if (req.partsCost() != null) {
            existing.setPartsCost(req.partsCost().compareTo(BigDecimal.ZERO) >= 0 ? req.partsCost() : BigDecimal.ZERO);
        }
        if (req.laborCost() != null) {
            existing.setLaborCost(req.laborCost().compareTo(BigDecimal.ZERO) >= 0 ? req.laborCost() : BigDecimal.ZERO);
        }

        BigDecimal cost = req.cost();
        if (cost == null && (req.partsCost() != null || req.laborCost() != null)) {
            BigDecimal p = existing.getPartsCost() != null ? existing.getPartsCost() : BigDecimal.ZERO;
            BigDecimal l = existing.getLaborCost() != null ? existing.getLaborCost() : BigDecimal.ZERO;
            cost = p.add(l);
        }
        if (cost != null) {
            existing.setCost(cost.compareTo(BigDecimal.ZERO) >= 0 ? cost : BigDecimal.ZERO);
        }

        if (req.performedBy() != null) existing.setPerformedBy(req.performedBy());
        if (req.notes() != null) existing.setNotes(req.notes());
        if (req.verified() != null) existing.setVerified(req.verified());
        if (req.isUpcoming() != null) existing.setIsUpcoming(req.isUpcoming());

        // Update items if supplied
        if (req.items() != null) {
            serviceItemRepository.deleteByServiceRecordId(id);
            for (CreateServiceItemRequest itemReq : req.items()) {
                if (itemReq == null) continue;
                String itemName = itemReq.itemName() != null && !itemReq.itemName().isBlank() ? itemReq.itemName().trim() : "Consumable / Part";
                BigDecimal qty = itemReq.quantity() != null && itemReq.quantity().compareTo(BigDecimal.ZERO) > 0 ? itemReq.quantity() : BigDecimal.ONE;
                BigDecimal itemCost = itemReq.itemCost() != null && itemReq.itemCost().compareTo(BigDecimal.ZERO) >= 0 ? itemReq.itemCost() : BigDecimal.ZERO;
                Integer months = itemReq.replacementIntervalMonths() != null && itemReq.replacementIntervalMonths() > 0 ? itemReq.replacementIntervalMonths() : null;
                Integer km = itemReq.replacementIntervalKm() != null && itemReq.replacementIntervalKm() > 0 ? itemReq.replacementIntervalKm() : null;

                VehicleServiceItem item = new VehicleServiceItem(
                        null,
                        id,
                        itemName,
                        itemReq.category() != null && !itemReq.category().isBlank() ? itemReq.category().trim() : "PART",
                        itemReq.brandSpec(),
                        qty,
                        itemReq.unit() != null ? itemReq.unit() : "pcs",
                        itemCost,
                        months,
                        km,
                        itemReq.notes(),
                        null
                );
                serviceItemRepository.save(item);
            }
        }

        // Safely record odometer reading if updated
        if (req.kmAtService() != null) {
            recordOdometerIfApplicable(existing.getVehicleId(), req.kmAtService(), existing.getServiceDate(), existing.getServiceType(), Boolean.TRUE.equals(existing.getIsUpcoming()));
        }

        // Advance maintenance schedule if linked
        UUID linkedScheduleId = req.getParsedLinkedScheduleId();
        if (linkedScheduleId != null) {
            advanceScheduleIfApplicable(linkedScheduleId, existing.getServiceDate(), existing.getKmAtService(), Boolean.TRUE.equals(existing.getIsUpcoming()));
        }

        // Sync or update finance transaction if requested or linked
        if (Boolean.TRUE.equals(req.syncToFinance()) && existing.getLinkedTransactionId() == null && existing.getCost().compareTo(BigDecimal.ZERO) > 0) {
            String txId = UUID.randomUUID().toString();
            try {
                Vehicle vehicle = vehicleRepository.findById(existing.getVehicleId()).orElse(null);
                String vehicleName = vehicle != null && vehicle.getName() != null ? vehicle.getName() : "Vehicle";
                String desc = "Service: " + existing.getServiceType() + " (" + vehicleName + ")";
                if (desc.length() > 255) desc = desc.substring(0, 255);
                String txSql = """
                        INSERT INTO transactions (id, transaction_type, amount, transaction_at, description, linked_module, linked_entity_id, linked_entity_type)
                        VALUES (:txId::uuid, 'EXPENSE', :amount, CURRENT_TIMESTAMP, :desc, 'VEHICLES', :vId, 'SERVICE_RECORD')
                        """;
                jdbc.update(txSql, new MapSqlParameterSource()
                        .addValue("txId", txId)
                        .addValue("amount", existing.getCost())
                        .addValue("desc", desc)
                        .addValue("vId", existing.getVehicleId().toString()));
                existing.setLinkedTransactionId(txId);
            } catch (Exception ignored) {
            }
        } else if (existing.getLinkedTransactionId() != null && existing.getCost().compareTo(BigDecimal.ZERO) > 0) {
            try {
                String desc = "Service: " + existing.getServiceType();
                if (desc.length() > 255) desc = desc.substring(0, 255);
                String updateTxSql = "UPDATE transactions SET amount = :amount, description = :desc WHERE id = :txId::uuid";
                jdbc.update(updateTxSql, new MapSqlParameterSource()
                        .addValue("txId", existing.getLinkedTransactionId())
                        .addValue("amount", existing.getCost())
                        .addValue("desc", desc));
            } catch (Exception ignored) {
            }
        }

        VehicleServiceRecord updated = serviceRecordRepository.update(existing);
        return enrichWithItems(updated);
    }

    @Transactional
    public void deleteServiceRecord(UUID id) {
        serviceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleServiceRecord", id));
        serviceRecordRepository.deleteById(id);
    }

    private void recordOdometerIfApplicable(UUID vehicleId, Integer kmAtService, LocalDate serviceDate, String serviceType, boolean isUpcoming) {
        if (kmAtService == null || kmAtService <= 0 || isUpcoming || odometerService == null) {
            return;
        }

        try {
            int currentMaxKm = 0;
            if (odometerRepository != null) {
                Optional<VehicleOdometerReading> latestReading = odometerRepository.findLatestByVehicleId(vehicleId);
                if (latestReading.isPresent()) {
                    currentMaxKm = latestReading.get().getOdometerKm();
                } else if (vehicleRepository != null) {
                    Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
                    if (vehicle != null) {
                        currentMaxKm = vehicle.getCurrentKm();
                    }
                }
            } else if (vehicleRepository != null) {
                Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
                if (vehicle != null) {
                    currentMaxKm = vehicle.getCurrentKm();
                }
            }

            // Only record odometer if reading is non-decreasing (>= latest)
            if (kmAtService >= currentMaxKm) {
                odometerService.recordReading(vehicleId, new RecordOdometerRequest(
                        serviceDate != null ? serviceDate : LocalDate.now(),
                        kmAtService,
                        "SERVICE",
                        false,
                        "Recorded at service: " + (serviceType != null ? serviceType : "Service")
                ));
            }
        } catch (Exception ignored) {
            // Guard against any unexpected odometer error to prevent rolling back service record creation
        }
    }

    private void advanceScheduleIfApplicable(UUID scheduleId, LocalDate serviceDate, Integer kmAtService, boolean isUpcoming) {
        if (scheduleId == null || isUpcoming || scheduleService == null) {
            return;
        }
        try {
            scheduleService.advanceScheduleAfterService(scheduleId, serviceDate != null ? serviceDate : LocalDate.now(), kmAtService);
        } catch (Exception ignored) {
            // Guard against schedule advancement failure so service record creation is never rolled back
        }
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
