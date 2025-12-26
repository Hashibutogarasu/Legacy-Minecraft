package com.karasu256.mcauth.processor.model;

import com.google.gson.annotations.SerializedName;

/**
 * Response from Microsoft OAuth token endpoint.
 */
public class MicrosoftTokenResponse {
    
    @SerializedName("access_token")
    private String accessToken;
    
    @SerializedName("refresh_token")
    private String refreshToken;
    
    @SerializedName("token_type")
    private String tokenType;
    
    @SerializedName("expires_in")
    private Integer expiresIn;
    
    private String error;
    
    @SerializedName("error_description")
    private String errorDescription;
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public Integer getExpiresIn() {
        return expiresIn;
    }
    
    public String getError() {
        return error;
    }
    
    public String getErrorDescription() {
        return errorDescription;
    }
    
    public boolean isSuccess() {
        return accessToken != null && !accessToken.isBlank();
    }
}
