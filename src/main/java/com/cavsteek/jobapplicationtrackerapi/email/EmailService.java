package com.cavsteek.jobapplicationtrackerapi.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service

public class EmailService {

    private final RestClient client;
    private final BrevoProperties props;

    public EmailService(RestClient.Builder builder, BrevoProperties props) {
        this.props = props;
        this.client = builder
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", props.apiKey())
                .build();
    }

    public void send(String to, String subject, String html) {
        var body = Map.of(
                "sender", Map.of("name", props.senderName(), "email", props.senderEmail()),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", html);

        client.post()
                .uri("/smtp/email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
