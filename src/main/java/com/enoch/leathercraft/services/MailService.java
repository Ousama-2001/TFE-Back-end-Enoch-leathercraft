package com.enoch.leathercraft.services;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetLink(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
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
        message.setSubject("Votre mot de passe a été modifié");
        message.setText("""
                Bonjour,

                Votre mot de passe a été modifié avec succès.
                Si ce n'était pas vous, contactez immédiatement le support.

                Enoch Leathercraft
                """);

        mailSender.send(message);
    }
}
