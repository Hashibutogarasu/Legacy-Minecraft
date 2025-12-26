package com.karasu256.mcauth.test;

import com.karasu256.mcauth.*;
import com.karasu256.mcauth.test.mock.MockMcAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class McAuthWebServerTest {

    private MockMcAuth mcAuth;
    private WebServer webServer;

    @BeforeEach
    public void setup() {
        mcAuth = new MockMcAuth();
    }

    @AfterEach
    public void tearDown() {
        if (webServer != null) {
            webServer.stop();
        }
        mcAuth.cleanUp();
    }

    @Test
    public void testWebServerIntegration() throws Exception {
        MicrosoftAuthenticationProvider provider = new MicrosoftAuthenticationProvider("test-client-id");
        mcAuth.setProvider(provider);
        
        // Configure mock to return test data
        mcAuth.withMockContext(MockMcAuth.createMockContext());

        // Custom WebServer to handle the callback and notify McAuth
        webServer = new WebServer(25585) {
            @Override
            protected String onReceivedCode(java.util.Optional<String> code) {
                if (code.isPresent()) {
                    mcAuth.processToken(code.get());
                    return "Auth code received";
                }
                return "Error: No code found";
            }
        };

        webServer.start();

        try {
            CompletableFuture<AuthResult> authFuture = mcAuth.startAuthentication();

            // Emulate browser opening the callback URL
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:25585/callback?code=mock-auth-code"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("Auth code received", response.body());

            // Verify authentication completes
            AuthResult result = authFuture.join();
            assertTrue(result.ok());
            assertEquals("MockUser", result.username());
            assertNotNull(result.session());

        } finally {
            webServer.stop();
        }
    }
    
    @Test
    public void testWebServerWithMultipleAuthentications() throws Exception {
        MicrosoftAuthenticationProvider provider = new MicrosoftAuthenticationProvider("test-client-id");
        mcAuth.setProvider(provider);
        mcAuth.withMockContext(MockMcAuth.createMockContext());

        webServer = new WebServer(25586) {
            @Override
            protected String onReceivedCode(java.util.Optional<String> code) {
                if (code.isPresent()) {
                    mcAuth.processToken(code.get());
                    return "Auth code received";
                }
                return "Error: No code found";
            }
        };

        webServer.start();

        try {
            HttpClient client = HttpClient.newHttpClient();
            
            // First authentication
            CompletableFuture<AuthResult> authFuture1 = mcAuth.startAuthentication();
            client.send(
                HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:25586/callback?code=code-1"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            AuthResult result1 = authFuture1.join();
            assertTrue(result1.ok());
            
            // Reset mock and prepare for second authentication
            mcAuth.prepareAuthentication();
            
            // Second authentication
            CompletableFuture<AuthResult> authFuture2 = mcAuth.startAuthentication();
            client.send(
                HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:25586/callback?code=code-2"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            AuthResult result2 = authFuture2.join();
            assertTrue(result2.ok());
            
        } finally {
            webServer.stop();
        }
    }
}