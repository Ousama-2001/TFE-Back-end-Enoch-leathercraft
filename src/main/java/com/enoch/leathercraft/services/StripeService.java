package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.OrderItemResponse;
import com.enoch.leathercraft.dto.OrderResponse;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class StripeService {

    private final String frontendUrl;

    public StripeService(
            @Value("${stripe.secret-key}") String secretKey,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl
    ) {
        Stripe.apiKey = secretKey;
        this.frontendUrl = frontendUrl;
    }

    public String createCheckoutSession(OrderResponse order) throws StripeException {

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/order-success/" + order.getReference() + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/checkout?canceled=true")
                .putMetadata("orderReference", order.getReference());

        // ✅ Calcul du multiplicateur de réduction (ex: -10% => multiplier par 0.90)
        BigDecimal discountMultiplier = BigDecimal.ONE;
        if (order.getCouponPercent() != null && order.getCouponPercent() > 0) {
            BigDecimal percent = BigDecimal.valueOf(order.getCouponPercent());
            discountMultiplier = BigDecimal.ONE.subtract(percent.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }

        // Ajouter les lignes de commande
        for (OrderItemResponse item : order.getItems()) {

            // ✅ On applique la réduction sur le prix unitaire envoyé à Stripe
            BigDecimal discountedUnitPrice = item.getUnitPrice().multiply(discountMultiplier);

            long unitAmountInCents = discountedUnitPrice
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            SessionCreateParams.LineItem lineItem =
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(item.getQuantity().longValue())
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("eur")
                                            .setUnitAmount(unitAmountInCents)
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName(item.getProductName())
                                                            .build()
                                            )
                                            .build()
                            )
                            .build();

            builder.addLineItem(lineItem);
        }

        SessionCreateParams params = builder.build();
        Session session = Session.create(params);

        return session.getUrl();
    }
}