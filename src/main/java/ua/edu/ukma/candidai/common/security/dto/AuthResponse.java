package ua.edu.ukma.candidai.common.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT authentication response containing access token")
public record AuthResponse(
        @Schema(
                description = "Signed HMAC-SHA256 JWT access token",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        String accessToken,

        @Schema(description = "Token type prefix", example = "Bearer")
        String tokenType,

        @Schema(description = "Token expiration time in seconds", example = "3600")
        long expiresIn
) {
    public static AuthResponse of(String accessToken, long expiresIn) {
        return new AuthResponse(accessToken, "Bearer", expiresIn);
    }
}
