package com.quickeats.notificationservice.service;

public interface EmailService {
    void sendEmail(String to, String subject, String bodyHtml);
}
