package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.Out.CheckoutOutDTO;
import com.example.qubaatisystem.DTO.Out.PaymentReceiptOutDTO;
import com.example.qubaatisystem.DTO.Out.PaymentStatusOutDTO;
import com.example.qubaatisystem.Enum.PaymentStatus;
import com.example.qubaatisystem.Enum.PlanAudience;
import com.example.qubaatisystem.Enum.SubscriptionStatus;
import com.example.qubaatisystem.Enum.UserRole;
import com.example.qubaatisystem.Model.Parent;
import com.example.qubaatisystem.Model.Payment;
import com.example.qubaatisystem.Model.Subscription;
import com.example.qubaatisystem.Model.SubscriptionPlan;
import com.example.qubaatisystem.Model.Teacher;
import com.example.qubaatisystem.Model.User;
import com.example.qubaatisystem.Repository.ParentRepository;
import com.example.qubaatisystem.Repository.PaymentRepository;
import com.example.qubaatisystem.Repository.TeacherRepository;
import com.example.qubaatisystem.Security.SecurityOwnershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    @Value("${moyasar.publishable-key:}")
    private String publishableKey;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    private final PaymentRepository paymentRepository;
    private final ParentRepository parentRepository;
    private final TeacherRepository teacherRepository;
    private final SubscriptionService subscriptionService;
    private final MoyasarService moyasarService;
    private final EmailService emailService;
    private final SecurityOwnershipService security;

    // ── Checkout ─────────────────────────────────────────────────────────────

    /**
     * Creates a PENDING payment record and returns everything the frontend needs
     * to initialise the Moyasar payment form.
     * Requires both MOYASAR_SECRET_KEY and MOYASAR_PUBLISHABLE_KEY to be set.
     * The localReference must be passed as metadata.localReference in the form so
     * that the callback can read it back from Moyasar without relying on query params.
     */
    @Transactional
    public CheckoutOutDTO checkout() {
        moyasarService.requireConfigured();
        if (publishableKey == null || publishableKey.isBlank()) {
            throw new ApiException("Payment gateway publishable key is not configured. Please contact support.");
        }

        User currentUser = security.getCurrentUser();
        Parent parent = null;
        Teacher teacher = null;
        PlanAudience audience;
        String planCode;

        if (currentUser.getRole() == UserRole.PARENT) {
            parent = parentRepository.findParentByUserId(currentUser.getId());
            if (parent == null) {
                throw new ApiException("No parent profile found for current user.");
            }
            audience = PlanAudience.PARENT;
            planCode = "PARENT_PLUS";
        } else if (currentUser.getRole() == UserRole.TEACHER) {
            teacher = teacherRepository.findTeacherByUserId(currentUser.getId());
            if (teacher == null) {
                throw new ApiException("No teacher profile found for current user.");
            }
            audience = PlanAudience.TEACHER;
            planCode = "TEACHER_PRO";
        } else {
            throw new AccessDeniedException("Only parents and teachers may initiate a subscription.");
        }

        SubscriptionPlan plan = subscriptionService.findPlanByAudienceAndCode(audience, planCode);

        if (plan.getPriceAmount() == null || plan.getPriceAmount() == 0) {
            throw new ApiException("Plan " + planCode + " is free and does not require payment.");
        }

        String localRef = UUID.randomUUID().toString();
        // Callback URL does not carry ref — localReference travels through Moyasar metadata
        String callbackUrl = appBaseUrl + "/api/v1/payments/callback";

        Payment payment = new Payment();
        payment.setLocalReference(localRef);
        payment.setPlan(plan);
        payment.setParent(parent);
        payment.setTeacher(teacher);
        payment.setAmount(plan.getPriceAmount());
        payment.setCurrency(plan.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return new CheckoutOutDTO(localRef, publishableKey, plan.getName(),
                plan.getPriceAmount(), plan.getCurrency(), callbackUrl);
    }

    // ── Callback ─────────────────────────────────────────────────────────────

    /**
     * Called by Moyasar after a payment attempt (GET /api/v1/payments/callback?id={moyasarPaymentId}).
     *
     * Flow:
     *  1. Fetch full payment details from Moyasar (network call BEFORE acquiring the DB lock).
     *  2. Confirm the fetched payment id matches the callback id.
     *  3. Read metadata.localReference — reject if absent or blank.
     *  4. Acquire pessimistic write lock on the local Payment row.
     *  5. Idempotency: if already PAID with matching moyasarPaymentId, return early.
     *  6. Reject FAILED / CANCELLED payments (terminal states cannot be reactivated).
     *  7. Verify Moyasar status, amount, currency, and metadata reference.
     *  8. Guard against a duplicate moyasarPaymentId already linked to another row.
     *  9. Activate the subscription.
     */
    @Transactional
    public void handleCallback(String moyasarPaymentId) {
        // 1. Fetch from Moyasar before touching the DB lock
        Map<String, Object> moyasarData = moyasarService.fetchPayment(moyasarPaymentId);
        if (moyasarData == null) {
            log.warn("Callback ignored — could not fetch Moyasar payment id={}", moyasarPaymentId);
            return;
        }

        // 2. Confirm fetched id matches what arrived in the callback
        String fetchedId = String.valueOf(moyasarData.get("id"));
        if (!moyasarPaymentId.equals(fetchedId)) {
            log.warn("Callback rejected — fetched id={} does not match callback id={}", fetchedId, moyasarPaymentId);
            throw new ApiException("Provider payment id mismatch.");
        }

        // 3. Read localReference from Moyasar metadata — must be present and non-blank
        String localReference = extractLocalReference(moyasarData);
        if (localReference == null) {
            log.warn("Callback rejected — metadata.localReference missing for moyasar id={}", moyasarPaymentId);
            throw new ApiException("Missing or blank localReference in payment metadata.");
        }

        // 4. Acquire pessimistic write lock on the Payment row
        Payment payment = paymentRepository.findByLocalReferenceForUpdate(localReference)
                .orElseThrow(() -> new ApiException("Payment not found for ref: " + localReference));

        // 5. Idempotency
        if (payment.getStatus() == PaymentStatus.PAID) {
            if (moyasarPaymentId.equals(payment.getMoyasarPaymentId())) {
                log.info("Callback idempotent — payment already paid ref={}", localReference);
                return;
            }
            log.warn("Callback rejected — PAID payment ref={} received a different moyasarPaymentId", localReference);
            throw new ApiException("Payment already processed with a different provider id.");
        }

        // 6. FAILED / CANCELLED are terminal — never reactivate
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Callback rejected — payment ref={} is in terminal status {}", localReference, payment.getStatus());
            throw new ApiException("Payment is in a terminal state and cannot be activated: " + payment.getStatus());
        }

        // 7a. Moyasar status
        String moyasarStatus = String.valueOf(moyasarData.get("status"));
        if (!"paid".equalsIgnoreCase(moyasarStatus)) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureMessage("Moyasar status: " + moyasarStatus);
            paymentRepository.save(payment);
            return;
        }

        // 7b. Amount
        Object amountObj = moyasarData.get("amount");
        Integer moyasarAmount = amountObj != null ? ((Number) amountObj).intValue() : null;
        if (!payment.getAmount().equals(moyasarAmount)) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureMessage("Amount mismatch: expected " + payment.getAmount() + ", got " + moyasarAmount);
            paymentRepository.save(payment);
            return;
        }

        // 7c. Currency (case-insensitive)
        String fetchedCurrency = String.valueOf(moyasarData.get("currency"));
        if (!payment.getCurrency().equalsIgnoreCase(fetchedCurrency)) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureMessage("Currency mismatch: expected " + payment.getCurrency() + ", got " + fetchedCurrency);
            paymentRepository.save(payment);
            return;
        }

        // 7d. Metadata localReference matches stored localReference (defensive)
        if (!localReference.equals(payment.getLocalReference())) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureMessage("localReference in metadata does not match stored payment reference");
            paymentRepository.save(payment);
            return;
        }

        // 8. Guard against duplicate moyasarPaymentId linked to a different local payment
        Optional<Payment> existingByMoyasarId = paymentRepository.findByMoyasarPaymentId(moyasarPaymentId);
        if (existingByMoyasarId.isPresent()
                && !existingByMoyasarId.get().getLocalReference().equals(localReference)) {
            log.warn("Callback rejected — moyasarPaymentId={} already linked to a different payment", moyasarPaymentId);
            throw new ApiException("Provider payment id already linked to another transaction.");
        }

        // 9. All checks passed — activate or extend existing subscription
        payment.setMoyasarPaymentId(moyasarPaymentId);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());

        Subscription subscription = subscriptionService.activateOrExtendSubscription(payment);
        payment.setSubscription(subscription);
        paymentRepository.save(payment);

        log.info("Payment activated — ref={} plan={}", localReference, payment.getPlan().getCode());

        try {
            emailService.sendPaymentConfirmation(payment);
        } catch (Exception e) {
            log.warn("Payment confirmation email could not be sent for ref={}: {}", localReference, e.getMessage());
        }
    }

    // ── Status ───────────────────────────────────────────────────────────────

    public PaymentStatusOutDTO getStatus(String localReference) {
        Payment payment = paymentRepository.findByLocalReference(localReference)
                .orElseThrow(() -> new ApiException("Payment not found: " + localReference));
        security.assertCurrentOwnsPaymentOrAdmin(payment);
        return new PaymentStatusOutDTO(
                payment.getLocalReference(),
                payment.getStatus(),
                payment.getPlan().getName(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaidAt(),
                payment.getFailureMessage());
    }

    // ── Receipt ──────────────────────────────────────────────────────────────

    /**
     * Returns a receipt for a completed payment.
     * Only available when Payment status is PAID.
     */
    public PaymentReceiptOutDTO getReceipt(String localReference) {
        Payment payment = paymentRepository.findByLocalReference(localReference)
                .orElseThrow(() -> new ApiException("Payment not found: " + localReference));
        security.assertCurrentOwnsPaymentOrAdmin(payment);

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new ApiException(
                    "Receipt is only available for completed payments. Current status: " + payment.getStatus());
        }

        PlanAudience subscriberType;
        Integer subscriberId;
        if (payment.getParent() != null) {
            subscriberType = PlanAudience.PARENT;
            subscriberId = payment.getParent().getId();
        } else {
            subscriberType = PlanAudience.TEACHER;
            subscriberId = payment.getTeacher().getId();
        }

        String amountSar = String.format("%.2f SAR", payment.getAmount() / 100.0);

        Subscription sub = payment.getSubscription();
        SubscriptionStatus subStatus   = sub != null ? sub.getStatus()   : null;
        LocalDateTime subStartsAt      = sub != null ? sub.getStartsAt() : null;
        LocalDateTime subEndsAt        = sub != null ? sub.getEndsAt()   : null;

        return new PaymentReceiptOutDTO(
                "Subscription Payment Receipt",
                payment.getLocalReference(),
                payment.getStatus(),
                payment.getPlan().getCode(),
                payment.getPlan().getName(),
                payment.getAmount(),
                amountSar,
                payment.getCurrency(),
                payment.getPaidAt(),
                payment.getMoyasarPaymentId(),
                subscriberType,
                subscriberId,
                subStatus,
                subStartsAt,
                subEndsAt);
    }

    // ---------- helpers ----------

    @SuppressWarnings("unchecked")
    private String extractLocalReference(Map<String, Object> moyasarData) {
        Object metadataObj = moyasarData.get("metadata");
        if (!(metadataObj instanceof Map)) return null;
        Object ref = ((Map<String, Object>) metadataObj).get("localReference");
        if (ref == null) return null;
        String refStr = String.valueOf(ref);
        return refStr.isBlank() ? null : refStr;
    }

}
