package com.example.qubaatisystem;

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
import com.example.qubaatisystem.Service.EmailService;
import com.example.qubaatisystem.Service.MoyasarService;
import com.example.qubaatisystem.Service.PaymentService;
import com.example.qubaatisystem.Service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock ParentRepository parentRepository;
    @Mock TeacherRepository teacherRepository;
    @Mock SubscriptionService subscriptionService;
    @Mock MoyasarService moyasarService;
    @Mock EmailService emailService;
    @Mock SecurityOwnershipService security;

    @InjectMocks PaymentService paymentService;

    @BeforeEach
    void setup() {
        // Inject @Value fields that @InjectMocks cannot set
        ReflectionTestUtils.setField(paymentService, "publishableKey", "pk_test_placeholder");
        ReflectionTestUtils.setField(paymentService, "appBaseUrl", "http://localhost:8080");
    }

    // ── checkout ──────────────────────────────────────────────────────────────

    @Test
    void checkout_throwsApiException_whenSecretKeyNotConfigured() {
        doThrow(new ApiException("Payment gateway is not configured."))
                .when(moyasarService).requireConfigured();

        assertThatThrownBy(() -> paymentService.checkout())
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void checkout_throwsApiException_whenPublishableKeyMissing() {
        ReflectionTestUtils.setField(paymentService, "publishableKey", "");
        doNothing().when(moyasarService).requireConfigured();

        assertThatThrownBy(() -> paymentService.checkout())
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("publishable key");
    }

    @Test
    void checkout_throwsAccessDenied_whenCallerIsAdmin() {
        doNothing().when(moyasarService).requireConfigured();
        when(security.getCurrentUser()).thenReturn(makeUser(UserRole.ADMIN));

        assertThatThrownBy(() -> paymentService.checkout())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void checkout_throwsApiException_whenParentHasNoProfile() {
        doNothing().when(moyasarService).requireConfigured();
        when(security.getCurrentUser()).thenReturn(makeUser(UserRole.PARENT));
        when(parentRepository.findParentByUserId(42)).thenReturn(null);

        assertThatThrownBy(() -> paymentService.checkout())
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No parent profile");
    }

    @Test
    void checkout_throwsApiException_whenTeacherHasNoProfile() {
        doNothing().when(moyasarService).requireConfigured();
        when(security.getCurrentUser()).thenReturn(makeUser(UserRole.TEACHER));
        when(teacherRepository.findTeacherByUserId(42)).thenReturn(null);

        assertThatThrownBy(() -> paymentService.checkout())
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No teacher profile");
    }

    @Test
    void checkout_throwsApiException_whenPlanIsMarkedFree() {
        doNothing().when(moyasarService).requireConfigured();
        when(security.getCurrentUser()).thenReturn(makeUser(UserRole.PARENT));
        Parent parent = new Parent();
        parent.setId(1);
        when(parentRepository.findParentByUserId(42)).thenReturn(parent);
        when(subscriptionService.findPlanByAudienceAndCode(PlanAudience.PARENT, "PARENT_PLUS"))
                .thenReturn(makePlan("PARENT_PLUS", PlanAudience.PARENT, 0));

        assertThatThrownBy(() -> paymentService.checkout())
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("free");
    }

    @Test
    void checkout_returnsCheckoutOutDTO_forAuthenticatedParent() {
        doNothing().when(moyasarService).requireConfigured();
        when(security.getCurrentUser()).thenReturn(makeUser(UserRole.PARENT));
        Parent parent = new Parent();
        parent.setId(1);
        when(parentRepository.findParentByUserId(42)).thenReturn(parent);
        when(subscriptionService.findPlanByAudienceAndCode(PlanAudience.PARENT, "PARENT_PLUS"))
                .thenReturn(makePlan("PARENT_PLUS", PlanAudience.PARENT, 5000));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutOutDTO result = paymentService.checkout();

        assertThat(result.getLocalReference()).isNotNull().isNotBlank();
        assertThat(result.getAmount()).isEqualTo(5000);
        assertThat(result.getCurrency()).isEqualTo("SAR");
        assertThat(result.getPublishableKey()).isEqualTo("pk_test_placeholder");
        assertThat(result.getCallbackUrl()).doesNotContain("ref=");
    }

    @Test
    void checkout_returnsCheckoutOutDTO_forAuthenticatedTeacher() {
        doNothing().when(moyasarService).requireConfigured();
        when(security.getCurrentUser()).thenReturn(makeUser(UserRole.TEACHER));
        Teacher teacher = new Teacher();
        teacher.setId(5);
        when(teacherRepository.findTeacherByUserId(42)).thenReturn(teacher);
        when(subscriptionService.findPlanByAudienceAndCode(PlanAudience.TEACHER, "TEACHER_PRO"))
                .thenReturn(makePlan("TEACHER_PRO", PlanAudience.TEACHER, 5000));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutOutDTO result = paymentService.checkout();

        assertThat(result.getLocalReference()).isNotNull().isNotBlank();
        assertThat(result.getAmount()).isEqualTo(5000);
        assertThat(result.getCurrency()).isEqualTo("SAR");
        assertThat(result.getPublishableKey()).isEqualTo("pk_test_placeholder");
        assertThat(result.getCallbackUrl()).doesNotContain("ref=");
    }

    // ── handleCallback — success path ─────────────────────────────────────────

    @Test
    void handleCallback_activatesSubscription_whenAllChecksPass() {
        String localRef  = "ref-success-001";
        String moyasarId = "mysr-id-success-001";

        Payment payment = makePayment(localRef, PaymentStatus.PENDING, 5000);
        when(moyasarService.fetchPayment(moyasarId))
                .thenReturn(makeMoyasarData(moyasarId, localRef, "paid", 5000, "SAR"));
        when(paymentRepository.findByLocalReferenceForUpdate(localRef)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByMoyasarPaymentId(moyasarId)).thenReturn(Optional.empty());
        when(subscriptionService.activateOrExtendSubscription(any(Payment.class)))
                .thenReturn(new Subscription());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.handleCallback(moyasarId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getMoyasarPaymentId()).isEqualTo(moyasarId);
        assertThat(payment.getPaidAt()).isNotNull();
        verify(subscriptionService).activateOrExtendSubscription(payment);
    }

    @Test
    void handleCallback_delegatesExtension_whenOwnerAlreadyHasActiveSubscription() {
        // A second successful payment for the same owner must call activateOrExtendSubscription
        // so the existing Subscription row is extended rather than a new one created.
        String localRef  = "ref-renew-001";
        String moyasarId = "mysr-id-renew-001";

        Payment payment = makePayment(localRef, PaymentStatus.PENDING, 5000);
        Subscription existingSub = new Subscription();
        existingSub.setStatus(com.example.qubaatisystem.Enum.SubscriptionStatus.ACTIVE);

        when(moyasarService.fetchPayment(moyasarId))
                .thenReturn(makeMoyasarData(moyasarId, localRef, "paid", 5000, "SAR"));
        when(paymentRepository.findByLocalReferenceForUpdate(localRef)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByMoyasarPaymentId(moyasarId)).thenReturn(Optional.empty());
        // activateOrExtendSubscription returns the existing (extended) subscription
        when(subscriptionService.activateOrExtendSubscription(any(Payment.class)))
                .thenReturn(existingSub);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.handleCallback(moyasarId);

        // The payment must be linked to the same subscription object that was returned
        assertThat(payment.getSubscription()).isSameAs(existingSub);
        // Exactly one call — no separate new-subscription creation path
        verify(subscriptionService).activateOrExtendSubscription(payment);
    }

    // ── handleCallback — idempotency ──────────────────────────────────────────

    @Test
    void handleCallback_isIdempotent_whenPaymentAlreadyPaidWithSameMoyasarId() {
        String localRef  = "ref-idem-001";
        String moyasarId = "mysr-id-idem-001";

        Payment payment = makePayment(localRef, PaymentStatus.PAID, 5000);
        payment.setMoyasarPaymentId(moyasarId);  // already stored from first processing

        when(moyasarService.fetchPayment(moyasarId))
                .thenReturn(makeMoyasarData(moyasarId, localRef, "paid", 5000, "SAR"));
        when(paymentRepository.findByLocalReferenceForUpdate(localRef)).thenReturn(Optional.of(payment));

        paymentService.handleCallback(moyasarId);

        verify(paymentRepository, never()).save(any());
    }

    // ── handleCallback — rejection cases ─────────────────────────────────────

    @Test
    void handleCallback_marksPaymentFailed_whenMoyasarStatusIsFailed() {
        String localRef  = "ref-failed-001";
        String moyasarId = "mysr-id-failed-001";

        Payment payment = makePayment(localRef, PaymentStatus.PENDING, 5000);
        when(moyasarService.fetchPayment(moyasarId))
                .thenReturn(makeMoyasarData(moyasarId, localRef, "failed", 5000, "SAR"));
        when(paymentRepository.findByLocalReferenceForUpdate(localRef)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.handleCallback(moyasarId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void handleCallback_marksPaymentFailed_whenAmountMismatch() {
        String localRef  = "ref-amount-001";
        String moyasarId = "mysr-id-amount-001";

        Payment payment = makePayment(localRef, PaymentStatus.PENDING, 5000);
        when(moyasarService.fetchPayment(moyasarId))
                .thenReturn(makeMoyasarData(moyasarId, localRef, "paid", 100, "SAR"));  // wrong amount
        when(paymentRepository.findByLocalReferenceForUpdate(localRef)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.handleCallback(moyasarId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureMessage()).containsIgnoringCase("mismatch");
    }

    @Test
    void handleCallback_marksPaymentFailed_whenCurrencyMismatch() {
        String localRef  = "ref-currency-001";
        String moyasarId = "mysr-id-currency-001";

        Payment payment = makePayment(localRef, PaymentStatus.PENDING, 5000);  // currency SAR
        when(moyasarService.fetchPayment(moyasarId))
                .thenReturn(makeMoyasarData(moyasarId, localRef, "paid", 5000, "USD"));  // wrong currency
        when(paymentRepository.findByLocalReferenceForUpdate(localRef)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.handleCallback(moyasarId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureMessage()).containsIgnoringCase("currency");
    }

    @Test
    void handleCallback_throws_whenMetadataLocalReferenceIsMissing() {
        String moyasarId = "mysr-id-no-meta-001";

        Map<String, Object> dataWithoutMeta = new HashMap<>();
        dataWithoutMeta.put("id", moyasarId);
        dataWithoutMeta.put("status", "paid");
        dataWithoutMeta.put("amount", 5000);
        dataWithoutMeta.put("currency", "SAR");
        // no "metadata" key

        when(moyasarService.fetchPayment(moyasarId)).thenReturn(dataWithoutMeta);

        assertThatThrownBy(() -> paymentService.handleCallback(moyasarId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("localReference");
    }

    @Test
    void handleCallback_throws_whenMetadataLocalReferenceNotFoundLocally() {
        String moyasarId = "mysr-id-unknown-ref-001";
        String unknownRef = "ref-does-not-exist";

        when(moyasarService.fetchPayment(moyasarId))
                .thenReturn(makeMoyasarData(moyasarId, unknownRef, "paid", 5000, "SAR"));
        when(paymentRepository.findByLocalReferenceForUpdate(unknownRef))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.handleCallback(moyasarId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void handleCallback_throws_whenDuplicateMoyasarPaymentIdLinkedToDifferentPayment() {
        String localRef  = "ref-dup-001";
        String moyasarId = "mysr-id-dup-001";

        Payment payment = makePayment(localRef, PaymentStatus.PENDING, 5000);
        Payment anotherPayment = makePayment("ref-other-001", PaymentStatus.PAID, 5000);
        anotherPayment.setMoyasarPaymentId(moyasarId);  // already used by a different payment

        when(moyasarService.fetchPayment(moyasarId))
                .thenReturn(makeMoyasarData(moyasarId, localRef, "paid", 5000, "SAR"));
        when(paymentRepository.findByLocalReferenceForUpdate(localRef)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByMoyasarPaymentId(moyasarId)).thenReturn(Optional.of(anotherPayment));

        assertThatThrownBy(() -> paymentService.handleCallback(moyasarId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already linked");
    }

    @Test
    void handleCallback_throws_whenPaymentStatusIsFailedOrCancelled() {
        String localRef  = "ref-terminal-001";
        String moyasarId = "mysr-id-terminal-001";

        Payment payment = makePayment(localRef, PaymentStatus.FAILED, 5000);
        when(moyasarService.fetchPayment(moyasarId))
                .thenReturn(makeMoyasarData(moyasarId, localRef, "paid", 5000, "SAR"));
        when(paymentRepository.findByLocalReferenceForUpdate(localRef)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.handleCallback(moyasarId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("terminal");
    }

    // ── getStatus / getReceipt — ownership ───────────────────────────────────

    @Test
    void getStatus_callsOwnershipCheck_andSucceeds_forOwningParent() {
        Payment payment = makePayment("ref-owner-status", PaymentStatus.PAID, 5000);
        payment.setPaidAt(LocalDateTime.now());
        when(paymentRepository.findByLocalReference("ref-owner-status")).thenReturn(Optional.of(payment));
        // security mock does nothing by default — ownership passes

        PaymentStatusOutDTO result = paymentService.getStatus("ref-owner-status");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(security).assertCurrentOwnsPaymentOrAdmin(payment);
    }

    @Test
    void getReceipt_callsOwnershipCheck_andSucceeds_forOwningParent() {
        String localRef = "ref-owner-receipt";
        LocalDateTime paidAt = LocalDateTime.of(2026, 6, 22, 12, 0);

        Parent parent = new Parent();
        parent.setId(7);

        Subscription sub = new Subscription();
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartsAt(paidAt);
        sub.setEndsAt(paidAt.plusDays(30));

        Payment payment = makePayment(localRef, PaymentStatus.PAID, 5000);
        payment.setParent(parent);
        payment.setPaidAt(paidAt);
        payment.setSubscription(sub);

        when(paymentRepository.findByLocalReference(localRef)).thenReturn(Optional.of(payment));
        // security mock does nothing by default — ownership passes

        paymentService.getReceipt(localRef);

        verify(security).assertCurrentOwnsPaymentOrAdmin(payment);
    }

    @Test
    void getStatus_throwsAccessDenied_whenDifferentUserChecksStatus() {
        Payment payment = makePayment("ref-denied-status", PaymentStatus.PAID, 5000);
        when(paymentRepository.findByLocalReference("ref-denied-status")).thenReturn(Optional.of(payment));
        doThrow(new AccessDeniedException("You may only access your own parent data"))
                .when(security).assertCurrentOwnsPaymentOrAdmin(any(Payment.class));

        assertThatThrownBy(() -> paymentService.getStatus("ref-denied-status"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("own parent data");
    }

    @Test
    void getStatus_succeedsForAdmin_whenOwnershipCheckPasses() {
        Payment payment = makePayment("ref-admin-status", PaymentStatus.PAID, 5000);
        payment.setPaidAt(LocalDateTime.now());
        when(paymentRepository.findByLocalReference("ref-admin-status")).thenReturn(Optional.of(payment));
        doNothing().when(security).assertCurrentOwnsPaymentOrAdmin(any(Payment.class));

        PaymentStatusOutDTO result = paymentService.getStatus("ref-admin-status");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(security).assertCurrentOwnsPaymentOrAdmin(payment);
    }

    @Test
    void getStatus_throwsApiException_notNpe_whenPaymentHasNoOwner() {
        // Payment with neither parent nor teacher — assertCurrentOwnsPaymentOrAdmin must throw
        // ApiException("Payment has no subscriber owner"), never NullPointerException.
        Payment payment = makePayment("ref-no-owner", PaymentStatus.PENDING, 5000);
        // makePayment leaves parent and teacher null by default
        when(paymentRepository.findByLocalReference("ref-no-owner")).thenReturn(Optional.of(payment));
        doThrow(new ApiException("Payment has no subscriber owner"))
                .when(security).assertCurrentOwnsPaymentOrAdmin(any(Payment.class));

        assertThatThrownBy(() -> paymentService.getStatus("ref-no-owner"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no subscriber owner");
    }

    // ── getStatus ─────────────────────────────────────────────────────────────

    @Test
    void getStatus_returnsPaymentStatusOutDTO_withCorrectFields() {
        Payment payment = makePayment("ref-xyz", PaymentStatus.PAID, 5000);
        payment.setPaidAt(LocalDateTime.now());
        when(paymentRepository.findByLocalReference("ref-xyz")).thenReturn(Optional.of(payment));

        PaymentStatusOutDTO result = paymentService.getStatus("ref-xyz");

        assertThat(result.getLocalReference()).isEqualTo("ref-xyz");
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(result.getAmount()).isEqualTo(5000);
    }

    @Test
    void getStatus_throwsApiException_whenPaymentNotFound() {
        when(paymentRepository.findByLocalReference("missing-ref")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getStatus("missing-ref"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Payment not found");
    }

    // ── handleCallback — email confirmation ───────────────────────────────────

    @Test
    void handleCallback_sendsEmailConfirmation_afterSuccessfulPayment() {
        String localRef  = "ref-email-success-001";
        String moyasarId = "mysr-id-email-success-001";

        Payment payment = makePayment(localRef, PaymentStatus.PENDING, 5000);
        when(moyasarService.fetchPayment(moyasarId))
                .thenReturn(makeMoyasarData(moyasarId, localRef, "paid", 5000, "SAR"));
        when(paymentRepository.findByLocalReferenceForUpdate(localRef)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByMoyasarPaymentId(moyasarId)).thenReturn(Optional.empty());
        when(subscriptionService.activateOrExtendSubscription(any(Payment.class)))
                .thenReturn(new Subscription());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.handleCallback(moyasarId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(emailService).sendPaymentConfirmation(payment);
    }

    @Test
    void handleCallback_doesNotSendEmail_whenPaymentStatusIsFailed() {
        String localRef  = "ref-email-failed-001";
        String moyasarId = "mysr-id-email-failed-001";

        Payment payment = makePayment(localRef, PaymentStatus.PENDING, 5000);
        when(moyasarService.fetchPayment(moyasarId))
                .thenReturn(makeMoyasarData(moyasarId, localRef, "failed", 5000, "SAR"));
        when(paymentRepository.findByLocalReferenceForUpdate(localRef)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.handleCallback(moyasarId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(emailService, never()).sendPaymentConfirmation(any());
    }

    @Test
    void handleCallback_doesNotFailPayment_whenEmailServiceThrows() {
        String localRef  = "ref-email-throw-001";
        String moyasarId = "mysr-id-email-throw-001";

        Payment payment = makePayment(localRef, PaymentStatus.PENDING, 5000);
        when(moyasarService.fetchPayment(moyasarId))
                .thenReturn(makeMoyasarData(moyasarId, localRef, "paid", 5000, "SAR"));
        when(paymentRepository.findByLocalReferenceForUpdate(localRef)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByMoyasarPaymentId(moyasarId)).thenReturn(Optional.empty());
        when(subscriptionService.activateOrExtendSubscription(any(Payment.class)))
                .thenReturn(new Subscription());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP connection refused"))
                .when(emailService).sendPaymentConfirmation(any(Payment.class));

        // Must not throw — payment flow is protected from email failures
        paymentService.handleCallback(moyasarId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(subscriptionService).activateOrExtendSubscription(payment);
    }

    // ── getReceipt ────────────────────────────────────────────────────────────

    @Test
    void getReceipt_returnsPaidReceipt_withCorrectFields() {
        String localRef = "ref-receipt-001";
        LocalDateTime paidAt = LocalDateTime.of(2026, 6, 21, 12, 0);

        Parent parent = new Parent();
        parent.setId(7);

        Subscription sub = new Subscription();
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartsAt(paidAt);
        sub.setEndsAt(paidAt.plusDays(30));

        Payment payment = makePayment(localRef, PaymentStatus.PAID, 5000);
        payment.setParent(parent);
        payment.setPaidAt(paidAt);
        payment.setMoyasarPaymentId("mysr-receipt-001");
        payment.setSubscription(sub);

        when(paymentRepository.findByLocalReference(localRef)).thenReturn(Optional.of(payment));

        PaymentReceiptOutDTO result = paymentService.getReceipt(localRef);

        assertThat(result.getTitle()).isEqualTo("Subscription Payment Receipt");
        assertThat(result.getReceiptReference()).isEqualTo(localRef);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(result.getAmountHalalas()).isEqualTo(5000);
        assertThat(result.getAmountSar()).isEqualTo("50.00 SAR");
        assertThat(result.getCurrency()).isEqualTo("SAR");
        assertThat(result.getSubscriberType()).isEqualTo(PlanAudience.PARENT);
        assertThat(result.getSubscriberId()).isEqualTo(7);
        assertThat(result.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(result.getSubscriptionEndsAt()).isEqualTo(paidAt.plusDays(30));
    }

    @Test
    void getReceipt_throwsApiException_whenPaymentIsNotPaid() {
        String localRef = "ref-pending-receipt";
        Payment payment = makePayment(localRef, PaymentStatus.PENDING, 5000);

        when(paymentRepository.findByLocalReference(localRef)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getReceipt(localRef))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("completed payments");
    }

    // ---------- helpers ----------

    private SubscriptionPlan makePlan(String code, PlanAudience audience, int price) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setCode(code);
        plan.setName(code);
        plan.setAudience(audience);
        plan.setPriceAmount(price);
        plan.setCurrency("SAR");
        plan.setDurationDays(30);
        plan.setActive(true);
        return plan;
    }

    private Payment makePayment(String localRef, PaymentStatus status, int amount) {
        Payment payment = new Payment();
        payment.setLocalReference(localRef);
        payment.setPlan(makePlan("PARENT_PLUS", PlanAudience.PARENT, amount));
        payment.setAmount(amount);
        payment.setCurrency("SAR");
        payment.setStatus(status);
        payment.setCreatedAt(LocalDateTime.now());
        return payment;
    }

    private User makeUser(UserRole role) {
        User user = new User();
        user.setId(42);
        user.setRole(role);
        return user;
    }

    /** Builds a simulated Moyasar payment-fetch response with all required fields. */
    private Map<String, Object> makeMoyasarData(String id, String localRef,
                                                 String status, int amount, String currency) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("localReference", localRef);

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("status", status);
        data.put("amount", amount);
        data.put("currency", currency);
        data.put("metadata", metadata);
        return data;
    }
}
