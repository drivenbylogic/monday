package com.monday.app.vehicle.service;

import com.monday.app.shared.exception.ResourceNotFoundException;
import com.monday.app.vehicle.dto.VehicleTimelineItemDto;
import com.monday.app.vehicle.entity.*;
import com.monday.app.vehicle.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleTimelineService {

    private final VehicleRepository vehicleRepository;
    private final VehicleServiceRecordRepository serviceRecordRepository;
    private final VehicleFuelLogRepository fuelLogRepository;
    private final VehicleOdometerReadingRepository odometerRepository;
    private final VehicleDocumentRepository documentRepository;
    private final VehicleInsuranceRepository insuranceRepository;
    private final VehicleTaskRepository taskRepository;
    private final VehicleObligationRepository obligationRepository;

    public VehicleTimelineService(VehicleRepository vehicleRepository,
                                  VehicleServiceRecordRepository serviceRecordRepository,
                                  VehicleFuelLogRepository fuelLogRepository,
                                  VehicleOdometerReadingRepository odometerRepository,
                                  VehicleDocumentRepository documentRepository,
                                  VehicleInsuranceRepository insuranceRepository,
                                  VehicleTaskRepository taskRepository,
                                  VehicleObligationRepository obligationRepository) {
        this.vehicleRepository = vehicleRepository;
        this.serviceRecordRepository = serviceRecordRepository;
        this.fuelLogRepository = fuelLogRepository;
        this.odometerRepository = odometerRepository;
        this.documentRepository = documentRepository;
        this.insuranceRepository = insuranceRepository;
        this.taskRepository = taskRepository;
        this.obligationRepository = obligationRepository;
    }

    public List<VehicleTimelineItemDto> getTimeline(UUID vehicleId, int limit) {
        vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        List<VehicleTimelineItemDto> items = new ArrayList<>();

        // 1. Service Records
        List<VehicleServiceRecord> services = serviceRecordRepository.findByVehicleId(vehicleId);
        for (VehicleServiceRecord s : services) {
            if (!Boolean.TRUE.equals(s.getIsUpcoming())) {
                String desc = s.getDescription() != null ? s.getDescription() : (s.getPerformedBy() != null ? "Performed by " + s.getPerformedBy() : "Service Completed");
                items.add(new VehicleTimelineItemDto(
                        "srv-" + s.getId(),
                        "SERVICE",
                        s.getServiceType() != null ? s.getServiceType() : "Scheduled Service",
                        desc,
                        s.getServiceDate(),
                        s.getKmAtService(),
                        s.getCost(),
                        s.getId().toString(),
                        "Service Visit",
                        "build"
                ));
            }
        }

        // 2. Fuel Logs
        List<VehicleFuelLog> fuelLogs = fuelLogRepository.findByVehicleId(vehicleId);
        for (VehicleFuelLog f : fuelLogs) {
            String brand = f.getFuelBrand() != null ? f.getFuelBrand() : (f.getBrand() != null ? f.getBrand() : "Fuel");
            String desc = (f.getVolumeLiters() != null ? f.getVolumeLiters() + " L" : "") +
                    (f.getPricePerLiter() != null ? " @ ₹" + f.getPricePerLiter() + "/L" : "") +
                    (f.getStationLocation() != null ? " • " + f.getStationLocation() : "");
            items.add(new VehicleTimelineItemDto(
                    "fuel-" + f.getId(),
                    "FUEL",
                    "Fuel Fill-up: " + brand,
                    desc.trim(),
                    f.getLogDate(),
                    f.getOdometerKm(),
                    f.getTotalCost(),
                    f.getId().toString(),
                    "Refueling",
                    "local_gas_station"
            ));
        }

        // 3. Odometer manual/telemetry updates (excluding service and fuel duplicates)
        List<VehicleOdometerReading> odoReadings = odometerRepository.findByVehicleId(vehicleId);
        for (VehicleOdometerReading o : odoReadings) {
            if ("MANUAL".equalsIgnoreCase(o.getSource()) || "TELEMETRY".equalsIgnoreCase(o.getSource())) {
                items.add(new VehicleTimelineItemDto(
                        "odo-" + o.getId(),
                        "ODOMETER",
                        "Odometer Reading: " + o.getOdometerKm() + " km",
                        o.getNotes() != null ? o.getNotes() : (o.getSource() + " odometer reading verification"),
                        o.getReadingDate(),
                        o.getOdometerKm(),
                        null,
                        o.getId().toString(),
                        "Usage Log",
                        "speed"
                ));
            }
        }

        // 4. Documents Issued / Validated
        List<VehicleDocument> docs = documentRepository.findByVehicleId(vehicleId);
        for (VehicleDocument d : docs) {
            if (!"INSURANCE".equalsIgnoreCase(d.getDocType())) {
                LocalDate dDate = d.getIssueDate() != null
                        ? d.getIssueDate()
                        : (d.getCreatedAt() != null ? d.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate() : LocalDate.now());
                items.add(new VehicleTimelineItemDto(
                        "doc-" + d.getId(),
                        "DOCUMENT",
                        d.getTitle(),
                        (d.getIssuer() != null ? "Issuer: " + d.getIssuer() : "Document verified") +
                                (d.getIdentifier() != null ? " • Ref: " + d.getIdentifier() : ""),
                        dDate,
                        null,
                        null,
                        d.getId().toString(),
                        d.getDocType() != null ? d.getDocType() : "Document",
                        "description"
                ));
            }
        }

        // 5. Insurance Policies
        List<VehicleInsurance> insurances = insuranceRepository.findByVehicleId(vehicleId);
        for (VehicleInsurance ins : insurances) {
            LocalDate insDate = ins.getCreatedAt() != null
                    ? ins.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                    : LocalDate.now();
            items.add(new VehicleTimelineItemDto(
                    "ins-" + ins.getId(),
                    "INSURANCE",
                    ins.getPolicyName() != null ? ins.getPolicyName() : "Insurance Policy Active",
                    (ins.getUnderwriter() != null ? ins.getUnderwriter() + " " : "") +
                            (ins.getPolicyNumber() != null ? "• Pol #" + ins.getPolicyNumber() : ""),
                    insDate,
                    null,
                    ins.getPremiumDue(),
                    ins.getId().toString(),
                    "Insurance",
                    "verified_user"
            ));
        }

        // 6. Completed Tasks
        List<VehicleTask> tasks = taskRepository.findByVehicleId(vehicleId);
        for (VehicleTask t : tasks) {
            if (Boolean.TRUE.equals(t.getCompleted()) && t.getCompletedAt() != null) {
                LocalDate cDate = t.getCompletedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                items.add(new VehicleTimelineItemDto(
                        "task-" + t.getId(),
                        "TASK",
                        "Task Completed: " + t.getTitle(),
                        t.getMetaText() != null ? t.getMetaText() : "Resolved task",
                        cDate,
                        null,
                        null,
                        t.getId().toString(),
                        "Task Done",
                        "check_circle"
                ));
            }
        }

        // 7. Obligations
        List<VehicleObligation> obligations = obligationRepository.findByVehicleId(vehicleId);
        for (VehicleObligation o : obligations) {
            if ("COMPLETED".equalsIgnoreCase(o.getStatus()) || "PAID".equalsIgnoreCase(o.getStatus())) {
                items.add(new VehicleTimelineItemDto(
                        "obl-" + o.getId(),
                        "OBLIGATION",
                        "Obligation Settled: " + o.getObligationType(),
                        o.getNotes() != null ? o.getNotes() : "Paid obligation",
                        o.getDueDate(),
                        null,
                        o.getAmount(),
                        o.getId().toString(),
                        "Settled",
                        "receipt_long"
                ));
            }
        }

        // Sort descending by eventDate, then by title
        items.sort(Comparator.comparing(VehicleTimelineItemDto::eventDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(VehicleTimelineItemDto::id, Comparator.reverseOrder()));

        return items.stream()
                .limit(limit > 0 ? limit : 50)
                .collect(Collectors.toList());
    }
}
