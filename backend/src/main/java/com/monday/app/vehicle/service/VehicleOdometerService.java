package com.monday.app.vehicle.service;

import com.monday.app.shared.exception.ResourceNotFoundException;
import com.monday.app.vehicle.dto.OdometerReadingDto;
import com.monday.app.vehicle.dto.RecordOdometerRequest;
import com.monday.app.vehicle.entity.Vehicle;
import com.monday.app.vehicle.entity.VehicleOdometerReading;
import com.monday.app.vehicle.repository.VehicleOdometerReadingRepository;
import com.monday.app.vehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VehicleOdometerService {

    private final VehicleOdometerReadingRepository odometerRepository;
    private final VehicleRepository vehicleRepository;

    public VehicleOdometerService(VehicleOdometerReadingRepository odometerRepository,
                                  VehicleRepository vehicleRepository) {
        this.odometerRepository = odometerRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public OdometerReadingDto recordReading(UUID vehicleId, RecordOdometerRequest req) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        if (req.odometerKm() == null || req.odometerKm() < 0) {
            throw new IllegalArgumentException("Odometer reading must be non-negative");
        }

        Optional<VehicleOdometerReading> latestOpt = odometerRepository.findLatestByVehicleId(vehicleId);

        boolean isCorrection = Boolean.TRUE.equals(req.isCorrection());
        if (latestOpt.isPresent() && !isCorrection) {
            VehicleOdometerReading latest = latestOpt.get();
            if (req.odometerKm() < latest.getOdometerKm()) {
                throw new IllegalArgumentException(
                        "Odometer reading (" + req.odometerKm() + " km) cannot decrease from latest recorded reading ("
                                + latest.getOdometerKm() + " km) unless explicitly submitted as a correction."
                );
            }
        }

        LocalDate readingDate = req.readingDate() != null ? req.readingDate() : LocalDate.now();
        String source = req.source() != null ? req.source() : "MANUAL";

        VehicleOdometerReading reading = new VehicleOdometerReading(
                null,
                vehicleId,
                readingDate,
                req.odometerKm(),
                source,
                isCorrection,
                req.notes(),
                null
        );

        VehicleOdometerReading saved = odometerRepository.save(reading);

        // Update vehicle currentKm if this reading is greater or if it's the latest
        if (req.odometerKm() > vehicle.getCurrentKm() || isCorrection) {
            vehicle.setCurrentKm(req.odometerKm());
            vehicleRepository.update(vehicle);
        }

        Integer delta = null;
        if (latestOpt.isPresent() && req.odometerKm() >= latestOpt.get().getOdometerKm()) {
            delta = req.odometerKm() - latestOpt.get().getOdometerKm();
        }

        return toDto(saved, delta);
    }

    public List<OdometerReadingDto> getReadings(UUID vehicleId) {
        List<VehicleOdometerReading> readings = odometerRepository.findByVehicleId(vehicleId);
        List<OdometerReadingDto> dtos = new ArrayList<>();

        for (int i = 0; i < readings.size(); i++) {
            VehicleOdometerReading current = readings.get(i);
            Integer delta = null;
            if (i < readings.size() - 1) {
                VehicleOdometerReading prev = readings.get(i + 1);
                delta = current.getOdometerKm() - prev.getOdometerKm();
            }
            dtos.add(toDto(current, delta));
        }

        return dtos;
    }

    public Optional<OdometerReadingDto> getLatestReading(UUID vehicleId) {
        return odometerRepository.findLatestByVehicleId(vehicleId).map(r -> toDto(r, null));
    }

    private OdometerReadingDto toDto(VehicleOdometerReading r, Integer delta) {
        return new OdometerReadingDto(
                r.getId(),
                r.getVehicleId(),
                r.getReadingDate(),
                r.getOdometerKm(),
                r.getSource(),
                r.getIsCorrection(),
                r.getNotes(),
                delta,
                r.getCreatedAt()
        );
    }
}
