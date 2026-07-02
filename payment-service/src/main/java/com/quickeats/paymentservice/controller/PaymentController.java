package com.quickeats.paymentservice.controller;

import com.quickeats.paymentservice.dto.PaymentRequest;
import com.quickeats.paymentservice.dto.PaymentResponse;
import com.quickeats.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse processPayment(@Valid @RequestBody PaymentRequest request) {
        return paymentService.processPayment(request);
    }

    @PostMapping("/refund")
    public PaymentResponse refundPayment(@RequestParam("orderId") String orderId) {
        return paymentService.refundPayment(orderId);
    }

    @GetMapping("/order/{orderId}")
    public PaymentResponse getPaymentByOrderId(@PathVariable("orderId") String orderId) {
        return paymentService.getPaymentByOrderId(orderId);
    }
}
