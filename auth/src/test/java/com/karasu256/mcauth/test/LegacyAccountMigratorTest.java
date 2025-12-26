package com.karasu256.mcauth.test;

import com.google.gson.JsonObject;
import com.karasu256.mcauth.migration.LegacyAccountMigrator;
import com.karasu256.mcauth.DataEncrypter;
import com.karasu256.mcauth.processor.TokenContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LegacyAccountMigratorTest {

    @TempDir
    Path tempDir;

    @Test
    public void testMigrateUnencryptedAccount() {
        JsonObject json = new JsonObject();
        json.addProperty("id", "12345678-1234-1234-1234-123456789abc");
        json.addProperty("name", "TestPlayer");
        json.addProperty("mcAccessToken", "mc-access-token");
        json.addProperty("msaRefreshToken", "msa-refresh-token");
        json.addProperty("encrypted", false);
        
        TokenContext context = LegacyAccountMigrator.fromLegacyJson(json, null);
        
        assertNotNull(context);
        assertEquals("TestPlayer", context.getMcUsername());
        assertEquals("12345678-1234-1234-1234-123456789abc", context.getMcUuid());
        assertEquals("mc-access-token", context.getMcAccessToken());
        assertEquals("msa-refresh-token", context.getMsRefreshToken());
    }
    
    @Test
    public void testMigrateEncryptedAccount() {
        String password = "test-password";
        DataEncrypter encrypter = new DataEncrypter(password);
        
        JsonObject json = new JsonObject();
        json.addProperty("id", "12345678-1234-1234-1234-123456789abc");
        json.addProperty("name", "TestPlayer");
        json.addProperty("mcAccessToken", encrypter.encrypt("mc-access-token"));
        json.addProperty("msaRefreshToken", encrypter.encrypt("msa-refresh-token"));
        json.addProperty("encrypted", true);
        
        TokenContext context = LegacyAccountMigrator.fromLegacyJson(json, password);
        
        assertNotNull(context);
        assertEquals("mc-access-token", context.getMcAccessToken());
        assertEquals("msa-refresh-token", context.getMsRefreshToken());
    }
    
    @Test
    public void testMigrateWithWrongPasswordReturnsNull() {
        String password = "correct-password";
        DataEncrypter encrypter = new DataEncrypter(password);
        
        JsonObject json = new JsonObject();
        json.addProperty("id", "12345678-1234-1234-1234-123456789abc");
        json.addProperty("name", "TestPlayer");
        json.addProperty("mcAccessToken", encrypter.encrypt("mc-access-token"));
        json.addProperty("encrypted", true);
        
        TokenContext context = LegacyAccountMigrator.fromLegacyJson(json, "wrong-password");
        
        assertNull(context);
    }
    
    @Test
    public void testLoadAllAccountsFromFile() throws Exception {
        String json = """
            [
                {
                    "id": "uuid-1",
                    "name": "Player1",
                    "mcAccessToken": "token1",
                    "msaRefreshToken": "refresh1",
                    "encrypted": false
                },
                {
                    "id": "uuid-2",
                    "name": "Player2",
                    "mcAccessToken": "token2",
                    "msaRefreshToken": "refresh2",
                    "encrypted": false
                }
            ]
            """;
        
        Path accountsPath = tempDir.resolve(".accounts.json");
        Files.writeString(accountsPath, json);
        
        List<TokenContext> accounts = LegacyAccountMigrator.loadAllAccounts(accountsPath, null);
        
        assertEquals(2, accounts.size());
        assertEquals("Player1", accounts.get(0).getMcUsername());
        assertEquals("Player2", accounts.get(1).getMcUsername());
    }
    
    @Test
    public void testLoadNonExistentFileReturnsEmptyList() throws Exception {
        Path nonExistent = tempDir.resolve("nonexistent.json");
        
        List<TokenContext> accounts = LegacyAccountMigrator.loadAllAccounts(nonExistent, null);
        
        assertTrue(accounts.isEmpty());
    }
}
