package com.directchat.ui;

import com.directchat.DirectChatMod;
import com.directchat.client.ChatInterceptor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

/**
 * Screen for displaying and solving captcha challenges.
 */
public class CaptchaScreen extends Screen {
    
    private final String captchaImageBase64;
    private final String playerUuid;
    private TextFieldWidget captchaInput;
    private String errorMessage = null;
    private boolean submitting = false;
    
    // Decoded captcha image info
    private int captchaWidth = 0;
    private int captchaHeight = 0;
    
    public CaptchaScreen(String captchaImageBase64, String playerUuid) {
        super(Text.literal("DirectChat - Captcha Verification"));
        this.captchaImageBase64 = captchaImageBase64;
        this.playerUuid = playerUuid;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Text input for captcha response
        int inputWidth = 200;
        int inputX = (this.width - inputWidth) / 2;
        int inputY = this.height / 2 + 40;
        
        captchaInput = new TextFieldWidget(
                this.textRenderer,
                inputX, inputY,
                inputWidth, 20,
                Text.literal("Enter captcha")
        );
        captchaInput.setMaxLength(32);
        captchaInput.setFocused(true);
        this.addSelectableChild(captchaInput);
        
        // Submit button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Submit"),
                button -> submitCaptcha()
        ).dimensions(inputX, inputY + 30, 95, 20).build());
        
        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Cancel"),
                button -> this.close()
        ).dimensions(inputX + 105, inputY + 30, 95, 20).build());
        
        // Try to get captcha image dimensions
        if (captchaImageBase64 != null && !captchaImageBase64.isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(captchaImageBase64);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
                if (img != null) {
                    captchaWidth = img.getWidth();
                    captchaHeight = img.getHeight();
                }
            } catch (Exception e) {
                DirectChatMod.LOGGER.error("Failed to decode captcha image", e);
            }
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        // Title
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                20,
                0xFFFFFF
        );
        
        // Instructions
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Please solve the captcha to verify you are human"),
                this.width / 2,
                40,
                0xAAAAAA
        );
        
        // Captcha image placeholder (since we can't easily render base64 images in Minecraft)
        int boxY = this.height / 2 - 40;
        int boxWidth = 200;
        int boxHeight = 60;
        int boxX = (this.width - boxWidth) / 2;
        
        // Draw captcha box background
        context.fill(boxX - 2, boxY - 2, boxX + boxWidth + 2, boxY + boxHeight + 2, 0xFF333333);
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF1A1A1A);
        
        // Show captcha text (for text-based captcha from nAntibot/similar)
        if (captchaImageBase64 != null && !captchaImageBase64.isEmpty()) {
            // If it's a simple text captcha (not base64 image), display it
            if (!captchaImageBase64.startsWith("data:") && captchaImageBase64.length() < 50) {
                context.drawCenteredTextWithShadow(
                        this.textRenderer,
                        Text.literal(captchaImageBase64),
                        this.width / 2,
                        boxY + boxHeight / 2 - 4,
                        0x00FF00
                );
            } else {
                context.drawCenteredTextWithShadow(
                        this.textRenderer,
                        Text.literal("[Captcha Image]"),
                        this.width / 2,
                        boxY + boxHeight / 2 - 4,
                        0x888888
                );
            }
        } else {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("Loading captcha..."),
                    this.width / 2,
                    boxY + boxHeight / 2 - 4,
                    0x888888
            );
        }
        
        // Input field label
        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal("Enter your answer:"),
                (this.width - 200) / 2,
                this.height / 2 + 28,
                0xFFFFFF
        );
        
        // Render text input
        captchaInput.render(context, mouseX, mouseY, delta);
        
        // Error message
        if (errorMessage != null) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal(errorMessage),
                    this.width / 2,
                    this.height / 2 + 80,
                    0xFF5555
            );
        }
        
        // Submitting indicator
        if (submitting) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("Verifying..."),
                    this.width / 2,
                    this.height / 2 + 80,
                    0xFFFF55
            );
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257) { // Enter key
            submitCaptcha();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private void submitCaptcha() {
        String response = captchaInput.getText().trim();
        
        if (response.isEmpty()) {
            errorMessage = "Please enter the captcha answer";
            return;
        }
        
        if (submitting) {
            return;
        }
        
        submitting = true;
        errorMessage = null;
        
        DirectChatMod mod = DirectChatMod.getInstance();
        mod.getApiClient().submitCaptcha(response, playerUuid)
                .thenAccept(result -> {
                    MinecraftClient.getInstance().execute(() -> {
                        submitting = false;
                        
                        if (result.isSuccess()) {
                            mod.getConfig().setAuthToken(result.token());
                            mod.setConnected(true);
                            mod.setDirectModeEnabled(true);
                            this.close();
                            ChatInterceptor.sendClientMessage("§a[DirectChat] Captcha verified! Connected successfully.");
                        } else if (result.requiresCaptcha()) {
                            errorMessage = "Incorrect captcha, please try again";
                            captchaInput.setText("");
                        } else {
                            errorMessage = result.message() != null ? result.message() : "Verification failed";
                        }
                    });
                });
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
