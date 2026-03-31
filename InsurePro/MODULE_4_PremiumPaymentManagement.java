package com.insurepro;

// ================================================================
// MODULE 4: PREMIUM & PAYMENT MANAGEMENT
// ================================================================
// Files covered in this module:
//   entity/Invoice.java
//   entity/Payment.java
//   entity/enums/InvoiceStatus.java
//   entity/enums/PaymentMethod.java
//   entity/enums/PaymentStatus.java
//   repository/InvoiceRepository.java
//   repository/PaymentRepository.java
//   dto/InvoiceDTO.java
//   dto/PaymentDTO.java
//   dto/PaymentRequest.java
//   service/InvoiceService.java
//   service/PaymentService.java
//   controller/PaymentController.java
// ================================================================

// ─────────────────────────────────────────
// entity/enums/InvoiceStatus.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity.enums;

public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    PAID,
    OVERDUE,
    CANCELLED
}
*/

// ─────────────────────────────────────────
// entity/enums/PaymentMethod.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity.enums;

public enum PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    NET_BANKING,
    UPI,
    BANK_TRANSFER,
    CHEQUE
}
*/

// ─────────────────────────────────────────
// entity/enums/PaymentStatus.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity.enums;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    REFUNDED
}
*/

// ─────────────────────────────────────────
// entity/Invoice.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity;

import com.insurepro.entity.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate invoiceDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    // Billing period this invoice covers
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;

    @Column(length = 500)
    private String notes;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
*/

// ─────────────────────────────────────────
// entity/Payment.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity;

import com.insurepro.entity.enums.PaymentMethod;
import com.insurepro.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    // Gateway transaction reference (e.g., Razorpay/Stripe order ID)
    private String transactionReference;

    // Gateway name (e.g., Razorpay, Stripe, PayU)
    private String gatewayName;

    @Column(length = 500)
    private String failureReason;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
*/

// ─────────────────────────────────────────
// repository/InvoiceRepository.java
// ─────────────────────────────────────────
/*
package com.insurepro.repository;

import com.insurepro.entity.Invoice;
import com.insurepro.entity.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByCustomer_CustomerId(Long customerId);
    List<Invoice> findByPolicy_PolicyId(Long policyId);
    List<Invoice> findByStatus(InvoiceStatus status);

    // Overdue invoices (due date passed and not paid)
    @Query("SELECT i FROM Invoice i WHERE i.dueDate < :today AND i.status = 'ISSUED'")
    List<Invoice> findOverdueInvoices(LocalDate today);

    Page<Invoice> findByCustomer_CustomerIdAndStatus(Long customerId, InvoiceStatus status, Pageable pageable);

    @Query("SELECT SUM(i.amount) FROM Invoice i WHERE i.status = 'PAID'")
    BigDecimal totalCollectedPremium();
}
*/

// ─────────────────────────────────────────
// repository/PaymentRepository.java
// ─────────────────────────────────────────
/*
package com.insurepro.repository;

import com.insurepro.entity.Payment;
import com.insurepro.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByInvoice_InvoiceId(Long invoiceId);
    Optional<Payment> findByTransactionReference(String reference);
    List<Payment> findByStatus(PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal totalSuccessfulPayments();
}
*/

// ─────────────────────────────────────────
// service/InvoiceService.java
// ─────────────────────────────────────────
/*
package com.insurepro.service;

import com.insurepro.entity.*;
import com.insurepro.entity.enums.*;
import com.insurepro.exception.*;
import com.insurepro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PolicyRepository policyRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public Invoice generateInvoice(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

        Invoice invoice = Invoice.builder()
                .policy(policy)
                .customer(policy.getCustomer())
                .amount(policy.getPremiumAmount())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .billingPeriodStart(policy.getStartDate())
                .billingPeriodEnd(policy.getEndDate())
                .status(InvoiceStatus.ISSUED)
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        auditLogService.log(null, "INVOICE_GENERATED", "Invoice", saved.getInvoiceId(), "");
        notificationService.sendNotification(
                policy.getCustomer().getEmail(),
                "New Invoice",
                "Invoice #" + saved.getInvoiceId() + " for ₹" + saved.getAmount() +
                " is due by " + saved.getDueDate());
        return saved;
    }

    public List<Invoice> getInvoicesByCustomer(Long customerId) {
        return invoiceRepository.findByCustomer_CustomerId(customerId);
    }

    // Scheduled: Mark overdue invoices daily at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void markOverdueInvoices() {
        List<Invoice> overdueInvoices = invoiceRepository.findOverdueInvoices(LocalDate.now());
        overdueInvoices.forEach(inv -> {
            inv.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(inv);
            notificationService.sendNotification(
                    inv.getCustomer().getEmail(),
                    "Payment Overdue",
                    "Your invoice #" + inv.getInvoiceId() + " is overdue. Please pay to avoid policy lapse.");
        });
    }
}
*/

// ─────────────────────────────────────────
// service/PaymentService.java
// ─────────────────────────────────────────
/*
package com.insurepro.service;

import com.insurepro.dto.PaymentRequest;
import com.insurepro.entity.*;
import com.insurepro.entity.enums.*;
import com.insurepro.exception.*;
import com.insurepro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public Payment initiatePayment(Long invoiceId, PaymentRequest request) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("Invoice is already paid");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Invoice is cancelled");
        }

        // Generate internal transaction reference
        String transactionRef = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        Payment payment = Payment.builder()
                .invoice(invoice)
                .amount(invoice.getAmount())
                .paymentDate(LocalDate.now())
                .method(PaymentMethod.valueOf(request.getPaymentMethod()))
                .gatewayName(request.getGatewayName())
                .transactionReference(transactionRef)
                .status(PaymentStatus.PROCESSING)
                .build();

        Payment saved = paymentRepository.save(payment);
        auditLogService.log(null, "PAYMENT_INITIATED", "Payment", saved.getPaymentId(), transactionRef);
        return saved;
    }

    @Transactional
    public Payment confirmPayment(Long paymentId, boolean success, String failureReason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (success) {
            payment.setStatus(PaymentStatus.SUCCESS);
            // Mark invoice as paid
            Invoice invoice = payment.getInvoice();
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);

            notificationService.sendNotification(
                    invoice.getCustomer().getEmail(),
                    "Payment Successful",
                    "Payment of ₹" + payment.getAmount() + " received for invoice #" + invoice.getInvoiceId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(failureReason);
        }

        auditLogService.log(null,
                success ? "PAYMENT_SUCCESS" : "PAYMENT_FAILED",
                "Payment", paymentId, "");
        return paymentRepository.save(payment);
    }

    public List<Payment> getPaymentsByInvoice(Long invoiceId) {
        return paymentRepository.findByInvoice_InvoiceId(invoiceId);
    }

    @Transactional
    public Payment refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Only successful payments can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        // Revert invoice to ISSUED status
        Invoice invoice = payment.getInvoice();
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoiceRepository.save(invoice);

        auditLogService.log(null, "PAYMENT_REFUNDED", "Payment", paymentId, "");
        return paymentRepository.save(payment);
    }
}
*/

// ─────────────────────────────────────────
// controller/PaymentController.java
// ─────────────────────────────────────────
/*
package com.insurepro.controller;

import com.insurepro.dto.PaymentRequest;
import com.insurepro.entity.*;
import com.insurepro.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Invoice generation, premium payments, and refunds")
public class PaymentController {

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    // POST /api/v1/payments/invoices/generate/{policyId}
    @PostMapping("/invoices/generate/{policyId}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @Operation(summary = "Generate a premium invoice for a policy")
    public ResponseEntity<Invoice> generateInvoice(@PathVariable Long policyId) {
        return ResponseEntity.ok(invoiceService.generateInvoice(policyId));
    }

    // GET /api/v1/payments/invoices/customer/{customerId}
    @GetMapping("/invoices/customer/{customerId}")
    @Operation(summary = "Get all invoices for a customer")
    public ResponseEntity<List<Invoice>> getInvoicesByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(invoiceService.getInvoicesByCustomer(customerId));
    }

    // POST /api/v1/payments/invoices/{invoiceId}/pay
    @PostMapping("/invoices/{invoiceId}/pay")
    @PreAuthorize("hasAnyRole('POLICYHOLDER','AGENT')")
    @Operation(summary = "Initiate payment for an invoice")
    public ResponseEntity<Payment> initiatePayment(
            @PathVariable Long invoiceId,
            @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.initiatePayment(invoiceId, request));
    }

    // POST /api/v1/payments/{paymentId}/confirm
    @PostMapping("/{paymentId}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @Operation(summary = "Confirm or fail a payment (webhook from payment gateway)")
    public ResponseEntity<Payment> confirmPayment(
            @PathVariable Long paymentId,
            @RequestParam boolean success,
            @RequestParam(required = false) String failureReason) {
        return ResponseEntity.ok(paymentService.confirmPayment(paymentId, success, failureReason));
    }

    // GET /api/v1/payments/invoices/{invoiceId}/history
    @GetMapping("/invoices/{invoiceId}/history")
    @Operation(summary = "Get payment history for an invoice")
    public ResponseEntity<List<Payment>> getPaymentHistory(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(paymentService.getPaymentsByInvoice(invoiceId));
    }

    // POST /api/v1/payments/{paymentId}/refund
    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refund a successful payment")
    public ResponseEntity<Payment> refundPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId));
    }
}
*/
