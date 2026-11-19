package com.ysh.domain.market.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KisTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Long expiresInSeconds,
        @JsonProperty("error_code")        String errorCode,
        @JsonProperty("error_description") String errorDescription
) {
    public boolean isError() {
        return errorCode != null && !errorCode.isBlank();
    }
}
