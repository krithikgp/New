package com.insurepro;

// ================================================================
// MODULE 7: NOTIFICATIONS & ALERTS
// ================================================================
// Files covered in this module:
//   entity/Notification.java
//   entity/enums/NotificationCategory.java
//   repository/NotificationRepository.java
//   service/NotificationService.java
//   controller/NotificationController.java
//
// PLUS: Global Exception Handler
//   exception/ResourceNotFoundException.java
//   exception/BadRequestException.java
//   exception/GlobalExceptionHandler.java
// ================================================================

// ─────────────────────────────────────────
// entity/enums/NotificationCategory.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity.enums;

public enum NotificationCategory {
    CLAIM_UPDATE,
    PAYMENT_REMINDER,
    PAYMENT_CONFIRMATION,
    POLICY_RENEWAL,
    POLICY_EXPIRY,
    KYC_UPDATE,
    COMPLIANCE_ALERT,
    SYSTEM
}
*/

// ─────────────────────────────────────────
// entity/Notification.java
// ─────────────────────────────────────────
/*
package com.insurepro.entity;

import com.insurepro.entity.enums.NotificationCategory;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    // Target user (nullable = broadcast / email-only notification)
    private Long userId;

    private String recipientEmail;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationCategory category;

    // READ / UNREAD / SENT / FAILED
    @Column(nullable = false)
    private String status;

    // INAPP, EMAIL, SMS (comma-separated for multiple channels)
    @Column(nullable = false)
    private String channels;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    private LocalDateTime readAt;
}
*/

// ─────────────────────────────────────────
// repository/NotificationRepository.java
// ─────────────────────────────────────────
/*
package com.insurepro.repository;

import com.insurepro.entity.Notification;
import com.insurepro.entity.enums.NotificationCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndStatus(Long userId, String status);
    List<Notification> findByUserId(Long userId);
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    List<Notification> findByCategory(NotificationCategory category);
    long countByUserIdAndStatus(Long userId, String status);
}
*/

// ─────────────────────────────────────────
// service/NotificationService.java
// ─────────────────────────────────────────
/*
package com.insurepro.service;

import com.insurepro.entity.Notification;
import com.insurepro.entity.enums.NotificationCategory;
import com.insurepro.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    // Called throughout the codebase — fires and forgets asynchronously
    @Async
    public void sendNotification(String recipientEmail, String subject, String message) {
        sendNotification(null, recipientEmail, subject, message, NotificationCategory.SYSTEM);
    }

    @Async
    public void sendNotification(Long userId, String recipientEmail, String subject,
                                  String message, NotificationCategory category) {
        // 1. Persist in-app notification
        Notification notification = Notification.builder()
                .userId(userId)
                .recipientEmail(recipientEmail)
                .subject(subject)
                .message(message)
                .category(category)
                .status("UNREAD")
                .channels("INAPP,EMAIL")
                .build();
        notificationRepository.save(notification);

        // 2. Send email
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(recipientEmail);
            mail.setSubject("[InsurePro] " + subject);
            mail.setText(message);
            mailSender.send(mail);

            notification.setStatus("SENT");
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", recipientEmail, e.getMessage());
            notification.setStatus("FAILED");
        }

        notificationRepository.save(notification);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndStatus(userId, "UNREAD");
    }

    public List<Notification> getAllNotifications(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setStatus("READ");
        notification.setReadAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndStatus(userId, "UNREAD");
    }
}
*/

// ─────────────────────────────────────────
// controller/NotificationController.java
// ─────────────────────────────────────────
/*
package com.insurepro.controller;

import com.insurepro.entity.Notification;
import com.insurepro.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notifications and alert management")
public class NotificationController {

    private final NotificationService notificationService;

    // GET /api/v1/notifications?userId=1
    @GetMapping
    @Operation(summary = "Get all notifications for a user")
    public ResponseEntity<List<Notification>> getAllNotifications(@RequestParam Long userId) {
        return ResponseEntity.ok(notificationService.getAllNotifications(userId));
    }

    // GET /api/v1/notifications/unread?userId=1
    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications for a user")
    public ResponseEntity<List<Notification>> getUnread(@RequestParam Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    // GET /api/v1/notifications/count?userId=1
    @GetMapping("/count")
    @Operation(summary = "Get count of unread notifications")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestParam Long userId) {
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.countUnread(userId)));
    }

    // PATCH /api/v1/notifications/{id}/read
    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }
}
*/

// ================================================================
// GLOBAL EXCEPTION HANDLER
// ================================================================

// ─────────────────────────────────────────
// exception/ResourceNotFoundException.java
// ─────────────────────────────────────────
/*
package com.insurepro.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
*/

// ─────────────────────────────────────────
// exception/BadRequestException.java
// ─────────────────────────────────────────
/*
package com.insurepro.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
*/

// ─────────────────────────────────────────
// exception/GlobalExceptionHandler.java
// ─────────────────────────────────────────
/*
package com.insurepro.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── 404 Not Found ───────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ── 400 Bad Request ─────────────────────────────────────────────
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ── 400 Validation errors from @Valid ───────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, msg);
    }

    // ── 401 Bad credentials ──────────────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCreds(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    // ── 403 Forbidden ────────────────────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }

    // ── 500 Catch-all ────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred. Please contact support.");
    }

    // ─── Helper ──────────────────────────────────────────────────────
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        ErrorResponse error = new ErrorResponse(
                status.value(), status.getReasonPhrase(), message, LocalDateTime.now());
        return ResponseEntity.status(status).body(error);
    }

    // ─── ErrorResponse DTO ───────────────────────────────────────────
    public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {}
}
*/
