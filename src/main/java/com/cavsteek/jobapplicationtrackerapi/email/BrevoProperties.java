package com.cavsteek.jobapplicationtrackerapi.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brevo")
public record BrevoProperties(String apiKey, String senderEmail, String senderName) {}