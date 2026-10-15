
package com.lostfound.service;

import com.lostfound.entity.FoundItem;
import com.lostfound.entity.LostItem;
import com.lostfound.entity.Match;
import com.lostfound.entity.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void notifyPotentialMatch(User user, Match match) {
        if (user == null || match == null) {
            return;
        }

        LostItem lost = match.getLostItem();
        FoundItem found = match.getFoundItem();

        String body = """
                Hello %s,

                A potential match was found.

                Lost item: %s
                Found item: %s
                Similarity score: %s

                Please log in to Lost & Found Matcher to review it.
                Do not share passwords or OTPs by email.
                """.formatted(
                safe(user.getName()),
                safe(lost.getTitle()),
                safe(found.getTitle()),
                match.getSimilarityScore() == null
                        ? "N/A"
                        : match.getSimilarityScore() + "%");

        send(user.getEmail(), "Potential lost item match", body);
    }

    public void notifyMatchConfirmed(Match match) {
        if (match == null) {
            return;
        }

        LostItem lost = match.getLostItem();
        FoundItem found = match.getFoundItem();

        String body = """
                Hello,

                A lost-and-found match has been confirmed.

                Lost item: %s
                Found item: %s

                Please coordinate responsibly to arrange the return.
                Do not share passwords or OTPs.

                Lost & Found Matcher
                """.formatted(
                safe(lost.getTitle()),
                safe(found.getTitle()));

        String lostEmail = lost.getUser().getEmail();
        String foundEmail = found.getUser().getEmail();

        send(lostEmail, "Lost & Found match confirmed", body);

        if (foundEmail != null
                && (lostEmail == null
                || !foundEmail.equalsIgnoreCase(lostEmail))) {
            send(foundEmail, "Lost & Found match confirmed", body);
        }
    }

    private void send(String to, String subject, String body) {
        if (!enabled) {
            log.debug("Email notifications disabled");
            return;
        }

        if (to == null || to.isBlank()
                || fromAddress == null || fromAddress.isBlank()) {
            log.warn("Email skipped: sender or recipient is missing");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Unable to send notification email", exception);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank()
                ? "Not provided" : value;
    }
}
