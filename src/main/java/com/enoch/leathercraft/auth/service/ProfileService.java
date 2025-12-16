// src/main/java/com/enoch/leathercraft/auth/service/ProfileService.java
package com.enoch.leathercraft.auth.service;

import com.enoch.leathercraft.auth.domain.EmailChangeToken;
import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.dto.EmailChangeConfirmRequest;
import com.enoch.leathercraft.auth.dto.EmailChangeRequest;
import com.enoch.leathercraft.auth.repo.EmailChangeTokenRepository;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.services.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository users;
    private final EmailChangeTokenRepository emailTokens;
    private final PasswordEncoder encoder;
    private final MailService mailService;

    // simple & suffisant (tu peux aussi utiliser jakarta @Email côté DTO)
    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    public void requestEmailChange(String authenticatedEmail, EmailChangeRequest req) {

        if (req == null) {
            throw new IllegalArgumentException("REQUEST_REQUIRED");
        }

        if (req.getNewEmail() == null || req.getNewEmail().isBlank()) {
            throw new IllegalArgumentException("EMAIL_REQUIRED");
        }

        String newEmail = req.getNewEmail().trim().toLowerCase();

        if (!EMAIL_REGEX.matcher(newEmail).matches()) {
            throw new IllegalArgumentException("EMAIL_INVALID");
        }

        // ⚠️ auth.getName() chez toi = email (Spring Security UserDetails)
        String currentEmail = authenticatedEmail == null ? null : authenticatedEmail.trim().toLowerCase();

        if (currentEmail == null || currentEmail.isBlank()) {
            throw new IllegalStateException("USER_NOT_FOUND");
        }

        User user = users.findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalStateException("USER_NOT_FOUND"));

        // optionnel mais recommandé : demander le mdp actuel
        if (req.getCurrentPassword() == null || req.getCurrentPassword().isBlank()) {
            throw new IllegalArgumentException("PASSWORD_REQUIRED");
        }
        if (!encoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("BAD_PASSWORD");
        }

        // si l'utilisateur met le même email -> pas besoin
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("EMAIL_SAME_AS_CURRENT");
        }

        if (users.existsByEmail(newEmail)) {
            throw new IllegalStateException("EMAIL_ALREADY_USED");
        }

        // supprime anciens tokens
        emailTokens.deleteByUserId(user.getId());

        EmailChangeToken token = EmailChangeToken.forUser(user, newEmail, 30);
        emailTokens.save(token);

        String link = "http://localhost:4200/confirm-email-change?token=" + token.getToken();

        // ✅ lien envoyé AU NOUVEL EMAIL
        mailService.sendEmailChangeConfirmLink(newEmail, link);
    }

    public void confirmEmailChange(EmailChangeConfirmRequest req) {

        if (req == null) {
            throw new IllegalArgumentException("REQUEST_REQUIRED");
        }

        if (req.getToken() == null || req.getToken().isBlank()) {
            throw new IllegalArgumentException("TOKEN_REQUIRED");
        }

        EmailChangeToken token = emailTokens.findByToken(req.getToken())
                .orElseThrow(() -> new IllegalArgumentException("TOKEN_INVALID"));

        if (token.isExpired()) {
            emailTokens.delete(token);
            throw new IllegalArgumentException("TOKEN_EXPIRED");
        }

        User user = token.getUser();
        String newEmail = token.getNewEmail().trim().toLowerCase();

        if (!EMAIL_REGEX.matcher(newEmail).matches()) {
            emailTokens.delete(token);
            throw new IllegalArgumentException("EMAIL_INVALID");
        }

        if (users.existsByEmail(newEmail)) {
            emailTokens.delete(token);
            throw new IllegalStateException("EMAIL_ALREADY_USED");
        }

        // ✅ garder l'ancien email pour notifier les deux
        String oldEmail = user.getEmail();

        user.setEmail(newEmail);
        users.save(user);

        // ✅ notifier ANCIEN + NOUVEL email
        mailService.sendEmailChangedEmail(oldEmail, newEmail);

        emailTokens.delete(token);
    }
}
