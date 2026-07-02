package com.quickeats.paymentservice.service.impl;

import com.quickeats.paymentservice.dto.PaymentRequest;
import com.quickeats.paymentservice.dto.PaymentResponse;
import com.quickeats.paymentservice.entity.Payment;
import com.quickeats.paymentservice.entity.PaymentStatus;
import com.quickeats.paymentservice.exception.PaymentException;
import com.quickeats.paymentservice.repository.PaymentRepository;
import com.quickeats.paymentservice.service.PaymentService;
import com.quickeats.paymentservice.service.RazorpayService;
import com.quickeats.paymentservice.service.RazorpayService.RazorpayTransactionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;

    public PaymentServiceImpl(PaymentRepository paymentRepository, RazorpayService razorpayService) {
        this.paymentRepository = paymentRepository;
        this.razorpayService = razorpayService;
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        logger.info("Processing payment request for Order: {}, Amount: {}, Key: {}",
                request.getOrderId(), request.getAmount(), request.getIdempotencyKey());

        // 1. Strict Idempotency Check: Return payment response immediately if key exists
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingPayment.isPresent()) {
            logger.warn("Duplicate request detected for idempotency key: {}. Returning cached payment status.", request.getIdempotencyKey());
            return mapToResponse(existingPayment.get());
        }

        // 2. Prepare transaction
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment.setStatus(PaymentStatus.PENDING);

        // Pre-save to acquire DB lock and register transaction
        payment = paymentRepository.saveAndFlush(payment);

        // 3. Call Razorpay API Simulator
        RazorpayTransactionResult result = razorpayService.capturePayment(request.getOrderId(), request.getAmount());

        if (result.isSuccess()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setRazorpayOrderId(result.getRazorpayOrderId());
            payment.setRazorpayPaymentId(result.getRazorpayPaymentId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentException(result.getErrorMessage(), HttpStatus.BAD_REQUEST);
        }

        payment = paymentRepository.save(payment);
        return mapToResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(String orderId) {
        logger.info("Processing refund request for Order: {}", orderId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException("Payment not found for Order ID: " + orderId, HttpStatus.NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            logger.info("Payment for Order ID: {} has already been refunded.", orderId);
            return mapToResponse(payment);
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentException("Cannot refund a non-successful payment. Current status: " + payment.getStatus(), HttpStatus.BAD_REQUEST);
        }

        // Call Razorpay Refund simulation
        boolean refundSuccess = razorpayService.refundPayment(payment.getRazorpayPaymentId(), payment.getAmount());

        if (refundSuccess) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment = paymentRepository.save(payment);
            return mapToResponse(payment);
        } else {
            throw new PaymentException("Failed to process refund through payment gateway", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException("Payment not found for Order ID: " + orderId, HttpStatus.NOT_FOUND));
        return mapToResponse(payment);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId()
        );
    }
}
