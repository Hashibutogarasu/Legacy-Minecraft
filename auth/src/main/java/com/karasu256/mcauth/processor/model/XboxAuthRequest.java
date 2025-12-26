package com.karasu256.mcauth.processor.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for Xbox Live authentication.
 */
public class XboxAuthRequest {
    
    @SerializedName("Properties")
    private Properties properties;
    
    @SerializedName("RelyingParty")
    private String relyingParty;
    
    @SerializedName("TokenType")
    private String tokenType;
    
    public XboxAuthRequest(String msAccessToken) {
        this.properties = new Properties(msAccessToken);
        this.relyingParty = "http://auth.xboxlive.com";
        this.tokenType = "JWT";
    }
    
    public static class Properties {
        @SerializedName("AuthMethod")
        private final String authMethod = "RPS";
        
        @SerializedName("SiteName")
        private final String siteName = "user.auth.xboxlive.com";
        
        @SerializedName("RpsTicket")
        private final String rpsTicket;
        
        public Properties(String msAccessToken) {
            this.rpsTicket = "d=" + msAccessToken;
        }
    }
}
