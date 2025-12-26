package wily.legacy.util.client;

import com.karasu256.mcauth.McAuth;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.network.chat.Component;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.client.MinecraftAccessor;
import wily.legacy.client.LegacyTip;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Legacy4J-specific extension of McAuth that adds session management
 * and user state tracking functionality.
 */
public class LegacyMcAuth extends McAuth {
    
    // Session check state - cached for 3 minutes
    private static long lastSessionCheckTime = 0L;
    private static Boolean lastSessionCheck = null;
    
    private static final long SESSION_CHECK_INTERVAL_MS = 180_000; // 3 minutes
    
    /**
     * Changes the actual Minecraft User, and replace its value assigned during game startup in different fields.
     * Shows a toast notification on success.
     *
     * @param user New Minecraft User
     */
    public static void setUser(User user) {
        if (MinecraftAccessor.getInstance().setUser(user)) {
            Component success = Component.translatable("legacy.menu.choose_user.success", user.getName());
            FactoryAPIClient.getToasts().addToast(new LegacyTip(success, Minecraft.getInstance().font.width(success) + 110, 46) {
                @Override
                public void renderTip(GuiGraphics guiGraphics, int i, int j, float f, float l) {
                    super.renderTip(guiGraphics, i, j, f, l);
                    GameProfile profile = /*? if >1.20.2 {*/Minecraft.getInstance().getGameProfile()/*?} else {*//*user.getGameProfile()*//*?}*/;
                    PlayerFaceRenderer.draw(guiGraphics, Minecraft.getInstance().getSkinManager().createLookup(profile, true).get(), 7, (height() - 32) / 2, 32);
                }
            }.centered().disappearTime(2400).canRemove(() -> user != Minecraft.getInstance().getUser()));
            
            // Reset session check when user changes
            resetSessionCheck();
        }
    }
    
    /**
     * Checks if the actual User has an offline account, within a 3-min interval so there are no excessive checks.
     *
     * <p>Note: Because of this interval, each User change requires resetting the session check
     * via {@link #resetSessionCheck()} so that there is no erroneous check.
     *
     * @return true if the user is offline, false otherwise
     */
    public static boolean isOfflineUser() {
        // Return cached result if within interval
        if (lastSessionCheck != null && Util.getMillis() - lastSessionCheckTime <= SESSION_CHECK_INTERVAL_MS) {
            return lastSessionCheck;
        }
        
        // Update check time and set default to offline
        lastSessionCheckTime = Util.getMillis();
        lastSessionCheck = true;
        
        // Perform async session check
        CompletableFuture.runAsync(() -> {
            try {
                String server = UUID.randomUUID().toString();
                Minecraft mc = Minecraft.getInstance();
                mc.services().sessionService().joinServer(
                    mc.getUser().getProfileId(),
                    mc.getUser().getAccessToken(),
                    server
                );
                lastSessionCheck = mc.services().sessionService().hasJoinedServer(
                    mc.getUser().getName(),
                    server,
                    null
                ) == null;
            } catch (AuthenticationException e) {
                lastSessionCheck = true;
            }
        });
        
        // Return false initially to prevent flicker (actual result will be cached after async check)
        return false;
    }
    
    /**
     * Resets the session check cache.
     * Should be called when the user changes to ensure a fresh check.
     */
    public static void resetSessionCheck() {
        lastSessionCheck = null;
        lastSessionCheckTime = 0L;
    }
    
    /**
     * Gets the last session check result without triggering a new check.
     *
     * @return the last session check result, or null if not yet checked
     */
    public static Boolean getLastSessionCheck() {
        return lastSessionCheck;
    }
}
