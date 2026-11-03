package com.cavsteek.jobapplicationtrackerapi.auth.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds) {}