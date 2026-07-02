package com.quickeats.paymentservice.dto;

import com.quickeats.paymentservice.entity.PaymentStatus;
import java.math.BigDecimal;

public class PaymentResponse {
    private Long id;
    private String orderId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String razorpayOrderId;
    private String razorpayPaymentId;

    public PaymentResponse() {}

    public PaymentResponse(Long id, String orderId, BigDecimal amount, PaymentStatus status, String razorpayOrderId, String razorpayPaymentId) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.razorpayOrderId = razorpayOrderId;
        this.razorpayPaymentId = razorpayPaymentId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
}
