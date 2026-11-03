package com.cavsteek.jobapplicationtrackerapi.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private final RestClient client;
    private final BrevoProperties props;

    public EmailService(BrevoProperties props) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));

        this.props = props;
        this.client = RestClient.builder()
                .requestFactory(factory)
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
