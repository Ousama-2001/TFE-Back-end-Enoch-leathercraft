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
                .setSuccessUrl(frontendUrl + "/order-success/" + order.getReference())
                .setCancelUrl(frontendUrl + "/checkout?canceled=true")
                .putMetadata("orderReference", order.getReference());

        // Ajouter les lignes de commande
        for (OrderItemResponse item : order.getItems()) {
            long unitAmountInCents = item.getUnitPrice()
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

        return session.getUrl(); // URL complète Stripe Checkout
    }
}
