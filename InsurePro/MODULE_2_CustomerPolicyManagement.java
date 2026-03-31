package com.insurepro;

// ================================================================
// MODULE 2: CUSTOMER & POLICY MANAGEMENT
// ================================================================
// Files covered in this module:
//   entity/Customer.java
//   entity/Policy.java
//   entity/enums/KYCStatus.java
//   entity/enums/PolicyType.java
//   entity/enums/PolicyStatus.java
//   repository/CustomerRepository.java
//   repository/PolicyRepository.java
//   dto/CustomerDTO.java
//   dto/PolicyDTO.java
//   service/CustomerService.java
//   service/PolicyService.java
//   controller/CustomerController.java
//   controller/PolicyController.java
// ================================================================

// ─────────────────────────────────────────
// entity/enums/KYCStatus.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity.enums;

public enum KYCStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
*/

// ─────────────────────────────────────────
// entity/enums/PolicyType.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity.enums;

public enum PolicyType {
    HEALTH,
    LIFE,
    AUTO,
    HOME,
    TRAVEL,
    LIABILITY
}
*/

// ─────────────────────────────────────────
// entity/enums/PolicyStatus.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity.enums;

public enum PolicyStatus {
    DRAFT,
    ACTIVE,
    EXPIRED,
    CANCELLED,
    PENDING_RENEWAL
}
*/

// ─────────────────────────────────────────
// entity/Customer.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity;

import com.insurepro.entity.enums.KYCStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customers")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate dob;

    // ContactInfo stored as JSON or separate fields
    private String email;
    private String phone;
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KYCStatus kycStatus = KYCStatus.PENDING;

    // KYC document reference (file path or document ID)
    private String kycDocumentRef;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Policy> policies;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
*/

// ─────────────────────────────────────────
// entity/Policy.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity;

import com.insurepro.entity.enums.PolicyStatus;
import com.insurepro.entity.enums.PolicyType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "policies")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long policyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyType policyType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal coverageAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal premiumAmount;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus status = PolicyStatus.DRAFT;

    // Underwriter who approved this policy
    private Long approvedBy;

    private String termsAndConditions;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
*/

// ─────────────────────────────────────────
// repository/CustomerRepository.java
// ─────────────────────────────────────────
/*
package com.insurepro.repository;

import com.insurepro.entity.Customer;
import com.insurepro.entity.enums.KYCStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Customer> findByKycStatus(KYCStatus kycStatus);

    @Query("SELECT c FROM Customer c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Customer> searchByName(String name, Pageable pageable);
}
*/

// ─────────────────────────────────────────
// repository/PolicyRepository.java
// ─────────────────────────────────────────
/*
package com.insurepro.repository;

import com.insurepro.entity.Policy;
import com.insurepro.entity.enums.PolicyStatus;
import com.insurepro.entity.enums.PolicyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findByCustomer_CustomerId(Long customerId);
    List<Policy> findByStatus(PolicyStatus status);
    List<Policy> findByPolicyType(PolicyType type);

    // Policies expiring within the next N days (for renewal alerts)
    @Query("SELECT p FROM Policy p WHERE p.endDate BETWEEN :today AND :futureDate AND p.status = 'ACTIVE'")
    List<Policy> findExpiringPolicies(LocalDate today, LocalDate futureDate);

    Page<Policy> findByCustomer_CustomerIdAndStatus(Long customerId, PolicyStatus status, Pageable pageable);
}
*/

// ─────────────────────────────────────────
// dto/CustomerDTO.java
// ─────────────────────────────────────────
/*
package com.insurepro.dto;

import com.insurepro.entity.enums.KYCStatus;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CustomerDTO {
    private Long customerId;
    private String name;
    private LocalDate dob;
    private String email;
    private String phone;
    private String address;
    private KYCStatus kycStatus;
}

// CreateCustomerRequest.java
@Data
public class CreateCustomerRequest {
    @NotBlank private String name;
    @NotNull private LocalDate dob;
    @Email @NotBlank private String email;
    private String phone;
    private String address;
}
*/

// ─────────────────────────────────────────
// dto/PolicyDTO.java
// ─────────────────────────────────────────
/*
package com.insurepro.dto;

import com.insurepro.entity.enums.PolicyStatus;
import com.insurepro.entity.enums.PolicyType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PolicyDTO {
    private Long policyId;
    private Long customerId;
    private String customerName;
    private PolicyType policyType;
    private BigDecimal coverageAmount;
    private BigDecimal premiumAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private PolicyStatus status;
}

// CreatePolicyRequest.java
@Data
public class CreatePolicyRequest {
    @NotNull private Long customerId;
    @NotNull private PolicyType policyType;
    @NotNull private BigDecimal coverageAmount;
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
}
*/

// ─────────────────────────────────────────
// service/CustomerService.java
// ─────────────────────────────────────────
/*
package com.insurepro.service;

import com.insurepro.dto.*;
import com.insurepro.entity.Customer;
import com.insurepro.entity.enums.KYCStatus;
import com.insurepro.exception.*;
import com.insurepro.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    public Customer createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Customer with email already exists");
        }
        Customer customer = Customer.builder()
                .name(request.getName())
                .dob(request.getDob())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .kycStatus(KYCStatus.PENDING)
                .build();
        Customer saved = customerRepository.save(customer);
        auditLogService.log(null, "CUSTOMER_CREATED", "Customer", saved.getCustomerId(), "");
        return saved;
    }

    public Customer getCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    public Page<Customer> searchCustomers(String name, Pageable pageable) {
        return customerRepository.searchByName(name, pageable);
    }

    @Transactional
    public Customer updateKycStatus(Long customerId, KYCStatus status) {
        Customer customer = getCustomer(customerId);
        customer.setKycStatus(status);
        auditLogService.log(null, "KYC_STATUS_UPDATED", "Customer", customerId,
                "New status: " + status);
        return customerRepository.save(customer);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
}
*/

// ─────────────────────────────────────────
// service/PolicyService.java
// ─────────────────────────────────────────
/*
package com.insurepro.service;

import com.insurepro.dto.*;
import com.insurepro.entity.*;
import com.insurepro.entity.enums.*;
import com.insurepro.exception.*;
import com.insurepro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public Policy createPolicy(CreatePolicyRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (customer.getKycStatus() != KYCStatus.VERIFIED) {
            throw new BadRequestException("Customer KYC not verified. Cannot create policy.");
        }

        BigDecimal premium = calculatePremium(request.getPolicyType(), request.getCoverageAmount());

        Policy policy = Policy.builder()
                .customer(customer)
                .policyType(request.getPolicyType())
                .coverageAmount(request.getCoverageAmount())
                .premiumAmount(premium)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(PolicyStatus.DRAFT)
                .build();

        Policy saved = policyRepository.save(policy);
        auditLogService.log(null, "POLICY_CREATED", "Policy", saved.getPolicyId(), "");
        notificationService.sendNotification(customer.getEmail(),
                "Policy Created", "Your policy #" + saved.getPolicyId() + " has been created.");
        return saved;
    }

    public Policy getPolicy(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + id));
    }

    public List<Policy> getPoliciesByCustomer(Long customerId) {
        return policyRepository.findByCustomer_CustomerId(customerId);
    }

    @Transactional
    public Policy activatePolicy(Long policyId, Long underwriterId) {
        Policy policy = getPolicy(policyId);
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setApprovedBy(underwriterId);
        auditLogService.log(underwriterId, "POLICY_ACTIVATED", "Policy", policyId, "");
        return policyRepository.save(policy);
    }

    @Transactional
    public Policy renewPolicy(Long policyId) {
        Policy policy = getPolicy(policyId);
        // Extend by 1 year from current end date
        policy.setEndDate(policy.getEndDate().plusYears(1));
        policy.setStatus(PolicyStatus.ACTIVE);
        auditLogService.log(null, "POLICY_RENEWED", "Policy", policyId, "");
        return policyRepository.save(policy);
    }

    @Transactional
    public Policy cancelPolicy(Long policyId, String reason) {
        Policy policy = getPolicy(policyId);
        policy.setStatus(PolicyStatus.CANCELLED);
        auditLogService.log(null, "POLICY_CANCELLED", "Policy", policyId, reason);
        return policyRepository.save(policy);
    }

    public List<Policy> getExpiringPolicies(int daysAhead) {
        return policyRepository.findExpiringPolicies(
                LocalDate.now(), LocalDate.now().plusDays(daysAhead));
    }

    // Premium calculation engine
    private BigDecimal calculatePremium(PolicyType type, BigDecimal coverageAmount) {
        double rate = switch (type) {
            case HEALTH    -> 0.05;
            case LIFE      -> 0.03;
            case AUTO      -> 0.04;
            case HOME      -> 0.025;
            case TRAVEL    -> 0.02;
            case LIABILITY -> 0.035;
        };
        return coverageAmount.multiply(BigDecimal.valueOf(rate));
    }
}
*/

// ─────────────────────────────────────────
// controller/CustomerController.java
// ─────────────────────────────────────────
/*
package com.insurepro.controller;

import com.insurepro.dto.*;
import com.insurepro.entity.Customer;
import com.insurepro.entity.enums.KYCStatus;
import com.insurepro.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer registration and KYC management")
public class CustomerController {

    private final CustomerService customerService;

    // POST /api/v1/customers
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','BROKER')")
    @Operation(summary = "Register a new customer")
    public ResponseEntity<Customer> createCustomer(@RequestBody CreateCustomerRequest request) {
        return ResponseEntity.ok(customerService.createCustomer(request));
    }

    // GET /api/v1/customers/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','BROKER','UNDERWRITER','COMPLIANCE_OFFICER')")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<Customer> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    // GET /api/v1/customers?name=John&page=0&size=20
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','BROKER')")
    @Operation(summary = "Search customers by name (paginated)")
    public ResponseEntity<Page<Customer>> searchCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        return ResponseEntity.ok(customerService.searchCustomers(name != null ? name : "", pageable));
    }

    // PATCH /api/v1/customers/{id}/kyc
    @PatchMapping("/{id}/kyc")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @Operation(summary = "Update customer KYC status")
    public ResponseEntity<Customer> updateKyc(
            @PathVariable Long id,
            @RequestParam KYCStatus status) {
        return ResponseEntity.ok(customerService.updateKycStatus(id, status));
    }
}
*/

// ─────────────────────────────────────────
// controller/PolicyController.java
// ─────────────────────────────────────────
/*
package com.insurepro.controller;

import com.insurepro.dto.CreatePolicyRequest;
import com.insurepro.entity.Policy;
import com.insurepro.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
@Tag(name = "Policies", description = "Policy lifecycle management")
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','BROKER')")
    @Operation(summary = "Create a new policy")
    public ResponseEntity<Policy> createPolicy(@RequestBody CreatePolicyRequest request) {
        return ResponseEntity.ok(policyService.createPolicy(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get policy by ID")
    public ResponseEntity<Policy> getPolicy(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.getPolicy(id));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all policies for a customer")
    public ResponseEntity<List<Policy>> getPoliciesByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(policyService.getPoliciesByCustomer(customerId));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('UNDERWRITER')")
    @Operation(summary = "Activate policy (Underwriter only)")
    public ResponseEntity<Policy> activatePolicy(
            @PathVariable Long id,
            @RequestParam Long underwriterId) {
        return ResponseEntity.ok(policyService.activatePolicy(id, underwriterId));
    }

    @PatchMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','BROKER')")
    @Operation(summary = "Renew an existing policy")
    public ResponseEntity<Policy> renewPolicy(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.renewPolicy(id));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @Operation(summary = "Cancel a policy")
    public ResponseEntity<Policy> cancelPolicy(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(policyService.cancelPolicy(id, reason));
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','BROKER')")
    @Operation(summary = "Get policies expiring within N days")
    public ResponseEntity<List<Policy>> getExpiringPolicies(
            @RequestParam(defaultValue = "30") int daysAhead) {
        return ResponseEntity.ok(policyService.getExpiringPolicies(daysAhead));
    }
}
*/
