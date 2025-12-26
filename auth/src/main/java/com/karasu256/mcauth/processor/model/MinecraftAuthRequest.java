package com.karasu256.mcauth.processor.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for Minecraft authentication.
 */
public class MinecraftAuthRequest {
    
    @SerializedName("identityToken")
    private final String identityToken;
    
    public MinecraftAuthRequest(String userHash, String xstsToken) {
        this.identityToken = "XBL3.0 x=" + userHash + ";" + xstsToken;
    }
}
