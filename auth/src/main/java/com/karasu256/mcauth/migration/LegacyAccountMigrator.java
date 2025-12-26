package com.karasu256.mcauth.migration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.karasu256.mcauth.DataEncrypter;
import com.karasu256.mcauth.processor.TokenContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Migrates .accounts.json data to TokenContext format.
 */
public class LegacyAccountMigrator {
    
    private static final String MC_ACCESS_TOKEN = "mcAccessToken";
    private static final String MSA_REFRESH_TOKEN = "msaRefreshToken";
    private static final String ENCRYPTED = "encrypted";
    
    /**
     * Loads all accounts from legacy .accounts.json file.
     */
    public static List<TokenContext> loadAllAccounts(Path accountsPath, String password) throws IOException {
        if (!Files.exists(accountsPath)) {
            return new ArrayList<>();
        }
        
        String content = Files.readString(accountsPath, StandardCharsets.UTF_8);
        JsonArray array = JsonParser.parseString(content).getAsJsonArray();
        
        List<TokenContext> accounts = new ArrayList<>();
        for (JsonElement element : array) {
            TokenContext context = fromLegacyJson(element.getAsJsonObject(), password);
            if (context != null) {
                accounts.add(context);
            }
        }
        return accounts;
    }
    
    /**
     * Converts a single legacy account JSON to TokenContext.
     */
    public static TokenContext fromLegacyJson(JsonObject json, String password) {
        TokenContext context = new TokenContext();
        
        boolean encrypted = json.has(ENCRYPTED) && json.get(ENCRYPTED).getAsBoolean();
        
        String mcToken = getStringOrNull(json, MC_ACCESS_TOKEN);
        String refreshToken = getStringOrNull(json, MSA_REFRESH_TOKEN);
        
        if (encrypted && password != null) {
            DataEncrypter encrypter = new DataEncrypter(password);
            if (mcToken != null) {
                try {
                    mcToken = encrypter.decrypt(mcToken);
                } catch (Exception e) {
                    return null;
                }
            }
            if (refreshToken != null) {
                try {
                    refreshToken = encrypter.decrypt(refreshToken);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        
        context.setMcAccessToken(mcToken);
        context.setMsRefreshToken(refreshToken);
        
        if (json.has("id")) {
            context.setMcUuid(json.get("id").getAsString());
        }
        if (json.has("name")) {
            context.setMcUsername(json.get("name").getAsString());
        }
        
        return context;
    }
    
    private static String getStringOrNull(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : null;
    }
}
