package com.ledgerline.notifications;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends notification emails. Locally they land in Mailpit (http://localhost:8025), never in a real
 * inbox. Tests switch email off ({@code ledgerline.notifications.email-enabled=false}).
 */
@Component
class EmailSender {

    private final ObjectProvider<JavaMailSender> mail;
    private final boolean enabled;

    EmailSender(
            ObjectProvider<JavaMailSender> mail,
            @Value("${ledgerline.notifications.email-enabled:true}") boolean enabled) {
        this.mail = mail;
        this.enabled = enabled;
    }

    void send(String to, String subject, String body) {
        if (!enabled) {
            return;
        }
        mail.ifAvailable(sender -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("Ledgerline <no-reply@ledgerline.dev>");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body + "\n\nLedgerline is a demo. No real money moves.");
            sender.send(message);
        });
    }
}
