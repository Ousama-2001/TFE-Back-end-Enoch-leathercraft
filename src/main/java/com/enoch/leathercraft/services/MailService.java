package com.enoch.leathercraft.services;

import com.enoch.leathercraft.entities.Order;
import com.enoch.leathercraft.entities.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    // Adresse d'expéditeur (configurable dans application.yml / properties)
    @Value("${app.mail.from:no-reply@enoch-leathercraft.com}")
    private String from;

    // ================== MOT DE PASSE ==================

    public void sendPasswordResetLink(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(from);
        message.setSubject("Réinitialisation de votre mot de passe");
        message.setText("""
                Bonjour,

                Voici votre lien pour réinitialiser votre mot de passe :
                """ + resetLink + """

                Si vous n'avez pas demandé cela, ignorez cet email.

                Enoch Leathercraft
                """);

        mailSender.send(message);
    }

    public void sendPasswordChangedEmail(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(from);
        message.setSubject("Votre mot de passe a été modifié");
        message.setText("""
                Bonjour,

                Votre mot de passe a été modifié avec succès.
                Si ce n'était pas vous, contactez immédiatement le support.

                Enoch Leathercraft
                """);

        mailSender.send(message);
    }

    // ================== COMMANDE : CONFIRMATION ==================

    public void sendOrderConfirmation(Order order) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(order.getCustomerEmail());
            message.setFrom(from);
            message.setSubject("Confirmation de votre commande " + order.getReference());
            message.setText(buildOrderBody(order));

            mailSender.send(message);

        } catch (Exception e) {
            // On ne casse pas la commande si l'email échoue
            e.printStackTrace();
        }
    }

    // ================== COMMANDE : MISE À JOUR STATUT ==================

    public void sendOrderStatusUpdated(Order order) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(order.getCustomerEmail());
            message.setFrom(from);
            message.setSubject("Mise à jour de votre commande " + order.getReference());
            message.setText(buildStatusBody(order));

            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildOrderBody(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bonjour,\n\n");
        sb.append("Merci pour votre commande sur Enoch Leathercraft.\n\n");
        sb.append("Référence : ").append(order.getReference()).append("\n");
        sb.append("Date      : ").append(order.getCreatedAt()).append("\n");
        sb.append("Statut    : ").append(order.getStatus()).append("\n\n");
        sb.append("Détail :\n");

        for (OrderItem item : order.getItems()) {
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            sb.append(" - ")
                    .append(item.getQuantity()).append(" × ")
                    .append(item.getProductName())
                    .append(" (").append(item.getUnitPrice()).append(" €)")
                    .append(" = ").append(lineTotal).append(" €\n");
        }

        sb.append("\nTotal : ").append(order.getTotalAmount()).append(" €\n\n");
        sb.append("Nous vous tiendrons informé(e) lors de l'expédition.\n\n");
        sb.append("L'équipe Enoch Leathercraft");

        return sb.toString();
    }

    private String buildStatusBody(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bonjour,\n\n");
        sb.append("Le statut de votre commande ").append(order.getReference()).append(" a été mis à jour.\n\n");
        sb.append("Nouveau statut : ").append(order.getStatus()).append("\n\n");

        if (order.getStatus() != null) {
            switch (order.getStatus()) {
                case SHIPPED -> sb.append("Votre commande a été expédiée. Elle sera bientôt livrée.\n\n");
                case DELIVERED -> sb.append("Votre commande est indiquée comme livrée.\n\n");
                case CANCELLED -> sb.append("Votre commande a été annulée.\n\n");
                default -> sb.append("");
            }
        }

        sb.append("Merci pour votre confiance.\n\n");
        sb.append("L'équipe Enoch Leathercraft");

        return sb.toString();
    }
}
