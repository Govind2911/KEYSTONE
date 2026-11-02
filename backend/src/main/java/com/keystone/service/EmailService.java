package com.keystone.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Optional e-mail copy of notifications. Does nothing unless SPRING_MAIL_HOST (SMTP) is configured. */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final ObjectProvider<JavaMailSender> sender;
    private final String from;

    public EmailService(ObjectProvider<JavaMailSender> sender, @Value("${keystone.mail.from:no-reply@keystone.local}") String from) {
        this.sender = sender;
        this.from = from;
    }

    @Async
    public void send(String to, String subject, String text) {
        JavaMailSender s = sender.getIfAvailable();
        if (s == null) return;
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(text);
            s.send(m);
        } catch (Exception e) {
            log.warn("Could not send e-mail to {}: {}", to, e.getMessage());
        }
    }
}
