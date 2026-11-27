package com.minecrafteft.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.stash.StashMenu;
import net.minecraft.client.renderer.RenderType;
import com.mojang.math.Axis;
import com.mojang.logging.LogUtils;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;

/** Renders TACZ's real gun model once per exact stack appearance and fits its visible bounds to a stash footprint. */
public final class TaczGunPreviewRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int BAKE_SIZE = 256;
    private static final int MAX_PREVIEWS = 64;
    private static final int MAX_BAKES_PER_TICK = 4;
    private static final int ALPHA_THRESHOLD = 8;
    private static final Map<String, Preview> PREVIEWS = new LinkedHashMap<>(16, 0.75F, true);
    private static TextureTarget target;
    private static long lastBakeTick = Long.MIN_VALUE;
    private static int bakesThisTick;

    private TaczGunPreviewRenderer() { }

    public static boolean render(GuiGraphics graphics, ItemStack stack, int x, int y, int width, int height) {
        return render(graphics,stack,x,y,width,height,false);
    }

    public static boolean render(GuiGraphics graphics, ItemStack stack, int x, int y, int width, int height, boolean caseOpen) {
        if (stack.isEmpty() || width <= 0 || height <= 0) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return false;

        IGun gun = IGun.getIGunOrNull(stack);
        // TACZ's actual per-stack identity is stored in GunId. Do not key previews only by
        // the shared registered item / IGun wrapper: that can make distinct guns reuse a model.
        String gunId = stack.hasTag() && stack.getTag().contains("GunId")
                ? stack.getTag().getString("GunId")
                : gun == null ? "unknown" : String.valueOf(gun.getGunId(stack));
        ItemStack displayGun=StashMenu.isWeaponCase(stack) && caseOpen
                ? com.minecrafteft.stash.WeaponCaseInventory.firstGunInItem(stack):ItemStack.EMPTY;
        if(!displayGun.isEmpty() && !CaseStoredGunRenderer.ready(displayGun)) return false;
        String key = StashMenu.isWeaponCase(stack) ? "minecrafteft:"+com.minecrafteft.block.WeaponCaseKind.of(stack).id()
                + (caseOpen ? "_open_v11#"+(displayGun.isEmpty()?"empty":displayKey(displayGun,
                String.valueOf(IGun.getIGunOrNull(displayGun).getGunId(displayGun)))) : "_closed_v11") : displayKey(stack, gunId);
        Preview preview = PREVIEWS.get(key);
        if (preview == null) {
            if (!canBakeThisTick(minecraft)) return false;
            bakesThisTick++;
            preview = bake(minecraft, stack, key, caseOpen,displayGun);
            if (preview == null) return false;
            PREVIEWS.put(key, preview);
            trimCache(minecraft);
        }

        int sourceWidth = preview.maxX - preview.minX + 1;
        int sourceHeight = preview.maxY - preview.minY + 1;
        // Keep the weapon's captured aspect ratio. Stretching this rectangle to the slot makes
        // pistols squat and can make attached parts look like a second, overlapping gun.
        float fit = Math.min((width - 2.0F) / sourceWidth, (height - 2.0F) / sourceHeight);
        if (!Float.isFinite(fit) || fit <= 0.0F) return false;
        int drawWidth = Math.max(1, Math.round(sourceWidth * fit));
        int drawHeight = Math.max(1, Math.round(sourceHeight * fit));
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;
        int sourceY = BAKE_SIZE - 1 - preview.maxY;
        graphics.blit(preview.texture(), drawX, drawY, drawWidth, drawHeight,
                preview.minX, sourceY, sourceWidth, sourceHeight, BAKE_SIZE, BAKE_SIZE);
        return true;
    }

    private static boolean canBakeThisTick(Minecraft minecraft) {
        long tick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        if (tick != lastBakeTick) {
            lastBakeTick = tick;
            bakesThisTick = 0;
        }
        return bakesThisTick < MAX_BAKES_PER_TICK;
    }

    /** Mirrors HDWS: appearance cache keys include GunId and attachments, but ignore live ammo state. */
    private static String displayKey(ItemStack stack, String gunId) {
        net.minecraft.nbt.CompoundTag appearance = stack.getTag() == null
                ? new net.minecraft.nbt.CompoundTag() : stack.getTag().copy();
        appearance.remove("GunCurrentAmmoCount");
        appearance.remove("HasBulletInBarrel");
        appearance.remove("GunFireMode");
        appearance.remove("MinecraftEFTStashRotated");
        return gunId + "#" + appearance;
    }

    private static Preview bake(Minecraft minecraft, ItemStack stack, String key, boolean caseOpen,ItemStack displayGun) {
        RenderTarget previewTarget = getTarget();
        if (previewTarget == null) return null;

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSorting = RenderSystem.getVertexSorting();
        PoseStack modelView = RenderSystem.getModelViewStack();
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int[] scissorBox = new int[4];
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
        boolean rendered = false;
        boolean taczScreenContext = false;
        Bounds bounds = null;
        float scale = BAKE_SIZE * 0.5F / 1.6F;
        float offsetX = 0.0F;
        float offsetY = 0.0F;

        try {
            buffers.endBatch();
            RenderSystem.disableScissor();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            // Expanded stash items are rendered after the vanilla Screen render event has
            // ended. TACZ selects its high-poly gun model only while this context is active.
            if (!StashMenu.isWeaponCase(stack)) {
                com.tacz.guns.client.renderer.item.ScreenItemVboContext.beforeScreen(null);
                taczScreenContext = true;
            }
            Matrix4f projection = new Matrix4f().setOrtho(0.0F, BAKE_SIZE, BAKE_SIZE, 0.0F, 1000.0F, 21000.0F);
            RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);
            modelView.pushPose();
            modelView.setIdentity();
            modelView.translate(0.0F, 0.0F, -11000.0F);
            RenderSystem.applyModelViewMatrix();

            for (int attempt = 0; attempt < 8; attempt++) {
                clearTarget(previewTarget);
                renderGun(minecraft, buffers, stack, scale, offsetX, offsetY, caseOpen,displayGun);
                // Item renderers (including TACZ's custom gun renderer) may change the active
                // framebuffer while flushing their buffers. Rebind our capture target before
                // reading pixels, otherwise the bounds/texture can come from another render pass.
                previewTarget.bindWrite(true);
                bounds = readBounds();
                if (bounds == null) {
                    scale *= 2.5F;
                    offsetX = 0.0F;
                    offsetY = 0.0F;
                    continue;
                }

                float centerErrorX = (bounds.minX() + bounds.maxX()) * 0.5F - BAKE_SIZE * 0.5F;
                float centerErrorY = (bounds.minY() + bounds.maxY()) * 0.5F - BAKE_SIZE * 0.5F;
                offsetX = clampOffset(offsetX + centerErrorX / scale);
                offsetY = clampOffset(offsetY + centerErrorY / scale);

                int span = Math.max(bounds.width(), bounds.height());
                if (touchesBorder(bounds)) {
                    scale *= 0.62F;
                    continue;
                }
                if (span >= BAKE_SIZE * 0.86F && span <= BAKE_SIZE * 0.94F) {
                    rendered = true;
                    break;
                }
                float factor = Math.min(4.0F, BAKE_SIZE * 0.90F / Math.max(1, span));
                scale *= Math.max(0.62F, factor);
            }

            if (!rendered && bounds != null && !touchesBorder(bounds)) rendered = true;
            if (!rendered || bounds == null) return null;

            // Keep the final copy tied to this gun's capture even if the renderer changed targets.
            previewTarget.bindWrite(true);
            DynamicTexture texture = copyTargetToDynamicTexture();
            if (texture == null) return null;
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MinecraftEFTMod.MOD_ID,
                    "gun_previews/" + UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            minecraft.getTextureManager().register(id, texture);
            return new Preview(id, bounds.minX(), bounds.maxX(), bounds.minY(), bounds.maxY());
        } catch (Throwable error) {
            LOGGER.error("Failed to create TACZ stash preview for {}", key, error);
            return null;
        } finally {
            buffers.endBatch();
            if (taczScreenContext) {
                com.tacz.guns.client.renderer.item.ScreenItemVboContext.afterScreen(null);
            }
            mainTarget.bindWrite(true);
            RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();
            if (scissorEnabled) {
                RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
            } else {
                RenderSystem.disableScissor();
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static TextureTarget getTarget() {
        if (target != null) return target;
        try {
            target = new TextureTarget(BAKE_SIZE, BAKE_SIZE, true, Minecraft.ON_OSX);
            target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        } catch (Throwable ignored) {
            return null;
        }
        return target;
    }

    private static void clearTarget(RenderTarget renderTarget) {
        renderTarget.bindWrite(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
        RenderSystem.clearDepth(1.0D);
        renderTarget.clear(Minecraft.ON_OSX);
        // RenderTarget.clear() unbinds the framebuffer. Bind AFTER clearing so the gun
        // is drawn into the capture texture rather than the default framebuffer.
        renderTarget.bindWrite(true);
        RenderSystem.enableDepthTest();
    }

    private static void renderGun(Minecraft minecraft, MultiBufferSource.BufferSource buffers,
                                  ItemStack stack, float scale, float offsetX, float offsetY, boolean caseOpen,ItemStack displayGun) {
        PoseStack pose = new PoseStack();
        pose.translate(BAKE_SIZE * 0.5F - scale * offsetX,
                BAKE_SIZE * 0.5F + scale * offsetY, 0.0F);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(StashMenu.isWeaponCase(stack) ? 22.0F : 15.0F));
        Lighting.setupFor3DItems();
        RenderSystem.enableDepthTest();
        if (StashMenu.isWeaponCase(stack)) {
            var kind=com.minecrafteft.block.WeaponCaseKind.of(stack);
            pose.mulPose(Axis.YP.rotationDegrees(180));
            if(kind.small()) pose.translate(-.85,caseOpen?-.5:-.2,-.4);
            else pose.translate(-1.35,-.6,-.8);
            WeaponCaseRenderer.renderCaseModel(pose,buffers,kind,caseOpen?1:0,0x00F000F0,displayGun);
        } else {
            minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                    0x00F000F0, OverlayTexture.NO_OVERLAY, pose, buffers, null, 0);
        }
        buffers.endBatch();
    }

    private static Bounds readBounds() {
        ByteBuffer pixels = readPixels();
        try {
            int minX = BAKE_SIZE;
            int minY = BAKE_SIZE;
            int maxX = -1;
            int maxY = -1;
            for (int y = 0; y < BAKE_SIZE; y++) {
                int row = y * BAKE_SIZE * 4;
                for (int x = 0; x < BAKE_SIZE; x++) {
                    if ((pixels.get(row + x * 4 + 3) & 0xFF) <= ALPHA_THRESHOLD) continue;
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
            return maxX < minX || maxY < minY ? null : new Bounds(minX, maxX, minY, maxY);
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }

    private static DynamicTexture copyTargetToDynamicTexture() {
        ByteBuffer pixels = readPixels();
        try {
            NativeImage image = new NativeImage(BAKE_SIZE, BAKE_SIZE, true);
            for (int y = 0; y < BAKE_SIZE; y++) {
                int row = y * BAKE_SIZE * 4;
                for (int x = 0; x < BAKE_SIZE; x++) {
                    int offset = row + x * 4;
                    int rgba = (pixels.get(offset) & 0xFF)
                            | ((pixels.get(offset + 1) & 0xFF) << 8)
                            | ((pixels.get(offset + 2) & 0xFF) << 16)
                            | ((pixels.get(offset + 3) & 0xFF) << 24);
                    image.setPixelRGBA(x, BAKE_SIZE - 1 - y, rgba);
                }
            }
            return new DynamicTexture(image);
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }

    private static ByteBuffer readPixels() {
        ByteBuffer pixels = MemoryUtil.memAlloc(BAKE_SIZE * BAKE_SIZE * 4);
        int packAlignment = GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        try {
            GL11.glReadPixels(0, 0, BAKE_SIZE, BAKE_SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            return pixels;
        } finally {
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, packAlignment);
        }
    }

    private static boolean touchesBorder(Bounds bounds) {
        return bounds.minX() <= 1 || bounds.minY() <= 1
                || bounds.maxX() >= BAKE_SIZE - 2 || bounds.maxY() >= BAKE_SIZE - 2;
    }

    private static float clampOffset(float offset) {
        return Math.max(-4.0F, Math.min(4.0F, offset));
    }

    private static void trimCache(Minecraft minecraft) {
        while (PREVIEWS.size() > MAX_PREVIEWS) {
            String oldestKey = PREVIEWS.keySet().iterator().next();
            Preview removed = PREVIEWS.remove(oldestKey);
            minecraft.getTextureManager().release(removed.texture());
        }
    }

    private record Bounds(int minX, int maxX, int minY, int maxY) {
        int width() { return maxX - minX + 1; }
        int height() { return maxY - minY + 1; }
    }

    private record Preview(ResourceLocation texture, int minX, int maxX, int minY, int maxY) { }
}

