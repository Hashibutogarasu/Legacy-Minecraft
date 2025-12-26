package com.karasu256.mcauth.processor.model;

import com.google.gson.annotations.SerializedName;

/**
 * Response from Minecraft authentication endpoint.
 */
public class MinecraftAuthResponse {
    
    @SerializedName("access_token")
    private String accessToken;
    
    @SerializedName("token_type")
    private String tokenType;
    
    @SerializedName("expires_in")
    private Integer expiresIn;
    
    private String error;
    
    @SerializedName("errorMessage")
    private String errorMessage;
    
    public String getAccessToken() {
        return accessToken;
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
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public boolean isSuccess() {
        return accessToken != null && !accessToken.isBlank();
    }
}
