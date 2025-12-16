package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.CouponRequest;
import com.enoch.leathercraft.dto.CouponResponse;
import com.enoch.leathercraft.dto.CouponValidateResponse;
import com.enoch.leathercraft.entities.Coupon;
import com.enoch.leathercraft.repository.CouponRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    // ---------- ADMIN CRUD ----------
    @Transactional(readOnly = true)
    public List<CouponResponse> getAll() {
        return couponRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public CouponResponse create(CouponRequest req) {
        String code = normalizeCode(req.code());
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Code obligatoire");
        if (couponRepository.existsByCodeIgnoreCase(code)) throw new IllegalArgumentException("Code déjà existant");
        validatePercent(req.percent());
        validateDates(req.startsAt(), req.endsAt());

        Coupon c = Coupon.builder()
                .code(code)
                .percent(req.percent())
                .startsAt(req.startsAt())
                .endsAt(req.endsAt())
                .active(req.active() == null ? true : req.active())
                .maxUses(req.maxUses())
                .usedCount(0)
                .build();

        return toDto(couponRepository.save(c));
    }

    @Transactional
    public CouponResponse update(Long id, CouponRequest req) {
        Coupon c = couponRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Coupon introuvable"));

        if (req.code() != null && !req.code().isBlank()) {
            String newCode = normalizeCode(req.code());
            if (!newCode.equalsIgnoreCase(c.getCode()) && couponRepository.existsByCodeIgnoreCase(newCode)) {
                throw new IllegalArgumentException("Code déjà existant");
            }
            c.setCode(newCode);
        }

        if (req.percent() != null) {
            validatePercent(req.percent());
            c.setPercent(req.percent());
        }

        if (req.startsAt() != null || req.endsAt() != null) {
            validateDates(req.startsAt(), req.endsAt());
            c.setStartsAt(req.startsAt());
            c.setEndsAt(req.endsAt());
        }

        if (req.active() != null) c.setActive(req.active());
        if (req.maxUses() != null) c.setMaxUses(req.maxUses());

        return toDto(couponRepository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        couponRepository.deleteById(id);
    }

    // ---------- PUBLIC / CHECKOUT ----------
    @Transactional(readOnly = true)
    public CouponValidateResponse validate(String rawCode) {
        String code = normalizeCode(rawCode);

        if (code == null || code.isBlank()) {
            return CouponValidateResponse.builder()
                    .code(rawCode)
                    .valid(false)
                    .percent(null)
                    .reason("EMPTY")
                    .build();
        }

        Coupon c = couponRepository.findByCodeIgnoreCase(code).orElse(null);
        if (c == null) {
            return CouponValidateResponse.builder()
                    .code(code)
                    .valid(false)
                    .percent(null)
                    .reason("NOT_FOUND")
                    .build();
        }

        Validation v = evaluate(c, Instant.now());

        return CouponValidateResponse.builder()
                .code(c.getCode())
                .valid(v.validNow)
                .percent(v.validNow ? c.getPercent() : null)
                .reason(v.status) // ✅ status interne -> reason dans DTO
                .build();
    }

    // appelé au moment où la commande est créée (usageCount++)
    @Transactional
    public Coupon requireValidCouponOrThrow(String rawCode) {
        String code = normalizeCode(rawCode);
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Coupon vide");

        Coupon c = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Coupon invalide"));

        Validation v = evaluate(c, Instant.now());
        if (!v.validNow) throw new IllegalArgumentException("Coupon non valable: " + v.status);

        return c;
    }

    @Transactional
    public void incrementUse(Coupon c) {
        c.setUsedCount((c.getUsedCount() == null ? 0 : c.getUsedCount()) + 1);
        couponRepository.save(c);
    }

    // ---------- RULES ----------
    private record Validation(boolean validNow, String status) {}

    private Validation evaluate(Coupon c, Instant now) {
        if (Boolean.FALSE.equals(c.getActive())) return new Validation(false, "INACTIVE");

        Integer percent = c.getPercent();
        if (percent == null || percent <= 0 || percent > 90) return new Validation(false, "INVALID_PERCENT");

        if (c.getStartsAt() != null && now.isBefore(c.getStartsAt())) return new Validation(false, "UPCOMING");
        if (c.getEndsAt() != null && now.isAfter(c.getEndsAt())) return new Validation(false, "EXPIRED");

        Integer maxUses = c.getMaxUses();
        Integer used = c.getUsedCount() == null ? 0 : c.getUsedCount();
        if (maxUses != null && used >= maxUses) return new Validation(false, "LIMIT_REACHED");

        return new Validation(true, "ACTIVE");
    }

    private void validatePercent(Integer p) {
        if (p == null) throw new IllegalArgumentException("Percent obligatoire");
        if (p <= 0 || p > 90) throw new IllegalArgumentException("Percent doit être entre 1 et 90");
    }

    private void validateDates(Instant start, Instant end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("endsAt ne peut pas être avant startsAt");
        }
    }

    private String normalizeCode(String code) {
        if (code == null) return null;
        return code.trim().toUpperCase();
    }

    private CouponResponse toDto(Coupon c) {
        Validation v = evaluate(c, Instant.now());

        return CouponResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .percent(c.getPercent())
                .startsAt(c.getStartsAt())
                .endsAt(c.getEndsAt())
                .active(c.getActive())
                .maxUses(c.getMaxUses())
                .usedCount(c.getUsedCount())
                .validNow(v.validNow)
                .status(v.status)
                .build();
    }
}
