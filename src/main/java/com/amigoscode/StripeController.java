package com.amigoscode;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.LineItem;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionListLineItemsParams;
import com.stripe.param.checkout.SessionRetrieveParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/v1/stripe")
public class StripeController {

    private static final String DOMAIN = "http://localhost:8080";

    private static final RequestOptions REQUEST_OPTIONS = RequestOptions.builder()
            .setApiKey("sk_test_51OVN7OEdLIJZ0ux1xUvp38blptZYhfVF3MjkCszdp0HLjZwtSkM9K9lQYJxrQ3To5NkwfrbnNByjeF8OVjDOvbWD00JSid3AP0")
            .build();

    @PostMapping
    public void checkoutSession(HttpServletResponse response) {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(DOMAIN + "/success.html")
                .setCancelUrl(DOMAIN + "/index.html")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPrice("price_1OVNCsEdLIJZ0ux1uBd80Syt")
                                .build()
                )
                .build();
        try {

            Session session = Session.create(params, REQUEST_OPTIONS);
            response.sendRedirect(session.getUrl());
        } catch (StripeException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("webhook")
    public void webhook(@RequestBody String payload,
                        HttpServletRequest request) {
        String sigHeader = request.getHeader("Stripe-Signature");
        Event event;
        try {
            String secret = "whsec_36fedc98f8b407a9f43c15786f2f8abdc7d99592303ef8485bbaa88245ad162d";
            event = Webhook.constructEvent(payload, sigHeader, secret);
        } catch (SignatureVerificationException e) {
            throw new RuntimeException(e);
        }
        switch (event.getType()) {
            case "checkout.session.completed" -> {
                System.out.printf("✅ %s\n", event.getType());
                Session sessionEvent = (Session) event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Cannot Deserialize Stipe Object"
                                )
                        );

                SessionRetrieveParams retrieveParams =
                        SessionRetrieveParams.builder()
                                .addExpand("line_items")
                                .build();

                try {
                    RequestOptions REQUEST_OPTIONS = RequestOptions.builder()
                            .setApiKey("sk_test_51OVN7OEdLIJZ0ux1xUvp38blptZYhfVF3MjkCszdp0HLjZwtSkM9K9lQYJxrQ3To5NkwfrbnNByjeF8OVjDOvbWD00JSid3AP0")
                            .build();
                    Session session = Session.retrieve(
                            sessionEvent.getId(),
                            retrieveParams,
                            REQUEST_OPTIONS
                    );

                    System.out.println("customer " + session.getCustomer());
                    System.out.println("session id " + session.getId());
                    System.out.println("payment status " + session.getPaymentStatus());

                    SessionListLineItemsParams sessionListLineItemsParams =
                            SessionListLineItemsParams.builder().build();

                    List<LineItem> lineItems = session
                            .listLineItems(sessionListLineItemsParams, REQUEST_OPTIONS)
                            .getData();
                    lineItems.forEach(System.out::println);

                } catch (StripeException e) {
                    throw new RuntimeException(e);
                }
            }
            default -> System.out.printf("❌ %s Event not supported%n", event.getType());
        }

    }
}
