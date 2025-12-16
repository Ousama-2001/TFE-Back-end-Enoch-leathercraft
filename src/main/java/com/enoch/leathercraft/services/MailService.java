// src/main/java/com/enoch/leathercraft/services/MailService.java
package com.enoch.leathercraft.services;

import com.enoch.leathercraft.auth.domain.Role;
import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.entities.Order;
import com.enoch.leathercraft.entities.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

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

        if (to == null || to.isBlank()) {
            log.warn("📧 Destinataire vide. Mail ignoré.");
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

    // ==================== ✅ WELCOME EMAIL (NEW) =======================
    public void sendWelcomeEmail(String to, String firstName) {
        String displayName = (firstName != null && !firstName.isBlank()) ? firstName.trim() : "👋";

        String text = """
                Bonjour %s,

                Bienvenue sur Enoch Leathercraft Shop ✨
                Votre compte a été créé avec succès.

                Vous pouvez maintenant :
                - parcourir le catalogue,
                - ajouter des articles à votre wishlist,
                - passer commande en toute sécurité.

                Merci de votre confiance,
                Enoch Leathercraft
                """.formatted(displayName);

        sendSimpleMail(to, "Bienvenue sur Enoch Leathercraft ✨", text);
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

    // ==================== ✅ EMAIL CHANGE =======================
    public void sendEmailChangeConfirmLink(String toNewEmail, String confirmLink) {
        String text = """
                Bonjour,

                Vous avez demandé à changer l'adresse email associée à votre compte Enoch Leathercraft.

                Pour confirmer votre nouvel email, cliquez sur ce lien :
                %s

                Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.

                Enoch Leathercraft
                """.formatted(confirmLink);

        sendSimpleMail(toNewEmail, "Confirmez votre nouvel email", text);
    }

    public void sendEmailChangedEmail(String oldEmail, String newEmail) {

        String oldEmailText = """
            Bonjour,

            L'adresse email associée à votre compte Enoch Leathercraft a été MODIFIÉE.

            Ancien email : %s
            Nouvel email : %s

            ⚠️ Si vous n'êtes PAS à l'origine de ce changement,
            contactez immédiatement notre support.

            Enoch Leathercraft – Sécurité
            """.formatted(oldEmail, newEmail);

        sendSimpleMail(oldEmail, "⚠️ Changement d'email sur votre compte", oldEmailText);

        String newEmailText = """
            Bonjour,

            Votre adresse email a bien été mise à jour avec succès.

            Nouvel email associé à votre compte :
            %s

            Bienvenue sur Enoch Leathercraft ✨

            Enoch Leathercraft
            """.formatted(newEmail);

        sendSimpleMail(newEmail, "Votre email a été mis à jour", newEmailText);
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
        if (!mailEnabled) {
            log.warn("📧 Envoi email réactivation désactivé");
            return;
        }

        String adminTo = (superAdminEmail != null && !superAdminEmail.isBlank())
                ? superAdminEmail
                : null;

        if (adminTo == null) {
            log.warn("Aucun super admin email configuré (app.superadmin.email). Mail réactivation ignoré.");
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(adminTo);
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

    // =============== 🔥 DEMANDE DE RETOUR (admins) ===============
    public void sendReturnRequested(Order order) {
        try {
            List<User> admins = userRepository.findByRoleInAndDeletedFalse(
                    List.of(Role.ADMIN, Role.SUPER_ADMIN)
            );

            Set<String> destinations = new HashSet<>();

            for (User admin : admins) {
                if (admin.getEmail() != null && !admin.getEmail().isBlank()) {
                    destinations.add(admin.getEmail());
                }
            }

            if (superAdminEmail != null && !superAdminEmail.isBlank()) {
                destinations.add(superAdminEmail);
            }

            if (destinations.isEmpty()) {
                log.warn("Aucun admin/superadmin trouvé pour notifier la demande de retour {}", order.getReference());
                return;
            }

            String subject = "Demande de retour – commande " + order.getReference();

            String body = """
                    Bonjour,

                    Une DEMANDE DE RETOUR vient d'être effectuée.

                    Référence commande : %s
                    Client : %s %s (%s)
                    Montant : %s €
                    Statut actuel : %s

                    Notes / motif de retour :
                    %s

                    Rendez-vous dans le back-office administrateur (onglet Commandes / Retours)
                    pour traiter cette demande.

                    Enoch Leathercraft – Notification automatique
                    """.formatted(
                    safe(order.getReference()),
                    safe(order.getFirstName()),
                    safe(order.getLastName()),
                    safe(order.getCustomerEmail()),
                    order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO,
                    String.valueOf(order.getStatus()),
                    safe(order.getNotes())
            );

            for (String to : destinations) {
                sendSimpleMail(to, subject, body);
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de la notification de retour pour {} : {}",
                    order.getReference(), e.getMessage());
        }
    }

    public void sendReturnApprovedToCustomer(Order order) {
        String subject = "Retour accepté – commande " + safe(order.getReference());

        String body = """
                Bonjour,

                Votre demande de retour pour la commande %s a été ACCEPTÉE.

                Vous pouvez renvoyer votre colis à l'adresse suivante :

                Enoch Leathercraft – Service Retours
                Rue de la Maroquinerie 42
                1000 Bruxelles
                Belgique

                Merci d'indiquer clairement la référence de commande : %s

                Dès réception et contrôle des articles, nous traiterons votre remboursement.

                Enoch Leathercraft
                """.formatted(
                safe(order.getReference()),
                safe(order.getReference())
        );

        sendSimpleMail(order.getCustomerEmail(), subject, body);
    }

    public void sendReturnRejectedToCustomer(Order order, String adminReason) {
        String subject = "Retour refusé – commande " + safe(order.getReference());

        String reason = (adminReason != null && !adminReason.isBlank())
                ? adminReason
                : "Aucune raison précise n'a été fournie.";

        String body = """
                Bonjour,

                Votre demande de retour pour la commande %s a été REFUSÉE.

                Raison fournie par notre équipe :
                %s

                Si vous pensez qu'il s'agit d'une erreur, vous pouvez répondre à cet email.

                Enoch Leathercraft
                """.formatted(
                safe(order.getReference()),
                reason
        );

        sendSimpleMail(order.getCustomerEmail(), subject, body);
    }

    public void sendPaidOrderCancelledToCustomer(Order order) {
        String subject = "Commande annulée – " + safe(order.getReference());

        String body = """
                Bonjour,

                Votre commande %s, qui avait été payée, a été ANNULÉE.

                Un remboursement sera traité sur votre moyen de paiement initial selon nos conditions
                (délai bancaire habituel).

                Référence commande : %s
                Montant : %s €

                Si vous n'êtes pas à l'origine de cette annulation ou si vous avez une question,
                vous pouvez répondre à cet email.

                Enoch Leathercraft
                """.formatted(
                safe(order.getReference()),
                safe(order.getReference()),
                order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO
        );

        sendSimpleMail(order.getCustomerEmail(), subject, body);
    }

    public void sendPaidOrderCancelledToAdmins(Order order) {
        try {
            List<User> admins = userRepository.findByRoleInAndDeletedFalse(
                    List.of(Role.ADMIN, Role.SUPER_ADMIN)
            );

            Set<String> destinations = new HashSet<>();

            for (User admin : admins) {
                if (admin.getEmail() != null && !admin.getEmail().isBlank()) {
                    destinations.add(admin.getEmail());
                }
            }

            if (superAdminEmail != null && !superAdminEmail.isBlank()) {
                destinations.add(superAdminEmail);
            }

            if (destinations.isEmpty()) {
                log.warn("Aucun admin/superadmin trouvé pour notifier l'annulation payée {}", order.getReference());
                return;
            }

            String subject = "Commande payée annulée – " + safe(order.getReference());

            String body = """
                    Bonjour,

                    Une COMMANDE PAYÉE vient d'être ANNULÉE par le client.

                    Référence : %s
                    Client    : %s %s (%s)
                    Montant   : %s €

                    Statut actuel : %s

                    Merci de vérifier le traitement du remboursement dans votre interface de paiement.

                    Enoch Leathercraft – Notification automatique
                    """.formatted(
                    safe(order.getReference()),
                    safe(order.getFirstName()),
                    safe(order.getLastName()),
                    safe(order.getCustomerEmail()),
                    order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO,
                    String.valueOf(order.getStatus())
            );

            for (String to : destinations) {
                sendSimpleMail(to, subject, body);
            }

        } catch (Exception e) {
            log.error("❌ Erreur envoi mail commande payée annulée pour {} : {}",
                    order.getReference(), e.getMessage());
        }
    }

    public void sendContactEmail(String name, String email, String message) {
        if (!mailEnabled) {
            log.warn("📧 Envoi email contact désactivé");
            return;
        }

        if (superAdminEmail == null || superAdminEmail.isBlank()) {
            log.warn("Aucun super admin email configuré (app.superadmin.email). Mail contact admin ignoré.");
            return;
        }

        try {
            SimpleMailMessage adminMsg = new SimpleMailMessage();
            adminMsg.setTo(superAdminEmail);
            adminMsg.setFrom(from);
            adminMsg.setSubject("📩 Nouveau message de contact");

            adminMsg.setText("""
                Nouveau message reçu via le formulaire de contact.

                Nom    : %s
                Email  : %s

                Message :
                %s

                — Enoch Leathercraft
                """.formatted(name, email, message));

            mailSender.send(adminMsg);

            SimpleMailMessage userMsg = new SimpleMailMessage();
            userMsg.setTo(email);
            userMsg.setFrom(from);
            userMsg.setSubject("Nous avons bien reçu votre message");

            userMsg.setText("""
                Bonjour %s,

                Merci pour votre message.
                Nous vous répondrons dans les plus brefs délais.

                — Enoch Leathercraft
                """.formatted(name));

            mailSender.send(userMsg);

            log.info("📧 Emails contact envoyés (admin + client)");

        } catch (Exception e) {
            log.error("❌ Erreur envoi email contact : {}", e.getMessage());
        }
    }

    private String safe(String v) {
        return v != null ? v : "";
    }
}
