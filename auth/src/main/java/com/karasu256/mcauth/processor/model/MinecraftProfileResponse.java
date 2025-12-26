package com.karasu256.mcauth.processor.model;

import com.google.gson.annotations.SerializedName;

/**
 * Response from Minecraft profile endpoint.
 */
public class MinecraftProfileResponse {
    
    private String id;
    private String name;
    private String error;
    
    @SerializedName("errorMessage")
    private String errorMessage;
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getError() {
        return error;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public boolean isSuccess() {
        return id != null && !id.isBlank();
    }
    
    public String getFormattedUuid() {
        if (id == null) return null;
        return id.replaceFirst(
            "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)",
            "$1-$2-$3-$4-$5");
    }
}
