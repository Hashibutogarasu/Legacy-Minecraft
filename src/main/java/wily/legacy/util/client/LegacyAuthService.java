package wily.legacy.util.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.karasu256.mcauth.DataEncrypter;
import com.karasu256.mcauth.McAuth;
import com.karasu256.mcauth.MicrosoftAuthenticationProvider;
import com.karasu256.mcauth.WebServer;
import com.karasu256.mcauth.processor.ProcessorProgress;
import com.karasu256.mcauth.processor.TokenContext;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.network.chat.Component;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.screen.LegacyLoadingScreen;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Singleton service wrapping the McAuth API for mod integration.
 * Provides authentication with progress screen display.
 */
public class LegacyAuthService {
    
    private static final String CLIENT_ID = "2f63b52c-2aeb-4f21-a753-12adfd4ef9fc";
    private static final int CALLBACK_PORT = 25585;
    private static LegacyAuthService instance;
    
    // Progress stage components
    public static final Component LOGIN_IN = Component.translatable("legacy.menu.choose_user.login_in");
    public static final Component ACQUIRING_MSAUTH_TOKEN = Component.translatable("legacy.menu.choose_user.stage.acquiringMSAuthCode");
    public static final Component ACQUIRING_MSACCESS_TOKEN = Component.translatable("legacy.menu.choose_user.stage.acquiringMSAccessToken");
    public static final Component ACQUIRING_XBOX_ACCESS_TOKEN = Component.translatable("legacy.menu.choose_user.stage.acquiringXboxAccessToken");
    public static final Component ACQUIRING_XBOX_XSTS_TOKEN = Component.translatable("legacy.menu.choose_user.stage.acquiringXboxXstsToken");
    public static final Component ACQUIRING_MC_ACCESS_TOKEN = Component.translatable("legacy.menu.choose_user.stage.acquiringMCAccessToken");
    public static final Component FINALIZING = Component.translatable("legacy.menu.choose_user.stage.finalizing");
    
    private final LegacyMcAuth mcAuth;
    private final List<LegacyMcAccount> accounts = new ArrayList<>();
    private final Path accountsPath;
    private final Gson gson = new Gson();
    
    private final AtomicReference<WebServer> activeServer = new AtomicReference<>();
    
    private LegacyAuthService() {
        mcAuth = new LegacyMcAuth();
        mcAuth.setProvider(new MicrosoftAuthenticationProvider(CLIENT_ID));
        accountsPath = Minecraft.getInstance().gameDirectory.toPath().resolve(".accounts.json");
    }
    
    public static LegacyAuthService getInstance() {
        if (instance == null) {
            instance = new LegacyAuthService();
        }
        return instance;
    }
    
    public void initialize() {
        loadAccounts();
    }
    
    public List<LegacyMcAccount> getAccounts() {
        return accounts;
    }
    
    private void loadAccounts() {
        accounts.clear();
        if (!Files.exists(accountsPath)) return;
        
        try {
            String content = Files.readString(accountsPath);
            JsonArray array = JsonParser.parseString(content).getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                String id = obj.has("id") ? obj.get("id").getAsString() : null;
                String name = obj.has("name") ? obj.get("name").getAsString() : null;
                String mcAccessToken = obj.has("mcAccessToken") ? obj.get("mcAccessToken").getAsString() : null;
                String msaRefreshToken = obj.has("msaRefreshToken") ? obj.get("msaRefreshToken").getAsString() : null;
                boolean encrypted = obj.has("encrypted") && obj.get("encrypted").getAsBoolean();
                
                if (id != null && name != null) {
                    accounts.add(new LegacyMcAccount(
                        UUID.fromString(id), name, mcAccessToken, msaRefreshToken, encrypted
                    ));
                }
            }
        } catch (Exception e) {
            // Log error but continue
        }
    }
    
    public void saveAccounts() {
        try {
            JsonArray jsonArray = new JsonArray();
            for (LegacyMcAccount account : accounts) {
                if (account == null) continue;
                JsonObject obj = new JsonObject();
                obj.addProperty("id", account.getUuid().toString());
                obj.addProperty("name", account.getName());
                if (account.getMcAccessToken() != null) {
                    obj.addProperty("mcAccessToken", account.getMcAccessToken());
                }
                if (account.getMsaRefreshToken() != null) {
                    obj.addProperty("msaRefreshToken", account.getMsaRefreshToken());
                }
                obj.addProperty("encrypted", account.isEncrypted());
                jsonArray.add(obj);
            }
            Files.writeString(accountsPath, gson.toJson(jsonArray));
        } catch (IOException e) {
            // Log error
        }
    }
    
    /**
     * Starts Microsoft authentication with progress screen.
     */
    /**
     * Starts Microsoft authentication with progress screen.
     */
    public CompletableFuture<LegacyMcAccount> startAuthentication(Runnable onClose, String password) {
        Minecraft minecraft = Minecraft.getInstance();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        // Prepare McAuth for new authentication session
        mcAuth.prepareAuthentication();
        
        // Cancellation flag
        AtomicBoolean cancelled = new AtomicBoolean(false);
        
        // Create and show loading screen - cancel auth on close
        LegacyLoadingScreen screen = LegacyLoadingScreen.createWithExecutor(LOGIN_IN, () -> {
            cancelled.set(true);
            mcAuth.cancelCurrentAuthentication();
            onClose.run();
        }, executor);
        minecraft.execute(() -> minecraft.setScreen(screen));
        
        // Update initial stage
        screen.setLoadingStage(ACQUIRING_MSAUTH_TOKEN);
        screen.setProgress(0f);
        
        return CompletableFuture.supplyAsync(() -> {
            // Stop any existing server
            WebServer existing = activeServer.getAndSet(null);
            if (existing != null) {
                existing.stop();
            }
            
            // Create web server to receive OAuth callback
            WebServer server = new WebServer(CALLBACK_PORT) {
                @Override
                protected String onReceivedCode(Optional<String> code) {
                    code.ifPresent(c -> mcAuth.processToken(c));
                    return code.isPresent() 
                        ? "Login successful! You can close this window."
                        : "Login failed. Please try again.";
                }
            };
            activeServer.set(server);
            
            try {
                server.start();
            } catch (IOException e) {
                throw new RuntimeException("Failed to start callback server", e);
            }
            
            // Open browser
            URI authUri = buildAuthorizationUri();
            Util.getPlatform().openUri(authUri);
            
            return server;
        }, executor).thenCompose(server -> {
            // Use McAuth's authentication which waits for processToken
            return mcAuth.startAuthentication(
                uri -> {}, // Browser already opened
                progress -> {
                    // Update loading screen with progress
                    minecraft.execute(() -> {
                        if (cancelled.get()) return;
                        screen.setLoadingStage(getStageComponent(progress.processorName()));
                        screen.setProgress(progress.getPercentage());
                    });
                }
            );
        }).thenApply(result -> {
            // Stop web server and complete authentication
            WebServer server = activeServer.getAndSet(null);
            if (server != null) {
                server.stop();
            }
            mcAuth.completeAuthentication();
            
            // Check if cancelled
            if (cancelled.get()) {
                return null;
            }
            
            if (!result.ok()) {
                throw new RuntimeException("Authentication failed");
            }
            
            // Update to finalizing stage
            minecraft.execute(() -> {
                if (cancelled.get()) return;
                screen.setLoadingStage(FINALIZING);
                screen.setProgress(1.0f);
            });
            
            // Parse session JSON
            JsonObject session = JsonParser.parseString(result.session()).getAsJsonObject();
            String uuid = session.has("uuid") ? session.get("uuid").getAsString() : null;
            String username = result.username();
            String accessToken = session.has("session") ? session.get("session").getAsString() : null;
            String refreshToken = session.has("refreshToken") ? session.get("refreshToken").getAsString() : null;
            
            // Encrypt tokens if password provided
            String storedAccess = accessToken;
            String storedRefresh = refreshToken;
            boolean isEncrypted = password != null && !password.isEmpty();
            if (isEncrypted) {
                DataEncrypter encrypter = new DataEncrypter(password);
                storedAccess = encrypter.encrypt(accessToken);
                storedRefresh = encrypter.encrypt(refreshToken);
            }
            
            LegacyMcAccount account = new LegacyMcAccount(
                uuid != null ? UUID.fromString(uuid) : UUID.randomUUID(),
                username,
                storedAccess,
                storedRefresh,
                isEncrypted
            );
            
            // Check if cancelled before closing screen
            if (!cancelled.get()) {
                minecraft.execute(() -> screen.onClose());
            }
            
            return account;
        }).exceptionally(e -> {
            // Stop server and complete authentication on error
            WebServer server = activeServer.getAndSet(null);
            if (server != null) {
                server.stop();
            }
            mcAuth.completeAuthentication();
            
            // Only show error if not cancelled
            if (!cancelled.get()) {
                minecraft.execute(() -> {
                    FactoryAPIClient.getToasts().addToast(new LegacyTip(
                        Component.translatable("legacy.menu.choose_user.failed", 
                            Component.translatable("legacy.menu.choose_user.failed.unauthorized").withStyle(ChatFormatting.RED)
                        ), 140, 46
                    ).centered());
                });
            }
            
            return null;
        });
    }
    
    /**
     * Performs login/refresh for an existing account with progress screen.
     */
    public void performLogin(LegacyMcAccount account, String password, Runnable onSuccess) {
        Minecraft minecraft = Minecraft.getInstance();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        // Cancellation flag
        AtomicBoolean cancelled = new AtomicBoolean(false);
        
        // Create and show loading screen - cancel auth on close
        LegacyLoadingScreen screen = LegacyLoadingScreen.createWithExecutor(LOGIN_IN, () -> {
            cancelled.set(true);
            mcAuth.cancelCurrentAuthentication();
        }, executor);
        minecraft.execute(() -> minecraft.setScreen(screen));
        
        // Get refresh token
        String refreshToken = account.getMsaRefreshToken();
        if (refreshToken == null) {
            // No refresh token - use existing access token
            minecraft.execute(() -> {
                if (cancelled.get()) return;
                setUser(account.toUser());
                screen.onClose();
                onSuccess.run();
            });
            return;
        }
        
        // Decrypt if encrypted
        String actualToken = refreshToken;
        if (account.isEncrypted() && password != null) {
            try {
                DataEncrypter encrypter = new DataEncrypter(password);
                actualToken = encrypter.decrypt(refreshToken);
            } catch (Exception e) {
                minecraft.execute(() -> {
                    FactoryAPIClient.getToasts().addToast(new LegacyTip(
                        Component.translatable("legacy.menu.choose_user.failed", 
                            Component.translatable("legacy.menu.choose_user.failed.incorrect_password").withStyle(ChatFormatting.RED)
                        ), 140, 46
                    ).centered());
                    screen.onClose();
                });
                return;
            }
        }
        
        // Update stage
        screen.setLoadingStage(ACQUIRING_MSACCESS_TOKEN);
        screen.setProgress(0.2f);
        
        final String finalToken = actualToken;
        
        mcAuth.refreshAuthentication(finalToken, progress -> {
            minecraft.execute(() -> {
                if (cancelled.get()) return;
                screen.setLoadingStage(getStageComponent(progress.processorName()));
                screen.setProgress(progress.getPercentage());
            });
        }).thenApply(result -> {
            // Check if cancelled
            if (cancelled.get()) {
                return null;
            }
            
            if (!result.ok()) {
                throw new RuntimeException("Refresh failed");
            }
            
            // Parse session
            JsonObject session = JsonParser.parseString(result.session()).getAsJsonObject();
            String uuid = session.has("uuid") ? session.get("uuid").getAsString() : null;
            String accessToken = session.has("session") ? session.get("session").getAsString() : null;
            String newRefresh = session.has("refreshToken") ? session.get("refreshToken").getAsString() : null;
            
            // Encrypt if needed
            String storedAccess = accessToken;
            String storedRefresh = newRefresh;
            if (account.isEncrypted() && password != null && !password.isEmpty()) {
                DataEncrypter encrypter = new DataEncrypter(password);
                storedAccess = encrypter.encrypt(accessToken);
                storedRefresh = encrypter.encrypt(newRefresh);
            }
            
            // Create updated account
            final String finalUuid = uuid;
            final String finalAccessToken = accessToken;
            LegacyMcAccount updatedAccount = new LegacyMcAccount(
                uuid != null ? UUID.fromString(uuid) : account.getUuid(),
                result.username(),
                storedAccess,
                storedRefresh,
                account.isEncrypted()
            );
            
            minecraft.execute(() -> {
                // Check if cancelled again before setting user
                if (cancelled.get()) return;
                
                // Update in list
                int idx = accounts.indexOf(account);
                if (idx >= 0) {
                    accounts.set(idx, updatedAccount);
                    saveAccounts();
                }
                
                // Set user with unencrypted token
                User user = new User(
                    result.username(),
                    finalUuid != null ? UUID.fromString(finalUuid) : account.getUuid(),
                    finalAccessToken,
                    Optional.empty(),
                    Optional.empty()
                );
                setUser(user);
                
                screen.onClose();
                onSuccess.run();
            });
            
            return result;
        }).exceptionally(e -> {
            if (cancelled.get()) return null;
            minecraft.execute(() -> {
                FactoryAPIClient.getToasts().addToast(new LegacyTip(
                    Component.translatable("legacy.menu.choose_user.failed", 
                        Component.translatable("legacy.menu.choose_user.failed.unauthorized").withStyle(ChatFormatting.RED)
                    ), 140, 46
                ).centered());
            });
            return null;
        });
    }
    
    /**
     * Maps processor name to stage component.
     */
    private Component getStageComponent(String processorName) {
        return switch (processorName) {
            case "Microsoft Access Token" -> ACQUIRING_MSACCESS_TOKEN;
            case "Xbox Access Token" -> ACQUIRING_XBOX_ACCESS_TOKEN;
            case "Xbox XSTS Token" -> ACQUIRING_XBOX_XSTS_TOKEN;
            case "Minecraft Access Token" -> ACQUIRING_MC_ACCESS_TOKEN;
            case "Complete" -> FINALIZING;
            default -> Component.literal(processorName);
        };
    }
    
    /**
     * Sets the Minecraft user session.
     */
    public static void setUser(User user) {
        LegacyMcAuth.setUser(user);
    }
    
    private URI buildAuthorizationUri() {
        String url = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize"
            + "?client_id=" + CLIENT_ID
            + "&response_type=code"
            + "&redirect_uri=http://localhost:" + CALLBACK_PORT + "/callback"
            + "&scope=XboxLive.signin%20offline_access"
            + "&state=" + UUID.randomUUID().toString().substring(0, 8);
        return URI.create(url);
    }
    
    /**
     * Creates an offline (non-Microsoft) account.
     */
    public static LegacyMcAccount createOfflineAccount(String username) {
        UUID uuid = UUID.nameUUIDFromBytes(("offline:" + username).getBytes());
        return new LegacyMcAccount(uuid, username, null, null, false);
    }
    
    /**
     * Account data wrapper.
     */
    public static class LegacyMcAccount {
        private final UUID uuid;
        private final String name;
        private final String mcAccessToken;
        private final String msaRefreshToken;
        private final boolean encrypted;
        private GameProfile profile;
        
        public LegacyMcAccount(UUID uuid, String name, String mcAccessToken, String msaRefreshToken, boolean encrypted) {
            this.uuid = uuid;
            this.name = name;
            this.mcAccessToken = mcAccessToken;
            this.msaRefreshToken = msaRefreshToken;
            this.encrypted = encrypted;
            this.profile = new GameProfile(uuid, name);
        }
        
        public UUID getUuid() {
            return uuid;
        }
        
        public String getName() {
            return name;
        }
        
        public String getMcAccessToken() {
            return mcAccessToken;
        }
        
        public String getMsaRefreshToken() {
            return msaRefreshToken;
        }
        
        public boolean isEncrypted() {
            return encrypted;
        }
        
        public GameProfile getProfile() {
            return profile;
        }
        
        public Optional<String> getRefreshToken() {
            return Optional.ofNullable(msaRefreshToken);
        }
        
        public User toUser() {
            String token = mcAccessToken != null ? mcAccessToken : "invalidtoken";
            return new User(
                name,
                uuid,
                token,
                Optional.empty(),
                Optional.empty()
            );
        }
        
        public boolean isOffline() {
            return msaRefreshToken == null;
        }
    }
}
