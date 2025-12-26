package com.karasu256.mcauth.processor;

/**
 * Mutable context passed through the processor chain.
 * Stores intermediate tokens and result data.
 */
public class TokenContext {
    private String msAuthCode;
    private String msAccessToken;
    private String msRefreshToken;
    private String xboxAccessToken;
    private String xboxXstsToken;
    private String xboxUserHash;
    private String mcAccessToken;
    private String mcUsername;
    private String mcUuid;

    public String getMsAuthCode() {
        return msAuthCode;
    }

    public void setMsAuthCode(String msAuthCode) {
        this.msAuthCode = msAuthCode;
    }

    public String getMsAccessToken() {
        return msAccessToken;
    }

    public void setMsAccessToken(String msAccessToken) {
        this.msAccessToken = msAccessToken;
    }

    public String getMsRefreshToken() {
        return msRefreshToken;
    }

    public void setMsRefreshToken(String msRefreshToken) {
        this.msRefreshToken = msRefreshToken;
    }

    public String getXboxAccessToken() {
        return xboxAccessToken;
    }

    public void setXboxAccessToken(String xboxAccessToken) {
        this.xboxAccessToken = xboxAccessToken;
    }

    public String getXboxXstsToken() {
        return xboxXstsToken;
    }

    public void setXboxXstsToken(String xboxXstsToken) {
        this.xboxXstsToken = xboxXstsToken;
    }

    public String getXboxUserHash() {
        return xboxUserHash;
    }

    public void setXboxUserHash(String xboxUserHash) {
        this.xboxUserHash = xboxUserHash;
    }

    public String getMcAccessToken() {
        return mcAccessToken;
    }

    public void setMcAccessToken(String mcAccessToken) {
        this.mcAccessToken = mcAccessToken;
    }

    public String getMcUsername() {
        return mcUsername;
    }

    public void setMcUsername(String mcUsername) {
        this.mcUsername = mcUsername;
    }

    public String getMcUuid() {
        return mcUuid;
    }

    public void setMcUuid(String mcUuid) {
        this.mcUuid = mcUuid;
    }

    public boolean hasRefreshToken() {
        return msRefreshToken != null && !msRefreshToken.isBlank();
    }

    public boolean hasAuthCode() {
        return msAuthCode != null && !msAuthCode.isBlank();
    }
}
