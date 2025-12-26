package com.karasu256.mcauth.processor.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data class for authentication session result.
 * Used for serializing the session data in AuthResult.
 */
public class AuthSessionResponse {
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("uuid")
    private String uuid;
    
    @SerializedName("session")
    private String accessToken;
    
    @SerializedName("refreshToken")
    private String refreshToken;
    
    public AuthSessionResponse() {
    }
    
    public AuthSessionResponse(String username, String uuid, String accessToken, String refreshToken) {
        this.username = username;
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
