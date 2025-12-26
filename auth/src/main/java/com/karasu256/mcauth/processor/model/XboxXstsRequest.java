package com.karasu256.mcauth.processor.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Request body for Xbox XSTS token.
 */
public class XboxXstsRequest {
    
    @SerializedName("Properties")
    private Properties properties;
    
    @SerializedName("RelyingParty")
    private String relyingParty;
    
    @SerializedName("TokenType")
    private String tokenType;
    
    public XboxXstsRequest(String xboxToken) {
        this.properties = new Properties(xboxToken);
        this.relyingParty = "rp://api.minecraftservices.com/";
        this.tokenType = "JWT";
    }
    
    public static class Properties {
        @SerializedName("SandboxId")
        private final String sandboxId = "RETAIL";
        
        @SerializedName("UserTokens")
        private final List<String> userTokens;
        
        public Properties(String xboxToken) {
            this.userTokens = List.of(xboxToken);
        }
    }
}
