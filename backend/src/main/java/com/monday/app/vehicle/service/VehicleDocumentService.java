package com.monday.app.vehicle.service;

import com.monday.app.shared.exception.ResourceNotFoundException;
import com.monday.app.vehicle.dto.CreateDocumentRequest;
import com.monday.app.vehicle.dto.RenewDocumentRequest;
import com.monday.app.vehicle.dto.UpdateDocumentRequest;
import com.monday.app.vehicle.dto.VehicleDocumentDto;
import com.monday.app.vehicle.entity.Vehicle;
import com.monday.app.vehicle.entity.VehicleDocument;
import com.monday.app.vehicle.entity.VehicleInsurance;
import com.monday.app.vehicle.repository.VehicleDocumentRepository;
import com.monday.app.vehicle.repository.VehicleInsuranceRepository;
import com.monday.app.vehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleDocumentService {

    private final VehicleDocumentRepository documentRepository;
    private final VehicleInsuranceRepository insuranceRepository;
    private final VehicleRepository vehicleRepository;

    public VehicleDocumentService(VehicleDocumentRepository documentRepository,
                                  VehicleInsuranceRepository insuranceRepository,
                                  VehicleRepository vehicleRepository) {
        this.documentRepository = documentRepository;
        this.insuranceRepository = insuranceRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public List<VehicleDocumentDto> getDocuments(UUID vehicleId, String typeFilter, String statusFilter, String search) {
        vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        List<VehicleDocument> docs = documentRepository.findByVehicleId(vehicleId);
        LocalDate today = LocalDate.now();

        return docs.stream()
                .map(doc -> toDto(doc, today))
                .filter(dto -> {
                    if (typeFilter != null && !typeFilter.isBlank() && !"ALL".equalsIgnoreCase(typeFilter)) {
                        if (!dto.docType().equalsIgnoreCase(typeFilter)) return false;
                    }
                    if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
                        if (!dto.effectiveStatus().equalsIgnoreCase(statusFilter)) return false;
                    }
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean match = (dto.title() != null && dto.title().toLowerCase().contains(s)) ||
                                        (dto.identifier() != null && dto.identifier().toLowerCase().contains(s)) ||
                                        (dto.issuer() != null && dto.issuer().toLowerCase().contains(s)) ||
                                        (dto.underwriter() != null && dto.underwriter().toLowerCase().contains(s)) ||
                                        (dto.policyNumber() != null && dto.policyNumber().toLowerCase().contains(s));
                        if (!match) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    public VehicleDocumentDto getDocument(UUID docId) {
        VehicleDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleDocument", docId));
        return toDto(doc, LocalDate.now());
    }

    @Transactional
    public VehicleDocumentDto createDocument(UUID vehicleId, CreateDocumentRequest req) {
        vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        String docType = req.docType() != null ? req.docType().trim().toUpperCase() : "CUSTOM";
        LocalDate expiryDate = req.expiryDate();
        String validUntil = req.validUntil();
        if (expiryDate != null && (validUntil == null || validUntil.isBlank())) {
            validUntil = expiryDate.toString();
        }

        VehicleDocument doc = new VehicleDocument(
                null,
                vehicleId,
                docType,
                req.title() != null ? req.title().trim() : (docType + " Document"),
                req.identifier() != null ? req.identifier().trim() : null,
                req.issuer() != null ? req.issuer().trim() : null,
                validUntil,
                req.issueDate(),
                req.validFrom(),
                expiryDate,
                req.verificationSource() != null ? req.verificationSource() : "Self-uploaded",
                Boolean.TRUE.equals(req.isVerified()),
                req.status() != null ? req.status() : "VALID",
                null,
                req.notes(),
                req.metadata(),
                null,
                null
        );

        VehicleDocument savedDoc = documentRepository.save(doc);

        // If insurance type or insurance details provided, link/create insurance record
        if ("INSURANCE".equalsIgnoreCase(docType) || req.underwriter() != null || req.policyNumber() != null) {
            VehicleInsurance insurance = new VehicleInsurance(
                    null,
                    vehicleId,
                    savedDoc.getId(),
                    req.policyName() != null ? req.policyName() : savedDoc.getTitle(),
                    req.underwriter() != null ? req.underwriter() : savedDoc.getIssuer(),
                    req.policyNumber() != null ? req.policyNumber() : savedDoc.getIdentifier(),
                    req.coverageScope() != null ? req.coverageScope() : "Comprehensive Cover",
                    req.insuredDeclaredValue(),
                    req.premiumDue(),
                    expiryDate != null ? expiryDate : req.insuranceDueDate(),
                    "ACTIVE",
                    null,
                    false,
                    null,
                    null
            );
            insuranceRepository.save(insurance);
        }

        return toDto(savedDoc, LocalDate.now());
    }

    @Transactional
    public VehicleDocumentDto updateDocument(UUID docId, UpdateDocumentRequest req) {
        VehicleDocument existing = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleDocument", docId));

        if (req.docType() != null) existing.setDocType(req.docType().trim().toUpperCase());
        if (req.title() != null) existing.setTitle(req.title().trim());
        if (req.identifier() != null) existing.setIdentifier(req.identifier().trim());
        if (req.issuer() != null) existing.setIssuer(req.issuer().trim());
        if (req.issueDate() != null) existing.setIssueDate(req.issueDate());
        if (req.validFrom() != null) existing.setValidFrom(req.validFrom());
        if (req.expiryDate() != null) {
            existing.setExpiryDate(req.expiryDate());
            existing.setValidUntil(req.expiryDate().toString());
        } else if (req.validUntil() != null) {
            existing.setValidUntil(req.validUntil());
        }
        if (req.verificationSource() != null) existing.setVerificationSource(req.verificationSource());
        if (req.isVerified() != null) existing.setIsVerified(req.isVerified());
        if (req.status() != null) existing.setStatus(req.status());
        if (req.notes() != null) existing.setNotes(req.notes());
        if (req.metadata() != null) existing.setMetadata(req.metadata());

        VehicleDocument updated = documentRepository.update(existing);

        // Update linked insurance if exists
        Optional<VehicleInsurance> insOpt = insuranceRepository.findByDocumentId(docId);
        if (insOpt.isPresent()) {
            VehicleInsurance ins = insOpt.get();
            if (req.policyName() != null) ins.setPolicyName(req.policyName());
            if (req.underwriter() != null) ins.setUnderwriter(req.underwriter());
            if (req.policyNumber() != null) ins.setPolicyNumber(req.policyNumber());
            if (req.coverageScope() != null) ins.setCoverageScope(req.coverageScope());
            if (req.insuredDeclaredValue() != null) ins.setInsuredDeclaredValue(req.insuredDeclaredValue());
            if (req.premiumDue() != null) ins.setPremiumDue(req.premiumDue());
            if (req.expiryDate() != null) ins.setDueDate(req.expiryDate());
            if (req.isRenewed() != null) ins.setIsRenewed(req.isRenewed());
            insuranceRepository.update(ins);
        }

        return toDto(updated, LocalDate.now());
    }

    @Transactional
    public VehicleDocumentDto renewDocument(UUID docId, RenewDocumentRequest req) {
        VehicleDocument existing = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleDocument", docId));

        LocalDate newExpiry = req.newExpiryDate();
        if (newExpiry != null) {
            existing.setExpiryDate(newExpiry);
            existing.setValidUntil(newExpiry.toString());
        } else if (req.newValidUntil() != null) {
            existing.setValidUntil(req.newValidUntil());
        }

        existing.setStatus("VALID");
        existing.setIsVerified(true);
        if (req.notes() != null && !req.notes().isBlank()) {
            String note = existing.getNotes() != null ? existing.getNotes() + "\n" + req.notes() : req.notes();
            existing.setNotes(note);
        }

        VehicleDocument updated = documentRepository.update(existing);

        // Also update linked insurance policy
        Optional<VehicleInsurance> insOpt = insuranceRepository.findByDocumentId(docId);
        if (insOpt.isPresent()) {
            VehicleInsurance ins = insOpt.get();
            ins.setIsRenewed(true);
            ins.setStatus("ACTIVE");
            if (newExpiry != null) ins.setDueDate(newExpiry);
            if (req.newPremiumDue() != null) ins.setPremiumDue(req.newPremiumDue());
            if (req.newInsuredDeclaredValue() != null) ins.setInsuredDeclaredValue(req.newInsuredDeclaredValue());
            if (req.newPolicyNumber() != null && !req.newPolicyNumber().isBlank()) ins.setPolicyNumber(req.newPolicyNumber());
            insuranceRepository.update(ins);
        }

        return toDto(updated, LocalDate.now());
    }

    @Transactional
    public void deleteDocument(UUID docId) {
        documentRepository.deleteById(docId);
    }

    private VehicleDocumentDto toDto(VehicleDocument doc, LocalDate today) {
        LocalDate expiry = doc.getExpiryDate();
        Long daysRemaining = null;
        boolean isExpiringSoon = false;
        boolean isExpired = false;
        boolean isIndefinite = false;
        String effectiveStatus = "VALID";

        if (expiry == null) {
            isIndefinite = true;
            effectiveStatus = "INDEFINITE";
        } else {
            daysRemaining = ChronoUnit.DAYS.between(today, expiry);
            if (daysRemaining < 0) {
                isExpired = true;
                effectiveStatus = "EXPIRED";
            } else if (daysRemaining <= 30) {
                isExpiringSoon = true;
                effectiveStatus = "EXPIRING_SOON";
            } else {
                effectiveStatus = "VALID";
            }
        }

        // Check if explicitly archived
        if ("ARCHIVED".equalsIgnoreCase(doc.getStatus())) {
            effectiveStatus = "ARCHIVED";
        }

        // Link insurance details if document is INSURANCE or linked
        Optional<VehicleInsurance> insOpt = insuranceRepository.findByDocumentId(doc.getId());
        if (insOpt.isEmpty() && "INSURANCE".equalsIgnoreCase(doc.getDocType())) {
            insOpt = insuranceRepository.findLatestActiveByVehicleId(doc.getVehicleId());
        }

        UUID insId = null;
        String policyName = null;
        String underwriter = null;
        String policyNumber = null;
        String coverageScope = null;
        java.math.BigDecimal idv = null;
        java.math.BigDecimal premiumDue = null;
        LocalDate dueDate = null;
        Boolean isRenewed = null;

        if (insOpt.isPresent()) {
            VehicleInsurance ins = insOpt.get();
            insId = ins.getId();
            policyName = ins.getPolicyName();
            underwriter = ins.getUnderwriter();
            policyNumber = ins.getPolicyNumber();
            coverageScope = ins.getCoverageScope();
            idv = ins.getInsuredDeclaredValue();
            premiumDue = ins.getPremiumDue();
            dueDate = ins.getDueDate();
            isRenewed = ins.getIsRenewed();
        }

        return new VehicleDocumentDto(
                doc.getId(),
                doc.getVehicleId(),
                doc.getDocType(),
                doc.getTitle(),
                doc.getIdentifier(),
                doc.getIssuer(),
                doc.getValidUntil(),
                doc.getIssueDate(),
                doc.getValidFrom(),
                doc.getExpiryDate(),
                doc.getVerificationSource(),
                doc.getIsVerified(),
                doc.getStatus(),
                doc.getCoveredComponents(),
                doc.getNotes(),
                doc.getMetadata(),
                daysRemaining,
                isExpiringSoon,
                isExpired,
                isIndefinite,
                effectiveStatus,
                insId,
                policyName,
                underwriter,
                policyNumber,
                coverageScope,
                idv,
                premiumDue,
                dueDate,
                isRenewed,
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
