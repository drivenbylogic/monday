package com.monday.app.vehicle.service;

import com.monday.app.shared.cache.CacheNames;
import com.monday.app.shared.exception.ResourceNotFoundException;
import com.monday.app.vehicle.dto.*;
import com.monday.app.vehicle.entity.*;
import com.monday.app.vehicle.repository.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class VehicleAnalyticsService {

    private final VehicleRepository vehicleRepository;
    private final VehicleOdometerReadingRepository odometerRepository;
    private final VehicleFuelLogRepository fuelLogRepository;
    private final VehicleServiceRecordRepository serviceRecordRepository;
    private final VehicleInsuranceRepository insuranceRepository;
    private final VehicleObligationRepository obligationRepository;
    private final VehicleMaintenanceScheduleRepository scheduleRepository;

    public VehicleAnalyticsService(VehicleRepository vehicleRepository,
                                   VehicleOdometerReadingRepository odometerRepository,
                                   VehicleFuelLogRepository fuelLogRepository,
                                   VehicleServiceRecordRepository serviceRecordRepository,
                                   VehicleInsuranceRepository insuranceRepository,
                                   VehicleObligationRepository obligationRepository,
                                   VehicleMaintenanceScheduleRepository scheduleRepository) {
        this.vehicleRepository = vehicleRepository;
        this.odometerRepository = odometerRepository;
        this.fuelLogRepository = fuelLogRepository;
        this.serviceRecordRepository = serviceRecordRepository;
        this.insuranceRepository = insuranceRepository;
        this.obligationRepository = obligationRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Cacheable(value = CacheNames.VEHICLE_ANALYTICS, keyGenerator = "userAwareKeyGenerator", sync = true)
    public VehicleAnalyticsDto getAnalytics(UUID vehicleId, String period) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        LocalDate today = LocalDate.now();
        LocalDate startDate = resolveStartDate(today, period);

        // 1. Usage & Odometer
        List<VehicleOdometerReading> allOdo = odometerRepository.findByVehicleId(vehicleId);
        UsageSummaryDto usage = calculateUsage(vehicle, allOdo, startDate, today);

        // 2. Fuel Efficiency
        List<VehicleFuelLog> fuelLogs = fuelLogRepository.findByVehicleId(vehicleId);
        FuelEfficiencySummaryDto fuelEfficiency = calculateFuelEfficiency(fuelLogs, startDate);

        // 3. Ownership Costs
        List<VehicleServiceRecord> serviceRecords = serviceRecordRepository.findByVehicleId(vehicleId);
        List<VehicleInsurance> insurances = insuranceRepository.findByVehicleId(vehicleId);
        List<VehicleObligation> obligations = obligationRepository.findByVehicleId(vehicleId);
        CostSummaryDto costs = calculateCosts(vehicle, fuelLogs, serviceRecords, insurances, obligations, startDate, today, period);

        // Recent odometer reading dtos
        List<OdometerReadingDto> recentOdoDtos = allOdo.stream().limit(10).map(r -> new OdometerReadingDto(
                r.getId(), r.getVehicleId(), r.getReadingDate(), r.getOdometerKm(),
                r.getSource(), r.getIsCorrection(), r.getNotes(), null, r.getCreatedAt()
        )).toList();

        return new VehicleAnalyticsDto(usage, fuelEfficiency, costs, recentOdoDtos);
    }

    private UsageSummaryDto calculateUsage(Vehicle vehicle, List<VehicleOdometerReading> readings, LocalDate startDate, LocalDate today) {
        int currentKm = vehicle.getCurrentKm();
        LocalDate latestDate = today;

        if (!readings.isEmpty()) {
            VehicleOdometerReading latest = readings.get(0);
            latestDate = latest.getReadingDate();
            if (latest.getOdometerKm() > currentKm) {
                currentKm = latest.getOdometerKm();
            }
        }

        // Distance covered in period
        int distancePeriod = 0;
        int distance30Days = 0;
        int distance12Months = 0;

        LocalDate thirtyDaysAgo = today.minusDays(30);
        LocalDate twelveMonthsAgo = today.minusMonths(12);

        if (readings.size() >= 2) {
            // Find reading closest to and before or on startDate
            int minKmPeriod = findBaselineKm(readings, startDate);
            if (minKmPeriod > 0 && currentKm > minKmPeriod) {
                distancePeriod = currentKm - minKmPeriod;
            }

            int minKm30 = findBaselineKm(readings, thirtyDaysAgo);
            if (minKm30 > 0 && currentKm > minKm30) {
                distance30Days = currentKm - minKm30;
            }

            int minKm12M = findBaselineKm(readings, twelveMonthsAgo);
            if (minKm12M > 0 && currentKm > minKm12M) {
                distance12Months = currentKm - minKm12M;
            }
        }

        // Next maintenance threshold
        List<VehicleMaintenanceSchedule> schedules = scheduleRepository.findByVehicleId(vehicle.getId());
        Integer minRemainingKm = null;
        String nextItem = null;

        for (VehicleMaintenanceSchedule s : schedules) {
            if (s.getNextDueKm() != null) {
                int rem = s.getNextDueKm() - currentKm;
                if (minRemainingKm == null || rem < minRemainingKm) {
                    minRemainingKm = rem;
                    nextItem = s.getItemName();
                }
            }
        }

        return new UsageSummaryDto(
                currentKm,
                latestDate,
                Math.max(0, distancePeriod),
                Math.max(0, distance30Days),
                Math.max(0, distance12Months),
                readings.size(),
                minRemainingKm != null ? Math.max(0, minRemainingKm) : null,
                nextItem
        );
    }

    private int findBaselineKm(List<VehicleOdometerReading> sortedReadings, LocalDate thresholdDate) {
        for (VehicleOdometerReading r : sortedReadings) {
            if (!r.getReadingDate().isAfter(thresholdDate)) {
                return r.getOdometerKm();
            }
        }
        return sortedReadings.get(sortedReadings.size() - 1).getOdometerKm();
    }

    private FuelEfficiencySummaryDto calculateFuelEfficiency(List<VehicleFuelLog> fuelLogs, LocalDate startDate) {
        if (fuelLogs.isEmpty()) {
            return new FuelEfficiencySummaryDto(
                    null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    0, 0, null, false, "No fuel fill-up logs recorded yet"
            );
        }

        List<VehicleFuelLog> filtered = fuelLogs.stream()
                .filter(l -> !l.getLogDate().isBefore(startDate))
                .toList();

        BigDecimal totalVolume = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        int fullTankCount = 0;
        int validFillUps = 0;
        double efficiencySum = 0;
        int efficiencyCount = 0;
        Double latestEfficiency = null;

        for (int i = 0; i < filtered.size(); i++) {
            VehicleFuelLog current = filtered.get(i);
            if (current.getVolumeLiters() != null) totalVolume = totalVolume.add(current.getVolumeLiters());
            if (current.getTotalCost() != null) totalCost = totalCost.add(current.getTotalCost());
            if (Boolean.TRUE.equals(current.getFullTank())) fullTankCount++;
            validFillUps++;

            if (current.getTripEfficiency() != null && current.getTripEfficiency().doubleValue() > 0) {
                if (latestEfficiency == null) {
                    latestEfficiency = current.getTripEfficiency().doubleValue();
                }
                efficiencySum += current.getTripEfficiency().doubleValue();
                efficiencyCount++;
            }
        }

        // If no precalculated trip efficiency, try computing between full tanks
        if (efficiencyCount == 0 && filtered.size() >= 2) {
            for (int i = 0; i < filtered.size() - 1; i++) {
                VehicleFuelLog curr = filtered.get(i);
                VehicleFuelLog prev = filtered.get(i + 1);
                if (curr.getOdometerKm() != null && prev.getOdometerKm() != null &&
                        curr.getOdometerKm() > prev.getOdometerKm() &&
                        curr.getVolumeLiters() != null && curr.getVolumeLiters().compareTo(BigDecimal.ZERO) > 0) {
                    int dist = curr.getOdometerKm() - prev.getOdometerKm();
                    double eff = dist / curr.getVolumeLiters().doubleValue();
                    if (eff > 2 && eff < 100) {
                        if (latestEfficiency == null) latestEfficiency = eff;
                        efficiencySum += eff;
                        efficiencyCount++;
                    }
                }
            }
        }

        Double avgEfficiency = efficiencyCount > 0
                ? BigDecimal.valueOf(efficiencySum / efficiencyCount).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : null;

        BigDecimal avgPrice = (totalVolume.compareTo(BigDecimal.ZERO) > 0 && totalCost.compareTo(BigDecimal.ZERO) > 0)
                ? totalCost.divide(totalVolume, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal costPerKm = null;
        if (avgEfficiency != null && avgEfficiency > 0 && avgPrice.compareTo(BigDecimal.ZERO) > 0) {
            costPerKm = avgPrice.divide(BigDecimal.valueOf(avgEfficiency), 2, RoundingMode.HALF_UP);
        }

        boolean hasSufficient = validFillUps >= 2 && efficiencyCount > 0;
        String note = hasSufficient
                ? "Calculated from validated fill-ups"
                : (validFillUps == 0 ? "No fuel logs in selected period" : "At least two fill-ups with odometer readings are needed for full-tank efficiency calculations");

        return new FuelEfficiencySummaryDto(
                latestEfficiency,
                avgEfficiency,
                totalVolume.setScale(2, RoundingMode.HALF_UP),
                totalCost.setScale(2, RoundingMode.HALF_UP),
                avgPrice,
                validFillUps,
                fullTankCount,
                costPerKm,
                hasSufficient,
                note
        );
    }

    private CostSummaryDto calculateCosts(Vehicle vehicle,
                                          List<VehicleFuelLog> fuelLogs,
                                          List<VehicleServiceRecord> serviceRecords,
                                          List<VehicleInsurance> insurances,
                                          List<VehicleObligation> obligations,
                                          LocalDate startDate,
                                          LocalDate today,
                                          String period) {

        BigDecimal periodFuel = BigDecimal.ZERO;
        BigDecimal cumulativeFuel = BigDecimal.ZERO;
        for (VehicleFuelLog f : fuelLogs) {
            BigDecimal c = f.getTotalCost() != null ? f.getTotalCost() : BigDecimal.ZERO;
            cumulativeFuel = cumulativeFuel.add(c);
            if (!f.getLogDate().isBefore(startDate)) {
                periodFuel = periodFuel.add(c);
            }
        }

        BigDecimal periodService = BigDecimal.ZERO;
        BigDecimal cumulativeService = BigDecimal.ZERO;
        BigDecimal periodRepairs = BigDecimal.ZERO;
        for (VehicleServiceRecord s : serviceRecords) {
            BigDecimal c = s.getCost() != null ? s.getCost() : BigDecimal.ZERO;
            cumulativeService = cumulativeService.add(c);
            if (!s.getServiceDate().isBefore(startDate)) {
                if (s.getServiceType() != null && s.getServiceType().toLowerCase().contains("repair")) {
                    periodRepairs = periodRepairs.add(c);
                } else {
                    periodService = periodService.add(c);
                }
            }
        }

        BigDecimal periodInsurance = BigDecimal.ZERO;
        BigDecimal cumulativeInsurance = BigDecimal.ZERO;
        for (VehicleInsurance ins : insurances) {
            BigDecimal p = ins.getPremiumDue() != null ? ins.getPremiumDue() : BigDecimal.ZERO;
            cumulativeInsurance = cumulativeInsurance.add(p);
            if (ins.getDueDate() != null && !ins.getDueDate().isBefore(startDate)) {
                periodInsurance = periodInsurance.add(p);
            }
        }

        BigDecimal periodObligations = BigDecimal.ZERO;
        BigDecimal cumulativeObligations = BigDecimal.ZERO;
        for (VehicleObligation o : obligations) {
            BigDecimal a = o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO;
            cumulativeObligations = cumulativeObligations.add(a);
            if (o.getDueDate() != null && !o.getDueDate().isBefore(startDate)) {
                periodObligations = periodObligations.add(a);
            }
        }

        BigDecimal totalPeriod = periodFuel.add(periodService).add(periodRepairs).add(periodInsurance).add(periodObligations);
        BigDecimal totalCumulative = cumulativeFuel.add(cumulativeService).add(cumulativeInsurance).add(cumulativeObligations);
        BigDecimal purchaseAmount = vehicle.getPurchaseAmount() != null ? vehicle.getPurchaseAmount() : BigDecimal.ZERO;
        BigDecimal totalOwnership = purchaseAmount.add(totalCumulative);

        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        breakdown.put("fuel", periodFuel);
        breakdown.put("servicing", periodService);
        breakdown.put("repairs", periodRepairs);
        breakdown.put("insurance", periodInsurance);
        breakdown.put("obligations", periodObligations);

        // Monthly trends for past 6 months
        List<MonthlyCostTrendDto> trends = calculateMonthlyTrends(fuelLogs, serviceRecords, insurances, obligations, today);

        // Cost per km in period
        BigDecimal costPerKm = null;
        if (vehicle.getCurrentKm() > 0 && totalCumulative.compareTo(BigDecimal.ZERO) > 0) {
            costPerKm = totalCumulative.divide(BigDecimal.valueOf(vehicle.getCurrentKm()), 2, RoundingMode.HALF_UP);
        }

        return new CostSummaryDto(
                totalPeriod.setScale(2, RoundingMode.HALF_UP),
                totalCumulative.setScale(2, RoundingMode.HALF_UP),
                purchaseAmount.setScale(2, RoundingMode.HALF_UP),
                totalOwnership.setScale(2, RoundingMode.HALF_UP),
                costPerKm,
                breakdown,
                trends,
                period != null ? period : "ALL"
        );
    }

    private List<MonthlyCostTrendDto> calculateMonthlyTrends(List<VehicleFuelLog> fuelLogs,
                                                             List<VehicleServiceRecord> serviceRecords,
                                                             List<VehicleInsurance> insurances,
                                                             List<VehicleObligation> obligations,
                                                             LocalDate today) {
        List<MonthlyCostTrendDto> list = new ArrayList<>();
        DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 5; i >= 0; i--) {
            LocalDate m = today.minusMonths(i);
            String ym = m.format(ymFmt);

            BigDecimal fCost = fuelLogs.stream()
                    .filter(l -> l.getLogDate().format(ymFmt).equals(ym))
                    .map(l -> l.getTotalCost() != null ? l.getTotalCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal sCost = serviceRecords.stream()
                    .filter(l -> l.getServiceDate().format(ymFmt).equals(ym))
                    .map(l -> l.getCost() != null ? l.getCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal iCost = insurances.stream()
                    .filter(l -> l.getDueDate() != null && l.getDueDate().format(ymFmt).equals(ym))
                    .map(l -> l.getPremiumDue() != null ? l.getPremiumDue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal oCost = obligations.stream()
                    .filter(l -> l.getDueDate() != null && l.getDueDate().format(ymFmt).equals(ym))
                    .map(l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal total = fCost.add(sCost).add(iCost).add(oCost);
            list.add(new MonthlyCostTrendDto(ym, fCost, sCost, iCost, oCost, total));
        }

        return list;
    }

    private LocalDate resolveStartDate(LocalDate today, String period) {
        if (period == null) return today.minusYears(10);
        return switch (period.toUpperCase()) {
            case "1M" -> today.minusMonths(1);
            case "3M" -> today.minusMonths(3);
            case "6M" -> today.minusMonths(6);
            case "1Y" -> today.minusYears(1);
            case "YTD" -> LocalDate.of(today.getYear(), 1, 1);
            default -> today.minusYears(10);
        };
    }
}
