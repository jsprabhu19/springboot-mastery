package com.quickeats.paymentservice.service;

import com.quickeats.paymentservice.dto.PaymentRequest;
import com.quickeats.paymentservice.dto.PaymentResponse;

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest request);
    PaymentResponse refundPayment(String orderId);
    PaymentResponse getPaymentByOrderId(String orderId);
}
