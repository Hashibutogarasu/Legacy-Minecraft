package com.karasu256.mcauth.test;

import com.karasu256.mcauth.*;
import com.karasu256.mcauth.utils.GsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class McAuthE2ETest {

    private McAuth mcAuth;
    private WebServer webServer;

    private final String realClientId = "2f63b52c-2aeb-4f21-a753-12adfd4ef9fc";

    @BeforeEach
    public void setup() {
        mcAuth = new McAuth();
    }

    @AfterEach
    public void tearDown() {
        if (webServer != null) {
            webServer.stop();
        }
        mcAuth.cleanUp();
    }

    @Test
    public void testE2EStructureWithRealClient() throws Exception {
        MicrosoftAuthenticationProvider provider = new MicrosoftAuthenticationProvider(realClientId);
        mcAuth.setProvider(provider);

        AtomicReference<URI> capturedUri = new AtomicReference<>();

        webServer = new WebServer(25585) {
            @Override
            protected String onReceivedCode(Optional<String> code) {
                if (code.isPresent()) {
                    mcAuth.processToken(code.get());
                    return "Logged in! You can close this window.";
                }
                return "Error: No code received.";
            }
        };
        webServer.start();

        CompletableFuture<AuthResult> authFuture = mcAuth.startAuthentication(uri -> {
            capturedUri.set(uri);
            System.out.println(
                    "==========================================================================================");
            System.out.println("PLEASE OPEN THIS URL TO AUTHENTICATE:");
            System.out.println(uri);
            System.out.println(
                    "==========================================================================================");
        });

        long start = System.currentTimeMillis();
        while (capturedUri.get() == null && System.currentTimeMillis() - start < 5000) {
            // noinspection BusyWait
            Thread.sleep(100);
        }

        assertNotNull(capturedUri.get(), "Browser URI should be generated");
        String uriStr = capturedUri.get().toString();
        assertTrue(uriStr.contains("client_id=" + URLEncoder.encode(realClientId, StandardCharsets.UTF_8)));

        try {
            AuthResult result = authFuture.get(2, TimeUnit.MINUTES);
            assertTrue(result.ok(), "Authentication should succeed");
            assertNotNull(result.username(), "Username should be present");

            verifyEncryptionLogic(result);

        } catch (TimeoutException e) {
            System.out.println(
                    "Test timed out waiting for user interaction. This is expected in automated environments without user input.");
            throw e;
        }
    }

    @Test
    public void testE2EStructureWithRealClientNoEncryption() throws Exception {
        MicrosoftAuthenticationProvider provider = new MicrosoftAuthenticationProvider(realClientId);
        mcAuth.setProvider(provider);

        AtomicReference<URI> capturedUri = new AtomicReference<>();

        webServer = new WebServer(25585) {
            @Override
            protected String onReceivedCode(Optional<String> code) {
                if (code.isPresent()) {
                    mcAuth.processToken(code.get());
                    return "Logged in! You can close this window.";
                }
                return "Error: No code received.";
            }
        };
        webServer.start();

        CompletableFuture<AuthResult> authFuture = mcAuth.startAuthentication(uri -> {
            capturedUri.set(uri);
            System.out.println(
                    "==========================================================================================");
            System.out.println("PLEASE OPEN THIS URL TO AUTHENTICATE (No Encryption Test):");
            System.out.println(uri);
            System.out.println(
                    "==========================================================================================");
        });

        long start = System.currentTimeMillis();
        while (capturedUri.get() == null && System.currentTimeMillis() - start < 5000) {
            // noinspection BusyWait
            Thread.sleep(100);
        }

        assertNotNull(capturedUri.get(), "Browser URI should be generated");
        String uriStr = capturedUri.get().toString();
        assertTrue(uriStr.contains("client_id=" + URLEncoder.encode(realClientId, StandardCharsets.UTF_8)));

        try {
            AuthResult result = authFuture.get(2, TimeUnit.MINUTES);
            assertTrue(result.ok(), "Authentication should succeed");
            assertNotNull(result.username(), "Username should be present");
        } catch (TimeoutException e) {
            System.out.println(
                    "Test timed out waiting for user interaction. This is expected in automated environments without user input.");
            throw e;
        }
    }

    private void verifyEncryptionLogic(AuthResult result) {
        String password = "secure-password";
        String sessionData = result.session();

        String key = DataEncrypter.createKey(password, "AES/CBC/PKCS5Padding");
        DataEncrypter encrypter = new DataEncrypter(key);

        String encrypted = encrypter.encrypt(sessionData);
        assertNotNull(encrypted);
        assertNotEquals(sessionData, encrypted);

        String decrypted = encrypter.decrypt(encrypted);
        assertEquals(sessionData, decrypted);

        McAuthResult restored = GsonUtils.decode(decrypted);
        assertNotNull(restored.username());
        assertNotNull(restored.session());
    }
}
