package com.quickeats.paymentservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service simulating interaction with Razorpay API (Test Mode).
 * Automatically processes transactions as successful except for special test case triggers.
 */
@Service
public class RazorpayService {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayService.class);

    @Value("${razorpay.key.id}")
    private String keyId;

    /**
     * Simulates payment capture.
     * Always returns a successful payment transaction unless the amount is exactly 9999.99
     * (which triggers a simulated gateway rejection to verify Saga compensating transactions).
     */
    public RazorpayTransactionResult capturePayment(String orderId, BigDecimal amount) {
        logger.info("Initiating Razorpay capture simulation (KeyId: {}) for Order ID: {} with amount: {}", keyId, orderId, amount);

        // Special test amount that triggers a payment processing failure
        if (amount.compareTo(new BigDecimal("9999.99")) == 0) {
            logger.warn("Simulated Razorpay transaction DECLINED for testing compensating saga flow.");
            return new RazorpayTransactionResult(false, null, null, "Razorpay Card Declined: Insufficient Funds");
        }

        // Simulating network delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String razorpayOrderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        String razorpayPaymentId = "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        logger.info("Razorpay transaction APPROVED. Order ID: {}, Payment ID: {}", razorpayOrderId, razorpayPaymentId);
        return new RazorpayTransactionResult(true, razorpayOrderId, razorpayPaymentId, null);
    }

    /**
     * Simulates payment refund on Razorpay.
     */
    public boolean refundPayment(String razorpayPaymentId, BigDecimal amount) {
        logger.info("Initiating Razorpay refund simulation for Payment ID: {} with amount: {}", razorpayPaymentId, amount);
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("Razorpay refund processed successfully for Payment ID: {}", razorpayPaymentId);
        return true;
    }

    // Inner helper class to hold transaction outcomes
    public static class RazorpayTransactionResult {
        private final boolean success;
        private final String razorpayOrderId;
        private final String razorpayPaymentId;
        private final String errorMessage;

        public RazorpayTransactionResult(boolean success, String razorpayOrderId, String razorpayPaymentId, String errorMessage) {
            this.success = success;
            this.razorpayOrderId = razorpayOrderId;
            this.razorpayPaymentId = razorpayPaymentId;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public String getRazorpayOrderId() { return razorpayOrderId; }
        public String getRazorpayPaymentId() { return razorpayPaymentId; }
        public String getErrorMessage() { return errorMessage; }
    }
}
