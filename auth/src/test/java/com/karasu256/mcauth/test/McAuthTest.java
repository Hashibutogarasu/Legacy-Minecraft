package com.karasu256.mcauth.test;

import com.karasu256.mcauth.*;
import com.karasu256.mcauth.processor.TokenContext;
import com.karasu256.mcauth.test.mock.MockMcAuth;
import com.karasu256.mcauth.utils.FileOperationUtils;
import com.karasu256.mcauth.utils.GsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class McAuthTest {

    @TempDir
    Path tempDir;
    
    private MockMcAuth mcAuth;

    @BeforeEach
    public void setup() {
        mcAuth = new MockMcAuth();
    }
    
    @AfterEach
    public void tearDown() {
        mcAuth.cleanUp();
    }

    @Test
    public void testFullFlowWithEncryption() {
        Path pathToSaveCredentials = tempDir.resolve(".accounts.json");

        String clientId = "test-client-id";
        String password = "test-password";
        String expectedUsername = "MockUser";

        String encryptionKey = DataEncrypter.createKey(password, "AES/CBC/PKCS5Padding");
        DataEncrypter encrypter = new DataEncrypter(encryptionKey);

        MicrosoftAuthenticationProvider provider = new MicrosoftAuthenticationProvider(clientId);
        mcAuth.setProvider(provider);
        mcAuth.setRequestTimeOut(5000);
        
        // Configure mock to return test data
        mcAuth.withMockContext(MockMcAuth.createMockContext());

        CompletableFuture<AuthResult> authFuture = mcAuth.startAuthentication();

        // Simulate OAuth callback
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(100);
                mcAuth.processToken("auth-code-123");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        AuthResult result = authFuture.join();

        assertTrue(result.ok(), "pass authentication");
        assertEquals(expectedUsername, result.username(), "Username equals");

        String encryptedJson = encrypter.encrypt(result.session());

        FileOperationUtils.write(encryptedJson, pathToSaveCredentials);
        assertTrue(pathToSaveCredentials.toFile().exists(), "file exists");

        String loadedContent = FileOperationUtils.read(pathToSaveCredentials);
        String decryptedJson = encrypter.decrypt(loadedContent);

        McAuthResult restoredResult = GsonUtils.decode(decryptedJson);
        assertEquals(result.username(), restoredResult.username(), "Dev");
    }
    
    @Test
    public void testAuthenticationWithMockResult() {
        String clientId = "test-client-id";
        MicrosoftAuthenticationProvider provider = new MicrosoftAuthenticationProvider(clientId);
        mcAuth.setProvider(provider);
        
        // Configure direct mock result
        String mockSession = "{\"username\":\"TestUser\",\"session\":\"test-token\"}";
        mcAuth.withMockResult(new AuthResult(true, "TestUser", mockSession));
        mcAuth.withMockContext(MockMcAuth.createMockContext());

        CompletableFuture<AuthResult> authFuture = mcAuth.startAuthentication();

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
                mcAuth.processToken("any-code");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        AuthResult result = authFuture.join();

        assertTrue(result.ok());
        assertEquals("TestUser", result.username());
        assertEquals(mockSession, result.session());
    }
    
    @Test
    public void testPrepareAuthenticationCancelsPrevious() {
        String clientId = "test-client-id";
        MicrosoftAuthenticationProvider provider = new MicrosoftAuthenticationProvider(clientId);
        mcAuth.setProvider(provider);
        mcAuth.withMockContext(MockMcAuth.createMockContext());
        
        // Start first authentication
        mcAuth.prepareAuthentication();
        assertTrue(mcAuth.isAuthenticationInProgress());
        
        // Start second authentication (should cancel first)
        mcAuth.prepareAuthentication();
        assertTrue(mcAuth.isAuthenticationInProgress());
        
        // Complete authentication
        mcAuth.completeAuthentication();
        assertFalse(mcAuth.isAuthenticationInProgress());
    }
}