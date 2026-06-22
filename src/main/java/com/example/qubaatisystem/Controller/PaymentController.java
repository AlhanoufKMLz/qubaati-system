package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.DTO.In.CheckoutInDTO;
import com.example.qubaatisystem.Enum.PlanAudience;
import com.example.qubaatisystem.Security.SecurityOwnershipService;
import com.example.qubaatisystem.Service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final SecurityOwnershipService security;

    // A subscriber may only check out for THEMSELVES (admin may check out for anyone).
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutInDTO dto) {
        if (dto != null && dto.getSubscriberType() == PlanAudience.TEACHER) {
            security.assertCurrentTeacherOrAdmin(dto.getSubscriberId());
        } else if (dto != null) {
            security.assertCurrentParentOrAdmin(dto.getSubscriberId());
        }
        return ResponseEntity.ok(paymentService.checkout(dto));
    }

    // Moyasar redirects here after payment; id is the Moyasar payment id.
    // localReference is read from metadata inside PaymentService — not from query params.
    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam String id) {
        paymentService.handleCallback(id);
        return ResponseEntity.ok("Payment processed");
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(@RequestParam String ref) {
        return ResponseEntity.ok(paymentService.getStatus(ref));
    }

    @GetMapping("/receipt")
    public ResponseEntity<?> receipt(@RequestParam String ref) {
        return ResponseEntity.ok(paymentService.getReceipt(ref));
    }
}
