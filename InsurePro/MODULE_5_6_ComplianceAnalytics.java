package com.insurepro;

// ================================================================
// MODULE 5: COMPLIANCE & AUDIT MANAGEMENT
// ================================================================
// Files covered in this module:
//   entity/ComplianceReport.java
//   repository/ComplianceReportRepository.java
//   service/ComplianceService.java
//   controller/ComplianceController.java
// ================================================================

// ─────────────────────────────────────────
// entity/ComplianceReport.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "compliance_reports")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Column(nullable = false)
    private String reportType;       // e.g. "MONTHLY", "QUARTERLY", "ANNUAL", "EXCEPTION"

    @Column(nullable = false)
    private String scope;            // e.g. "CLAIMS", "PREMIUMS", "POLICIES", "FULL"

    // JSON blob containing the computed metrics
    @Column(columnDefinition = "TEXT", nullable = false)
    private String metrics;

    @Column(nullable = false)
    private String generatedByEmail; // compliance officer who triggered this

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedDate;

    // Period this report covers
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    // File path if the report was exported to PDF/CSV
    private String exportFilePath;

    @Column(nullable = false)
    private String status;           // DRAFT, FINALIZED, SUBMITTED_TO_REGULATOR
}
*/

// ─────────────────────────────────────────
// repository/ComplianceReportRepository.java
// ─────────────────────────────────────────
/*
package com.insurepro.repository;

import com.insurepro.entity.ComplianceReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplianceReportRepository extends JpaRepository<ComplianceReport, Long> {
    List<ComplianceReport> findByReportType(String reportType);
    List<ComplianceReport> findByScope(String scope);
    Page<ComplianceReport> findByGeneratedDateBetween(
            LocalDateTime from, LocalDateTime to, Pageable pageable);
    List<ComplianceReport> findByGeneratedByEmail(String email);
}
*/

// ─────────────────────────────────────────
// service/ComplianceService.java
// ─────────────────────────────────────────
/*
package com.insurepro.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurepro.entity.*;
import com.insurepro.entity.enums.*;
import com.insurepro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ComplianceService {

    private final ComplianceReportRepository complianceReportRepository;
    private final AuditLogRepository auditLogRepository;
    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final InvoiceRepository invoiceRepository;
    private final ObjectMapper objectMapper;

    // Generate a compliance report on demand
    @Transactional
    public ComplianceReport generateReport(String reportType, String scope,
                                           String generatedByEmail,
                                           LocalDateTime periodStart,
                                           LocalDateTime periodEnd) throws Exception {
        Map<String, Object> metrics = buildMetrics(scope, periodStart, periodEnd);

        ComplianceReport report = ComplianceReport.builder()
                .reportType(reportType)
                .scope(scope)
                .metrics(objectMapper.writeValueAsString(metrics))
                .generatedByEmail(generatedByEmail)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .status("DRAFT")
                .build();

        return complianceReportRepository.save(report);
    }

    // Scheduled: auto-generate monthly compliance report on 1st of every month
    @Scheduled(cron = "0 0 6 1 * ?")
    public void autoGenerateMonthlyReport() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusMonths(1).withDayOfMonth(1).withHour(0);
        LocalDateTime end   = LocalDateTime.now().withDayOfMonth(1).withHour(0).minusSeconds(1);
        generateReport("MONTHLY", "FULL", "system@insurepro.com", start, end);
    }

    private Map<String, Object> buildMetrics(String scope, LocalDateTime from, LocalDateTime to) {
        Map<String, Object> metrics = new LinkedHashMap<>();

        if ("CLAIMS".equals(scope) || "FULL".equals(scope)) {
            List<Object[]> claimStatusCounts = claimRepository.countByStatus();
            Map<String, Long> claimBreakdown = new LinkedHashMap<>();
            for (Object[] row : claimStatusCounts) {
                claimBreakdown.put(((ClaimStatus) row[0]).name(), (Long) row[1]);
            }
            metrics.put("claimsByStatus", claimBreakdown);
            metrics.put("totalClaims", claimBreakdown.values().stream().mapToLong(l -> l).sum());
        }

        if ("PREMIUMS".equals(scope) || "FULL".equals(scope)) {
            metrics.put("totalCollectedPremium", invoiceRepository.totalCollectedPremium());
        }

        if ("POLICIES".equals(scope) || "FULL".equals(scope)) {
            metrics.put("totalActivePolicies",
                    policyRepository.findByStatus(PolicyStatus.ACTIVE).size());
            metrics.put("totalCancelledPolicies",
                    policyRepository.findByStatus(PolicyStatus.CANCELLED).size());
        }

        metrics.put("reportPeriod", from + " to " + to);
        return metrics;
    }

    public List<AuditLog> getAuditLogs(Long userId, String entityType, Long entityId) {
        if (userId != null) return auditLogRepository.findByUserId(userId);
        if (entityType != null && entityId != null)
            return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
        return auditLogRepository.findAll();
    }

    public Page<ComplianceReport> getAllReports(Pageable pageable) {
        return complianceReportRepository.findAll(pageable);
    }

    @Transactional
    public ComplianceReport finalizeReport(Long reportId) {
        ComplianceReport report = complianceReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        report.setStatus("FINALIZED");
        return complianceReportRepository.save(report);
    }
}
*/

// ─────────────────────────────────────────
// controller/ComplianceController.java
// ─────────────────────────────────────────
/*
package com.insurepro.controller;

import com.insurepro.entity.*;
import com.insurepro.service.ComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/compliance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','ADMIN')")
@Tag(name = "Compliance & Audit", description = "Audit logs and regulatory report generation")
public class ComplianceController {

    private final ComplianceService complianceService;

    // POST /api/v1/compliance/reports/generate
    @PostMapping("/reports/generate")
    @Operation(summary = "Generate a new compliance report")
    public ResponseEntity<ComplianceReport> generateReport(
            @RequestParam String reportType,
            @RequestParam String scope,
            @RequestParam String generatedBy,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) throws Exception {
        return ResponseEntity.ok(complianceService.generateReport(reportType, scope, generatedBy, from, to));
    }

    // GET /api/v1/compliance/reports
    @GetMapping("/reports")
    @Operation(summary = "List all compliance reports (paginated)")
    public ResponseEntity<Page<ComplianceReport>> listReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(complianceService.getAllReports(PageRequest.of(page, size,
                Sort.by("generatedDate").descending())));
    }

    // PATCH /api/v1/compliance/reports/{id}/finalize
    @PatchMapping("/reports/{id}/finalize")
    @Operation(summary = "Finalize a compliance report")
    public ResponseEntity<ComplianceReport> finalizeReport(@PathVariable Long id) {
        return ResponseEntity.ok(complianceService.finalizeReport(id));
    }

    // GET /api/v1/compliance/audit-logs?userId=1&entityType=Claim&entityId=5
    @GetMapping("/audit-logs")
    @Operation(summary = "View immutable audit logs with optional filters")
    public ResponseEntity<List<AuditLog>> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId) {
        return ResponseEntity.ok(complianceService.getAuditLogs(userId, entityType, entityId));
    }
}
*/

// ================================================================
// MODULE 6: ANALYTICS & REPORTING
// ================================================================
// Files covered in this module:
//   entity/KPIReport.java
//   repository/KPIReportRepository.java
//   service/AnalyticsService.java
//   controller/AnalyticsController.java
// ================================================================

// ─────────────────────────────────────────
// entity/KPIReport.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "kpi_reports")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KPIReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Column(nullable = false)
    private String scope;           // e.g. "CLAIMS", "PREMIUMS", "POLICIES"

    // JSON blob containing computed KPI values
    @Column(columnDefinition = "TEXT", nullable = false)
    private String metrics;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedDate;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
}
*/

// ─────────────────────────────────────────
// service/AnalyticsService.java
// ─────────────────────────────────────────
/*
package com.insurepro.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurepro.entity.*;
import com.insurepro.entity.enums.*;
import com.insurepro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final InvoiceRepository invoiceRepository;
    private final SettlementRepository settlementRepository;
    private final KPIReportRepository kpiReportRepository;
    private final ObjectMapper objectMapper;

    // Real-time dashboard KPIs
    public Map<String, Object> getDashboardKPIs() {
        Map<String, Object> kpis = new LinkedHashMap<>();

        // Total active policies
        long activePolicies = policyRepository.countByStatus(PolicyStatus.ACTIVE);
        kpis.put("activePolicies", activePolicies);

        // Claims breakdown
        List<Object[]> claimCounts = claimRepository.countByStatus();
        Map<String, Long> claimBreakdown = new LinkedHashMap<>();
        long totalClaims = 0;
        long settledClaims = 0;
        for (Object[] row : claimCounts) {
            String status = ((ClaimStatus) row[0]).name();
            long count = (Long) row[1];
            claimBreakdown.put(status, count);
            totalClaims += count;
            if ("SETTLED".equals(status) || "APPROVED".equals(status)) settledClaims += count;
        }
        kpis.put("claimsByStatus", claimBreakdown);

        // Claim Settlement Rate (%)
        double settlementRate = totalClaims > 0 ?
                (double) settledClaims / totalClaims * 100 : 0.0;
        kpis.put("claimSettlementRate", String.format("%.2f%%", settlementRate));

        // Total premium collected
        BigDecimal totalPremium = invoiceRepository.totalCollectedPremium();
        kpis.put("totalPremiumCollected", totalPremium != null ? totalPremium : BigDecimal.ZERO);

        // Total settlements paid out
        BigDecimal totalSettlements = settlementRepository.totalPaidAmount();
        kpis.put("totalSettlementsPaid", totalSettlements != null ? totalSettlements : BigDecimal.ZERO);

        // Claim Ratio = Total claims paid / Total premiums collected
        if (totalPremium != null && totalPremium.compareTo(BigDecimal.ZERO) > 0
                && totalSettlements != null) {
            BigDecimal claimRatio = totalSettlements
                    .divide(totalPremium, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            kpis.put("claimRatio", claimRatio.setScale(2, RoundingMode.HALF_UP) + "%");
        } else {
            kpis.put("claimRatio", "0.00%");
        }

        // Policies expiring in next 30 days
        long expiringPolicies = policyRepository.findExpiringPolicies(
                LocalDate.now(), LocalDate.now().plusDays(30)).size();
        kpis.put("policiesExpiringIn30Days", expiringPolicies);

        return kpis;
    }

    @Transactional
    public KPIReport snapshotKPIs(String scope) throws Exception {
        Map<String, Object> kpis = getDashboardKPIs();
        KPIReport report = KPIReport.builder()
                .scope(scope)
                .metrics(objectMapper.writeValueAsString(kpis))
                .build();
        return kpiReportRepository.save(report);
    }
}
*/

// ─────────────────────────────────────────
// controller/AnalyticsController.java
// ─────────────────────────────────────────
/*
package com.insurepro.controller;

import com.insurepro.entity.KPIReport;
import com.insurepro.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE_OFFICER','UNDERWRITER')")
@Tag(name = "Analytics & Reporting", description = "KPIs, dashboards, and financial reporting")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // GET /api/v1/analytics/dashboard
    @GetMapping("/dashboard")
    @Operation(summary = "Get real-time KPI dashboard data")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(analyticsService.getDashboardKPIs());
    }

    // POST /api/v1/analytics/kpi-snapshot?scope=FULL
    @PostMapping("/kpi-snapshot")
    @Operation(summary = "Persist a KPI snapshot for historical trend analysis")
    public ResponseEntity<KPIReport> snapshotKPIs(
            @RequestParam(defaultValue = "FULL") String scope) throws Exception {
        return ResponseEntity.ok(analyticsService.snapshotKPIs(scope));
    }
}
*/
