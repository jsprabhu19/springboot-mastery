package com.quickeats.notificationservice.service.impl;

import com.quickeats.notificationservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class ResendEmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(ResendEmailServiceImpl.class);

    private final String apiKey;
    private final String fromEmail;
    private final RestTemplate restTemplate;

    public ResendEmailServiceImpl(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from-email:QuickEats <onboarding@resend.dev>}") String fromEmail) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void sendEmail(String to, String subject, String bodyHtml) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("PLACEHOLDER") || apiKey.contains("RESEND_API_KEY")) {
            logger.info("==========================================================================");
            logger.info("RESEND API KEY NOT SPECIFIED. LOGGING NOTIFICATION (SIMULATION MODE):");
            logger.info("TO: {}", to);
            logger.info("FROM: {}", fromEmail);
            logger.info("SUBJECT: {}", subject);
            logger.info("BODY: {}", bodyHtml);
            logger.info("==========================================================================");
            return;
        }

        logger.info("Sending live email via Resend to: {}", to);
        String resendUrl = "https://api.resend.com/emails";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("from", fromEmail);
        body.put("to", Collections.singletonList(to));
        body.put("subject", subject);
        body.put("html", bodyHtml);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            Map<?, ?> response = restTemplate.postForObject(resendUrl, request, Map.class);
            if (response != null && response.containsKey("id")) {
                logger.info("Email dispatched successfully! Resend Notification ID: {}", response.get("id"));
            } else {
                logger.warn("Resend accepted request but did not return a notification ID. Response: {}", response);
            }
        } catch (Exception e) {
            logger.error("Failed to send email notification via Resend: {}", e.getMessage(), e);
        }
    }
}
