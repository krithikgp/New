package com.insurepro;

// ================================================================
// MODULE 3: CLAIMS MANAGEMENT
// ================================================================
// Files covered in this module:
//   entity/Claim.java
//   entity/Settlement.java
//   entity/enums/ClaimType.java
//   entity/enums/ClaimStatus.java
//   entity/enums/SettlementStatus.java
//   repository/ClaimRepository.java
//   repository/SettlementRepository.java
//   dto/CreateClaimRequest.java
//   dto/ClaimDTO.java
//   dto/SettlementDTO.java
//   service/ClaimService.java
//   service/SettlementService.java
//   controller/ClaimController.java
// ================================================================

// ─────────────────────────────────────────
// entity/enums/ClaimType.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity.enums;

public enum ClaimType {
    HEALTH,
    ACCIDENT,
    FIRE,
    THEFT,
    NATURAL_DISASTER,
    LIABILITY,
    DEATH
}
*/

// ─────────────────────────────────────────
// entity/enums/ClaimStatus.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity.enums;

public enum ClaimStatus {
    SUBMITTED,
    UNDER_REVIEW,
    DOCUMENTS_REQUIRED,
    VALIDATED,
    APPROVED,
    REJECTED,
    SETTLED,
    CLOSED
}
*/

// ─────────────────────────────────────────
// entity/enums/SettlementStatus.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity.enums;

public enum SettlementStatus {
    PENDING,
    PROCESSING,
    PAID,
    FAILED
}
*/

// ─────────────────────────────────────────
// entity/Claim.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity;

import com.insurepro.entity.enums.ClaimStatus;
import com.insurepro.entity.enums.ClaimType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long claimId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private LocalDate claimDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimType claimType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status = ClaimStatus.SUBMITTED;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountRequested;

    @Column(length = 2000)
    private String description;

    // Comma-separated file paths to uploaded supporting documents
    @Column(length = 1000)
    private String documentPaths;

    // Adjuster who last reviewed this claim
    private Long assignedAdjusterId;

    // Rejection reason, if applicable
    @Column(length = 1000)
    private String rejectionReason;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
*/

// ─────────────────────────────────────────
// entity/Settlement.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity;

import com.insurepro.entity.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settlementId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false, unique = true)
    private Claim claim;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountApproved;

    @Column(nullable = false)
    private Long approvedBy;           // Adjuster user ID

    @Column(nullable = false)
    private LocalDate settlementDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status = SettlementStatus.PENDING;

    // Bank/payment reference once payout is initiated
    private String paymentReference;

    @Column(length = 1000)
    private String notes;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
*/

// ─────────────────────────────────────────
// repository/ClaimRepository.java
// ─────────────────────────────────────────
/*
package com.insurepro.repository;

import com.insurepro.entity.Claim;
import com.insurepro.entity.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByCustomer_CustomerId(Long customerId);
    List<Claim> findByPolicy_PolicyId(Long policyId);
    List<Claim> findByStatus(ClaimStatus status);
    List<Claim> findByAssignedAdjusterId(Long adjusterId);
    Page<Claim> findByStatus(ClaimStatus status, Pageable pageable);

    @Query("SELECT c FROM Claim c WHERE c.claimDate BETWEEN :from AND :to")
    List<Claim> findClaimsInDateRange(LocalDate from, LocalDate to);

    // KPI: claim count by status
    @Query("SELECT c.status, COUNT(c) FROM Claim c GROUP BY c.status")
    List<Object[]> countByStatus();

    // Total amount requested by a customer
    @Query("SELECT SUM(c.amountRequested) FROM Claim c WHERE c.customer.customerId = :customerId AND c.status = 'APPROVED'")
    java.math.BigDecimal totalApprovedAmountByCustomer(Long customerId);
}
*/

// ─────────────────────────────────────────
// repository/SettlementRepository.java
// ─────────────────────────────────────────
/*
package com.insurepro.repository;

import com.insurepro.entity.Settlement;
import com.insurepro.entity.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByClaim_ClaimId(Long claimId);
    List<Settlement> findByStatus(SettlementStatus status);

    @Query("SELECT SUM(s.amountApproved) FROM Settlement s WHERE s.status = 'PAID'")
    BigDecimal totalPaidAmount();
}
*/

// ─────────────────────────────────────────
// service/ClaimService.java
// ─────────────────────────────────────────
/*
package com.insurepro.service;

import com.insurepro.dto.CreateClaimRequest;
import com.insurepro.entity.*;
import com.insurepro.entity.enums.*;
import com.insurepro.exception.*;
import com.insurepro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Transactional
    public Claim submitClaim(CreateClaimRequest request, List<MultipartFile> documents) throws IOException {
        Policy policy = policyRepository.findById(request.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new BadRequestException("Cannot submit claim on non-active policy");
        }

        // Save uploaded documents
        List<String> savedPaths = new ArrayList<>();
        for (MultipartFile doc : documents) {
            String filename = UUID.randomUUID() + "_" + doc.getOriginalFilename();
            Path filePath = Paths.get(uploadDir, filename);
            Files.createDirectories(filePath.getParent());
            Files.copy(doc.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            savedPaths.add(filePath.toString());
        }

        Claim claim = Claim.builder()
                .policy(policy)
                .customer(policy.getCustomer())
                .claimDate(LocalDate.now())
                .claimType(request.getClaimType())
                .amountRequested(request.getAmountRequested())
                .description(request.getDescription())
                .documentPaths(String.join(",", savedPaths))
                .status(ClaimStatus.SUBMITTED)
                .build();

        Claim saved = claimRepository.save(claim);
        auditLogService.log(null, "CLAIM_SUBMITTED", "Claim", saved.getClaimId(), "");
        notificationService.sendNotification(
                policy.getCustomer().getEmail(),
                "Claim Submitted",
                "Your claim #" + saved.getClaimId() + " has been submitted successfully.");
        return saved;
    }

    public Claim getClaim(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + id));
    }

    public List<Claim> getClaimsByCustomer(Long customerId) {
        return claimRepository.findByCustomer_CustomerId(customerId);
    }

    @Transactional
    public Claim assignAdjuster(Long claimId, Long adjusterId) {
        Claim claim = getClaim(claimId);
        claim.setAssignedAdjusterId(adjusterId);
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        auditLogService.log(adjusterId, "CLAIM_ASSIGNED", "Claim", claimId, "");
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim approveClaim(Long claimId, Long adjusterId) {
        Claim claim = getClaim(claimId);
        claim.setStatus(ClaimStatus.APPROVED);
        auditLogService.log(adjusterId, "CLAIM_APPROVED", "Claim", claimId, "");
        notificationService.sendNotification(
                claim.getCustomer().getEmail(),
                "Claim Approved",
                "Your claim #" + claimId + " has been approved. Settlement is in progress.");
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim rejectClaim(Long claimId, Long adjusterId, String reason) {
        Claim claim = getClaim(claimId);
        claim.setStatus(ClaimStatus.REJECTED);
        claim.setRejectionReason(reason);
        auditLogService.log(adjusterId, "CLAIM_REJECTED", "Claim", claimId, reason);
        notificationService.sendNotification(
                claim.getCustomer().getEmail(),
                "Claim Rejected",
                "Your claim #" + claimId + " has been rejected. Reason: " + reason);
        return claimRepository.save(claim);
    }

    public List<Claim> getClaimsByStatus(ClaimStatus status) {
        return claimRepository.findByStatus(status);
    }
}
*/

// ─────────────────────────────────────────
// service/SettlementService.java
// ─────────────────────────────────────────
/*
package com.insurepro.service;

import com.insurepro.entity.*;
import com.insurepro.entity.enums.*;
import com.insurepro.exception.*;
import com.insurepro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final ClaimRepository claimRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public Settlement createSettlement(Long claimId, BigDecimal amountApproved, Long adjusterId, String notes) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found"));

        if (claim.getStatus() != ClaimStatus.APPROVED) {
            throw new BadRequestException("Claim must be in APPROVED status before settlement");
        }

        if (settlementRepository.findByClaim_ClaimId(claimId).isPresent()) {
            throw new BadRequestException("Settlement already exists for claim: " + claimId);
        }

        // Ensure settlement does not exceed coverage
        BigDecimal coverage = claim.getPolicy().getCoverageAmount();
        if (amountApproved.compareTo(coverage) > 0) {
            throw new BadRequestException("Settlement amount exceeds coverage amount");
        }

        Settlement settlement = Settlement.builder()
                .claim(claim)
                .amountApproved(amountApproved)
                .approvedBy(adjusterId)
                .settlementDate(LocalDate.now())
                .status(SettlementStatus.PENDING)
                .notes(notes)
                .build();

        Settlement saved = settlementRepository.save(settlement);

        // Mark claim as settled
        claim.setStatus(ClaimStatus.SETTLED);
        claimRepository.save(claim);

        auditLogService.log(adjusterId, "SETTLEMENT_CREATED", "Settlement", saved.getSettlementId(), "");
        notificationService.sendNotification(
                claim.getCustomer().getEmail(),
                "Settlement Initiated",
                "Settlement of ₹" + amountApproved + " for claim #" + claimId + " is being processed.");

        return saved;
    }

    public Settlement getSettlement(Long id) {
        return settlementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found"));
    }

    @Transactional
    public Settlement markAsPaid(Long settlementId, String paymentReference) {
        Settlement settlement = getSettlement(settlementId);
        settlement.setStatus(SettlementStatus.PAID);
        settlement.setPaymentReference(paymentReference);
        auditLogService.log(null, "SETTLEMENT_PAID", "Settlement", settlementId, paymentReference);
        return settlementRepository.save(settlement);
    }
}
*/

// ─────────────────────────────────────────
// controller/ClaimController.java
// ─────────────────────────────────────────
/*
package com.insurepro.controller;

import com.insurepro.dto.CreateClaimRequest;
import com.insurepro.entity.Claim;
import com.insurepro.entity.Settlement;
import com.insurepro.entity.enums.ClaimStatus;
import com.insurepro.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/claims")
@RequiredArgsConstructor
@Tag(name = "Claims", description = "Claim submission, adjudication, and settlement")
public class ClaimController {

    private final ClaimService claimService;
    private final SettlementService settlementService;

    // POST /api/v1/claims  (multipart: claim data + documents)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('POLICYHOLDER','AGENT','BROKER')")
    @Operation(summary = "Submit a new claim with supporting documents")
    public ResponseEntity<Claim> submitClaim(
            @RequestPart("claim") CreateClaimRequest request,
            @RequestPart(value = "documents", required = false) List<MultipartFile> documents) throws Exception {
        return ResponseEntity.ok(claimService.submitClaim(request, documents != null ? documents : List.of()));
    }

    // GET /api/v1/claims/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Get claim details by ID")
    public ResponseEntity<Claim> getClaim(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaim(id));
    }

    // GET /api/v1/claims/customer/{customerId}
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all claims for a customer")
    public ResponseEntity<List<Claim>> getClaimsByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(claimService.getClaimsByCustomer(customerId));
    }

    // GET /api/v1/claims?status=SUBMITTED
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CLAIMS_ADJUSTER')")
    @Operation(summary = "Get claims filtered by status")
    public ResponseEntity<List<Claim>> getClaimsByStatus(@RequestParam(required = false) ClaimStatus status) {
        return ResponseEntity.ok(claimService.getClaimsByStatus(status));
    }

    // PATCH /api/v1/claims/{id}/assign
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','CLAIMS_ADJUSTER')")
    @Operation(summary = "Assign adjuster to a claim")
    public ResponseEntity<Claim> assignAdjuster(@PathVariable Long id, @RequestParam Long adjusterId) {
        return ResponseEntity.ok(claimService.assignAdjuster(id, adjusterId));
    }

    // PATCH /api/v1/claims/{id}/approve
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('CLAIMS_ADJUSTER')")
    @Operation(summary = "Approve a claim")
    public ResponseEntity<Claim> approveClaim(@PathVariable Long id, @RequestParam Long adjusterId) {
        return ResponseEntity.ok(claimService.approveClaim(id, adjusterId));
    }

    // PATCH /api/v1/claims/{id}/reject
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('CLAIMS_ADJUSTER')")
    @Operation(summary = "Reject a claim with reason")
    public ResponseEntity<Claim> rejectClaim(
            @PathVariable Long id,
            @RequestParam Long adjusterId,
            @RequestParam String reason) {
        return ResponseEntity.ok(claimService.rejectClaim(id, adjusterId, reason));
    }

    // POST /api/v1/claims/{id}/settlement
    @PostMapping("/{id}/settlement")
    @PreAuthorize("hasRole('CLAIMS_ADJUSTER')")
    @Operation(summary = "Create a settlement for an approved claim")
    public ResponseEntity<Settlement> createSettlement(
            @PathVariable Long id,
            @RequestParam BigDecimal amountApproved,
            @RequestParam Long adjusterId,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(settlementService.createSettlement(id, amountApproved, adjusterId, notes));
    }

    // PATCH /api/v1/claims/settlements/{settlementId}/pay
    @PatchMapping("/settlements/{settlementId}/pay")
    @PreAuthorize("hasAnyRole('ADMIN','CLAIMS_ADJUSTER')")
    @Operation(summary = "Mark settlement as paid")
    public ResponseEntity<Settlement> markSettlementPaid(
            @PathVariable Long settlementId,
            @RequestParam String paymentReference) {
        return ResponseEntity.ok(settlementService.markAsPaid(settlementId, paymentReference));
    }
}
*/
