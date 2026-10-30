package com.monday.app.vehicle.service;

import com.monday.app.vehicle.dto.CreateDocumentRequest;
import com.monday.app.vehicle.dto.RenewDocumentRequest;
import com.monday.app.vehicle.dto.VehicleDocumentDto;
import com.monday.app.vehicle.entity.Vehicle;
import com.monday.app.vehicle.entity.VehicleDocument;
import com.monday.app.vehicle.entity.VehicleInsurance;
import com.monday.app.vehicle.repository.VehicleDocumentRepository;
import com.monday.app.vehicle.repository.VehicleInsuranceRepository;
import com.monday.app.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleDocumentServiceTest {

    private VehicleDocumentRepository documentRepository;
    private VehicleInsuranceRepository insuranceRepository;
    private VehicleRepository vehicleRepository;
    private VehicleDocumentService documentService;

    private UUID vehicleId;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        documentRepository = mock(VehicleDocumentRepository.class);
        insuranceRepository = mock(VehicleInsuranceRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        documentService = new VehicleDocumentService(documentRepository, insuranceRepository, vehicleRepository);

        vehicleId = UUID.randomUUID();
        vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
    }

    @Test
    void getDocuments_computesEffectiveStatusesCorrectly() {
        LocalDate today = LocalDate.now();

        // 1. Expired document
        VehicleDocument d1 = new VehicleDocument(UUID.randomUUID(), vehicleId, "PUC", "Pollution Certificate", "PUC-1", "Govt", "past", null, null, today.minusDays(5), null, true, "ACTIVE", null, null, null, null, null);
        // 2. Expiring soon document (within 30 days)
        VehicleDocument d2 = new VehicleDocument(UUID.randomUUID(), vehicleId, "INSURANCE", "Insurance Policy", "POL-1", "Allianz", "soon", null, null, today.plusDays(10), null, true, "ACTIVE", null, null, null, null, null);
        // 3. Valid document (>30 days)
        VehicleDocument d3 = new VehicleDocument(UUID.randomUUID(), vehicleId, "REGISTRATION", "Registration Certificate", "RC-1", "DMV", "future", null, null, today.plusDays(180), null, true, "ACTIVE", null, null, null, null, null);
        // 4. Indefinite document (null expiry)
        VehicleDocument d4 = new VehicleDocument(UUID.randomUUID(), vehicleId, "TITLE", "Vehicle Title", "TIT-1", "DMV", "Permanent", null, null, null, null, true, "ACTIVE", null, null, null, null, null);

        when(documentRepository.findByVehicleId(vehicleId)).thenReturn(List.of(d1, d2, d3, d4));

        List<VehicleDocumentDto> result = documentService.getDocuments(vehicleId, "ALL", "ALL", null);

        assertEquals(4, result.size());
        assertEquals("EXPIRED", result.get(0).effectiveStatus());
        assertTrue(result.get(0).isExpired());

        assertEquals("EXPIRING_SOON", result.get(1).effectiveStatus());
        assertTrue(result.get(1).isExpiringSoon());

        assertEquals("VALID", result.get(2).effectiveStatus());
        assertFalse(result.get(2).isExpired());

        assertEquals("INDEFINITE", result.get(3).effectiveStatus());
        assertTrue(result.get(3).isIndefinite());
    }

    @Test
    void createDocument_insuranceType_createsBothDocumentAndInsuranceRecord() {
        LocalDate expiry = LocalDate.now().plusYears(1);
        CreateDocumentRequest req = new CreateDocumentRequest(
                "INSURANCE",
                "Executive Comprehensive Policy",
                "POL-9999",
                "Hiscox",
                expiry.toString(),
                LocalDate.now(),
                LocalDate.now(),
                expiry,
                "Underwriter Direct",
                true,
                "ACTIVE",
                "Full zero-dep cover",
                null,
                "Executive Comprehensive Policy",
                "Hiscox",
                "POL-9999",
                "Zero-Dep + Engine Protect",
                new BigDecimal("150000.00"),
                new BigDecimal("3500.00"),
                expiry
        );

        when(documentRepository.save(any())).thenAnswer(inv -> {
            VehicleDocument doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });

        VehicleDocumentDto result = documentService.createDocument(vehicleId, req);

        assertNotNull(result);
        assertEquals("INSURANCE", result.docType());
        verify(documentRepository).save(any());
        verify(insuranceRepository).save(any());
    }

    @Test
    void renewDocument_updatesExpiryAndLinkedInsurance() {
        UUID docId = UUID.randomUUID();
        LocalDate oldExpiry = LocalDate.now().minusDays(2);
        LocalDate newExpiry = LocalDate.now().plusYears(1);

        VehicleDocument doc = new VehicleDocument(docId, vehicleId, "INSURANCE", "Old Policy", "POL-1", "Allianz", oldExpiry.toString(), null, null, oldExpiry, null, true, "EXPIRED", null, null, null, null, null);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(documentRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        VehicleInsurance ins = new VehicleInsurance(UUID.randomUUID(), vehicleId, docId, "Old Policy", "Allianz", "POL-1", "Scope", new BigDecimal("100000"), new BigDecimal("3000"), oldExpiry, "EXPIRED", null, false, null, null);
        when(insuranceRepository.findByDocumentId(docId)).thenReturn(Optional.of(ins));

        RenewDocumentRequest req = new RenewDocumentRequest(newExpiry, newExpiry.toString(), "POL-2-RENEWED", new BigDecimal("3200"), new BigDecimal("110000"), "Renewed for 2027");
        VehicleDocumentDto renewed = documentService.renewDocument(docId, req);

        assertNotNull(renewed);
        assertEquals(newExpiry, renewed.expiryDate());
        assertEquals("VALID", renewed.effectiveStatus());
        verify(insuranceRepository).update(ins);
        assertTrue(ins.getIsRenewed());
        assertEquals("POL-2-RENEWED", ins.getPolicyNumber());
    }
}
