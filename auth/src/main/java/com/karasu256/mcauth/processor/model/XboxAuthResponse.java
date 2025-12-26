package com.karasu256.mcauth.processor.model;

import com.google.gson.annotations.SerializedName;

/**
 * Response from Xbox Live authentication endpoint.
 */
public class XboxAuthResponse {
    
    @SerializedName("Token")
    private String token;
    
    @SerializedName("DisplayClaims")
    private DisplayClaims displayClaims;
    
    @SerializedName("XErr")
    private String error;
    
    @SerializedName("Message")
    private String message;
    
    public String getToken() {
        return token;
    }
    
    public DisplayClaims getDisplayClaims() {
        return displayClaims;
    }
    
    public String getError() {
        return error;
    }
    
    public String getMessage() {
        return message;
    }
    
    public boolean isSuccess() {
        return token != null && !token.isBlank();
    }
    
    public String getUserHash() {
        if (displayClaims != null && displayClaims.xui != null && displayClaims.xui.length > 0) {
            return displayClaims.xui[0].uhs;
        }
        return null;
    }
    
    public static class DisplayClaims {
        private XuiEntry[] xui;
    }
    
    public static class XuiEntry {
        private String uhs;
    }
}
