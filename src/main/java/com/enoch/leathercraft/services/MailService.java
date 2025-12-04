package com.enoch.leathercraft.services;

import com.enoch.leathercraft.entities.Order;
import com.enoch.leathercraft.entities.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@enoch-leathercraft.com}")
    private String from;

    @Value("${app.superadmin.email:}")
    private String superAdminEmail;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    // ====================== GENERIC SENDER =======================
    private void sendSimpleMail(String to, String subject, String text) {
        if (!mailEnabled) {
            log.warn("📧 Envoi désactivé (app.mail.enabled=false). Mail ignoré → {}", to);
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setFrom(from);
            msg.setSubject(subject);
            msg.setText(text);

            mailSender.send(msg);
            log.info("📧 Email envoyé → {}", to);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l’envoi d’un email à {} : {}", to, e.getMessage());
        }
    }

    // ==================== PASSWORD RESET =======================
    public void sendPasswordResetLink(String to, String resetLink) {
        String text = """
                Bonjour,

                Voici votre lien pour réinitialiser votre mot de passe :
                %s

                Si vous n'avez pas demandé cela, ignorez cet email.

                Enoch Leathercraft
                """.formatted(resetLink);

        sendSimpleMail(to, "Réinitialisation du mot de passe", text);
    }

    public void sendPasswordChangedEmail(String to) {
        String text = """
                Bonjour,

                Votre mot de passe a bien été modifié.
                Si ce n'était pas vous, contactez immédiatement notre support.

                Enoch Leathercraft
                """;

        sendSimpleMail(to, "Votre mot de passe a été modifié", text);
    }

    // ==================== ORDER CONFIRMATION =======================
    public void sendOrderConfirmation(Order order) {
        try {
            String body = buildOrderBody(order);
            sendSimpleMail(order.getCustomerEmail(),
                    "Confirmation de commande " + order.getReference(),
                    body);
        } catch (Exception e) {
            log.error("Erreur envoi mail confirmation commande {}", order.getReference(), e);
        }
    }

    public void sendOrderStatusUpdated(Order order) {
        try {
            String body = buildStatusBody(order);
            sendSimpleMail(order.getCustomerEmail(),
                    "Mise à jour commande " + order.getReference(),
                    body);
        } catch (Exception e) {
            log.error("Erreur envoi mail statut commande {}", order.getReference(), e);
        }
    }

    private String buildOrderBody(Order order) {
        StringBuilder sb = new StringBuilder();

        sb.append("Bonjour,\n\n");
        sb.append("Merci pour votre commande !\n\n");
        sb.append("Référence : ").append(order.getReference()).append("\n");
        sb.append("Date      : ").append(order.getCreatedAt()).append("\n\n");

        sb.append("Articles :\n");
        for (OrderItem item : order.getItems()) {
            BigDecimal total = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            sb.append(" - ").append(item.getQuantity()).append(" × ").append(item.getProductName())
                    .append(" = ").append(total).append(" €\n");
        }

        sb.append("\nTotal : ").append(order.getTotalAmount()).append(" €\n\n");
        sb.append("Merci pour votre confiance.\n");
        sb.append("Enoch Leathercraft");

        return sb.toString();
    }

    private String buildStatusBody(Order order) {
        StringBuilder sb = new StringBuilder();

        sb.append("Bonjour,\n\n");
        sb.append("Le statut de votre commande ").append(order.getReference()).append(" a été modifié.\n\n");
        sb.append("Nouveau statut : ").append(order.getStatus()).append("\n\n");

        sb.append("Merci pour votre confiance.\n");
        sb.append("Enoch Leathercraft");

        return sb.toString();
    }

// =============== DEMANDE RÉACTIVATION COMPTE ===============

    public void sendReactivationRequestEmailToAdmin(String userEmail, String message) {

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo("saidenoch@gmail.com");
            msg.setFrom(from);
            msg.setSubject("🔔 [Enoch] Demande de réactivation de compte");

            String body =
                    "Bonjour,\n\n" +
                            "Vous avez reçu une NOUVELLE demande de support.\n\n" +
                            "Type de demande : RÉACTIVATION DE COMPTE\n" +
                            "Provenance    : Formulaire de réactivation (page de connexion)\n\n" +
                            "Email utilisateur : " + userEmail + "\n\n" +
                            "Message :\n" +
                            (message == null || message.isBlank()
                                    ? "Aucun message fourni."
                                    : message) +
                            "\n\n" +
                            "Connectez-vous au panneau super administrateur pour gérer cette demande.\n\n" +
                            "Enoch Leathercraft Shop";

            msg.setText(body);
            mailSender.send(msg);

        } catch (Exception e) {
            log.error("❌ Erreur envoi email super admin : {}", e.getMessage());
        }
    }

}
