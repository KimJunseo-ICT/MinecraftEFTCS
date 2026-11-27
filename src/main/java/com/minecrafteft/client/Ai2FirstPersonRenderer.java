package com.minecrafteft.client;

import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.medical.MedikitItem;
import com.minecrafteft.medical.MedikitType;
import com.minecrafteft.medical.Ai2Treatment;
import com.minecrafteft.medical.Ai2TreatmentProgress;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = MinecraftEFTMod.MOD_ID, value = Dist.CLIENT)
public final class Ai2FirstPersonRenderer {
    private static final Ai2Motion.Pose IDLE = Ai2Motion.sample(0);
    private static final boolean YSM_265 = ModList.get().getModContainerById("yes_steve_model")
            .map(mod -> "2.6.5-forge+mc1.20.1".equals(mod.getModInfo().getVersion().toString())).orElse(false);
    private record ReturningKit(InteractionHand hand, int slot, MedikitItem item) {}
    private static ReturningKit returning;
    private static IfakMotion.Variant ifakVariant = IfakMotion.Variant.WRAP;

    @SubscribeEvent
    public static void started(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof LocalPlayer player && event.getItem().getItem() instanceof MedikitItem kit
                && kit.type == MedikitType.IFAK) {
            ifakVariant = player.getRandom().nextBoolean() ? IfakMotion.Variant.WRAP : IfakMotion.Variant.INJECT;
        }
    }

    private Ai2FirstPersonRenderer() {}

    @SubscribeEvent
    public static void finished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        var result = event.getResultStack();
        if (result.getItem() instanceof MedikitItem item && result.getDamageValue() < result.getMaxDamage()) {
            returning = new ReturningKit(player.getUsedItemHand(), player.getInventory().selected, item);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void hand(RenderHandEvent event) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.isInvisible()) return;
        boolean active = Ai2Treatment.using(player);
        if (returning != null && (active || !player.getCooldowns().isOnCooldown(returning.item())
                || !player.getItemInHand(returning.hand()).is(returning.item())
                || (returning.hand() == InteractionHand.MAIN_HAND && player.getInventory().selected != returning.slot()))) {
            returning = null;
        }
        if (!active && event.getItemStack().getItem() instanceof com.minecrafteft.inventory.DogtagItem) {
            event.setCanceled(true);
            renderDogtag(event, player);
            return;
        }
        if (!active && !(event.getItemStack().getItem() instanceof MedikitItem)) return;
        event.setCanceled(true);
        if (active && event.getHand() != player.getUsedItemHand()) return;
        if (!active && event.getItemStack().getDamageValue() >= event.getItemStack().getMaxDamage()) return;

        MedikitType type = ((MedikitItem)(active ? player.getUseItem() : event.getItemStack()).getItem()).type;
        boolean mirror = (event.getHand() == InteractionHand.MAIN_HAND
                ? player.getMainArm() : player.getMainArm().getOpposite()) == HumanoidArm.LEFT;
        boolean slim = "slim".equals(player.getModelName());
        var ysmArms = YSM_265 ? Ysm265Ai2Arms.active(player) : null;
        float t = 0;
        if (active) {
            t = Math.min(com.minecrafteft.status.StatusUseTiming.elapsed(player,player.getUseItemRemainingTicks(),event.getPartialTick()) / 20F,
                    type.useTicks / 20F);
        } else if (returning != null && event.getHand() == returning.hand()) {
            float remaining = player.getCooldowns().getCooldownPercent(returning.item(), event.getPartialTick());
            t = type.readySeconds() - remaining * Ai2TreatmentProgress.REUSE_DELAY_TICKS / 20F;
        }
        boolean supportSleeve = player.isModelPartShown(sleeve(!mirror));
        var supportArm = ysmArms == null ? null : ysmArms.side(!mirror);
        float radius = supportArm != null ? 1.65F / 16F * supportArm.widthScale()
                : Ai2Motion.skinRadius(slim, supportSleeve);
        if (type == MedikitType.IFAK) {
            var motion = IfakMotion.sample(t, ifakVariant, radius, ysmArms != null);
            renderArms(event, player, active, motion.left(), motion.right(), mirror, slim, ysmArms);
            IfakKitModel.performance(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
                    motion, ifakVariant, active, mirror, radius);
            return;
        }
        Ai2Motion.Pose motion;
        if (ysmArms == null) {
            motion = t == 0 ? IDLE : Ai2Motion.sample(t, slim, supportSleeve);
        } else {
            motion = Ai2YsmMotion.sample(t, radius);
        }
        PoseStack p = event.getPoseStack();
        var buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        renderArms(event, player, active, motion.left(), motion.right(), mirror, slim, ysmArms);
        p.pushPose();
        if (mirror) p.scale(-1, 1, 1);
        p.pushPose();
        p.translate(motion.kit().x, motion.kit().y, motion.kit().z);
        p.mulPose(motion.kitRotation());
        Ai2KitModel.kit(p, buffers, motion.lid(), light);
        p.popPose();
        if (active && motion.syringeVisible()) {
            p.translate(motion.syringe().x, motion.syringe().y, motion.syringe().z);
            p.mulPose(motion.syringeRotation());
            Ai2KitModel.syringe(p, buffers, motion.plunger(), light);
        }
        p.popPose();
    }

    private static void renderDogtag(RenderHandEvent event, LocalPlayer player) {
        boolean left = (event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm()
                : player.getMainArm().getOpposite()) == HumanoidArm.LEFT;
        float partial = event.getPartialTick();
        float step = player.walkAnimation.position(partial);
        float speed = player.walkAnimation.speed(partial);
        float attack = net.minecraft.util.Mth.sin(event.getSwingProgress() * (float)Math.PI);
        var grip = new Vector3f(.27F, -.025F - event.getEquipProgress()*.36F + attack*.045F, -.64F);
        var arms = YSM_265 ? Ysm265Ai2Arms.active(player) : null;
        renderArms(event,player,false,grip,grip,left,"slim".equals(player.getModelName()),arms);
        var pose = event.getPoseStack();
        pose.pushPose();
        try {
            pose.translate(left ? -grip.x : grip.x,grip.y,grip.z-.035F);
            pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(net.minecraft.util.Mth.sin(step)*speed*12 + attack*10));
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(left ? 16 : -16));
            pose.scale(.38F,.38F,.38F);
            pose.translate(0,-.47,0);
            Minecraft.getInstance().getItemRenderer().renderStatic(event.getItemStack(),
                    net.minecraft.world.item.ItemDisplayContext.NONE,event.getPackedLight(),OverlayTexture.NO_OVERLAY,
                    pose,event.getMultiBufferSource(),player.level(),player.getId());
        } finally { pose.popPose(); }
    }

    private static void renderArms(RenderHandEvent event, LocalPlayer player, boolean active,
                                   Vector3f leftGrip, Vector3f rightGrip, boolean mirror, boolean slim, Ai2YsmMotion.Arms ysmArms) {
        var p = event.getPoseStack();
        var buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        if (Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            if (active) arm(p, buffers, light, player, renderer, leftGrip, true, mirror, slim, ysmArms);
            arm(p, buffers, light, player, renderer, rightGrip, false, mirror, slim, ysmArms);
        }
    }

    private static PlayerModelPart sleeve(boolean left) {
        return left ? PlayerModelPart.LEFT_SLEEVE : PlayerModelPart.RIGHT_SLEEVE;
    }

    private static void arm(PoseStack p, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                            PlayerRenderer renderer, Vector3f grip, boolean support, boolean mirror, boolean slim,
                            Ai2YsmMotion.Arms ysmArms) {
        boolean left = support != mirror;
        Vector3f palm = new Vector3f(grip);
        var root = Ai2Motion.shoulder(support);
        if (mirror) { palm.x = -palm.x; root.x = -root.x; }
        p.pushPose();
        try {
            p.translate(palm.x, palm.y, palm.z);
            p.mulPose(Ai2Motion.armRotation(palm, support, mirror));
            float reach = palm.distance(root);
            var binding = ysmArms == null ? null : ysmArms.side(left);
            if (binding != null) {
                p.pushPose();
                try {
                    p.scale(binding.widthScale(), reach / binding.length(), binding.widthScale());
                    p.mulPose(binding.toVertical());
                    p.translate(-binding.palm().x, -binding.palm().y, -binding.palm().z);
                    if (ForgeHooksClient.renderSpecificFirstPersonArm(p, buffers, light, player,
                            left ? HumanoidArm.LEFT : HumanoidArm.RIGHT)) return;
                } finally {
                    p.popPose();
                }
            }
            var model = renderer.getModel();
            var skin = player.getSkinTextureLocation();
            p.scale(Ai2Motion.ARM_SCALE, reach / .75F, Ai2Motion.ARM_SCALE);
            // Mesh coordinates relative to the shoulder pivot. Both skin types end at Y=10.
            p.translate((left ? -1 : 1) * (slim ? .5F : 1F) / 16F, -10F / 16F, 0);
            renderPart(left ? model.leftArm : model.rightArm, p, buffers.getBuffer(RenderType.entitySolid(skin)), light);
            if (player.isModelPartShown(sleeve(left)))
                renderPart(left ? model.leftSleeve : model.rightSleeve, p, buffers.getBuffer(RenderType.entityTranslucent(skin)), light);
        } finally {
            p.popPose();
        }
    }

    // Reuse the player's arm and sleeve meshes without carrying third-person bob or use poses into the rig.
    private static void renderPart(ModelPart part, PoseStack p, VertexConsumer vertices, int light) {
        var previous = part.storePose();
        boolean visible = part.visible;
        try {
            part.setPos(0, 0, 0);
            part.setRotation(0, 0, 0);
            part.visible = true;
            part.render(p, vertices, light, OverlayTexture.NO_OVERLAY);
        } finally {
            part.loadPose(previous);
            part.visible = visible;
        }
    }

    @SubscribeEvent public static void movement(MovementInputUpdateEvent event) {
        if(!Ai2Treatment.using(event.getEntity())) return;
        // Vanilla applies .2 use-item slowdown immediately after this event.
        // Compensate that multiplier to allow ordinary walking, never sprinting.
        if(!event.getEntity().isPassenger()) {
            event.getInput().leftImpulse*=5;
            event.getInput().forwardImpulse*=5;
        }
        event.getInput().jumping=false;
        event.getEntity().setSprinting(false);
    }
}
