package me.fril.regeneration.client;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

import me.fril.regeneration.RegenerationMod;
import me.fril.regeneration.common.capability.CapabilityRegeneration;
import me.fril.regeneration.common.capability.IRegeneration;
import me.fril.regeneration.common.types.RegenTypes;
import me.fril.regeneration.network.MessageRegenChoice;
import me.fril.regeneration.network.NetworkHandler;
import me.fril.regeneration.util.RegenObjects;
import me.fril.regeneration.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Created by Sub
 * on 16/09/2018.
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = RegenerationMod.MODID)
public class ClientEventHandler {
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void onRenderHand(RenderHandEvent e) {
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		float f = 0.2F;
		
		if (player.getHeldItemMainhand().getItem() != Items.AIR)
			return;
		
		IRegeneration cap = CapabilityRegeneration.getForPlayer(player);
		
		if (!cap.isGlowing() || mc.gameSettings.thirdPersonView > 0) return;
		
		
		GlStateManager.pushMatrix();
		
		float leftHandedFactor = mc.gameSettings.mainHand.equals(EnumHandSide.RIGHT) ? 1 : -1;
		GlStateManager.translate(0.33F*leftHandedFactor, -0.23F, -0.5F); //move in place
		GlStateManager.translate(-.8F*player.swingProgress*leftHandedFactor, -.8F*player.swingProgress, -.4F*player.swingProgress); //compensate for 'punching' motion
		GlStateManager.translate(-(player.renderArmYaw - player.prevRenderArmYaw)/400F, (player.renderArmPitch - player.prevRenderArmPitch)/500F, 0); //compensate for 'swinging' motion
		
		RenderUtil.setupRenderLightning();
		GlStateManager.rotate((mc.player.ticksExisted + RenderUtil.renderTick) / 2F, 0, 1, 0);
		for (int i = 0; i < 15; i++) {
			GlStateManager.rotate((mc.player.ticksExisted + RenderUtil.renderTick) * i / 70F, 1, 1, 0);
			Color primaryColor = cap.getPrimaryColor();
			
			Random rand = player.world.rand;
			RenderUtil.drawGlowingLine(new Vec3d((-f / 2F) + rand.nextFloat() * f, (-f / 2F) + rand.nextFloat() * f, (-f / 2F) + rand.nextFloat() * f), new Vec3d((-f / 2F) + rand.nextFloat() * f, (-f / 2F) + rand.nextFloat() * f, (-f / 2F) + rand.nextFloat() * f), 0.1F, primaryColor, 0);
		}
		RenderUtil.finishRenderLightning();
		
		GlStateManager.popMatrix();
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void onUpdate(LivingEvent.LivingUpdateEvent e) {
		if (!(e.getEntityLiving() instanceof EntityPlayerSP) || Minecraft.getMinecraft().player == null) return;
		
		EntityPlayer player = (EntityPlayer) e.getEntityLiving();
		IRegeneration regeneration = CapabilityRegeneration.getForPlayer(player);
		
		if (regeneration.isRegenerating() && !regeneration.isInGracePeriod() && // player is actually regenerating or choosing
				regeneration.getSolaceTicks() > 0 &&
				Minecraft.getMinecraft().player.getEntityId() == player.getEntityId()) {
			Minecraft.getMinecraft().gameSettings.thirdPersonView = 2;
		}
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void onClientTick(TickEvent.ClientTickEvent e) {
		EntityPlayer player = Minecraft.getMinecraft().player;
		if (player == null || Minecraft.getMinecraft().world == null) return;
		
		// apparently there's no check if we're actually in a situation where we have to choose between grace/immediate regen, that happens on the server side (packet handler)
		if (RegenKeyBinds.ENTER_GRACE.isPressed()) {
			NetworkHandler.INSTANCE.sendToServer(new MessageRegenChoice(true));
			
			IRegeneration cap = CapabilityRegeneration.getForPlayer(player);
			if (cap.getSolaceTicks() > 0 && cap.getTicksRegenerating() == 0) { // player has chosen to enter grace period, set the perspective back to 0
				Minecraft.getMinecraft().gameSettings.thirdPersonView = 0;
			}
		}
		
		if (RegenKeyBinds.REGEN_NOW.isPressed()) {
			NetworkHandler.INSTANCE.sendToServer(new MessageRegenChoice(false));
		}
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void cameraUpdate(EntityViewRenderEvent.FOVModifier e) {
		if (Minecraft.getMinecraft().player == null) return;
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		IRegeneration cap = CapabilityRegeneration.getForPlayer(player);
		
		if (cap.getTicksRegenerating() >= 1 && cap.getType() == RegenTypes.LAYDOWN || cap.getSolaceTicks() > 0 && cap.getSolaceTicks() < 200 && !cap.isInGracePeriod()) {
			e.setFOV(30);
		}
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void onRenderPlayerPre(RenderPlayerEvent.Pre e) {
		EntityPlayer player = e.getEntityPlayer();
		IRegeneration cap = CapabilityRegeneration.getForPlayer(player);
		
		if (cap.isRegenerating() && cap.getSolaceTicks() >= 200 && !cap.isInGracePeriod()) {
			cap.getType().onRenderPlayerPre(e);
			
			/*if (handler.getType().getType().isLaying()) { XXX reimplement somehow? I'm not sure how to handle this
				RenderUtil.renderPlayerLaying(e, player);
			}*/
		}
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void keyInput(InputUpdateEvent e) {
		if (Minecraft.getMinecraft().player == null) return;
		
		IRegeneration cap = CapabilityRegeneration.getForPlayer(Minecraft.getMinecraft().player);
		if (cap.isRegenerating() && !cap.isInGracePeriod() && cap.getType().blockMovement()) { //locking user
			MovementInput moveType = e.getMovementInput();
			moveType.rightKeyDown = false;
			moveType.leftKeyDown = false;
			moveType.backKeyDown = false;
			moveType.jump = false;
			moveType.moveForward = 0.0F;
			moveType.sneak = false;
			moveType.moveStrafe = 0.0F;
		}
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void registerModels(ModelRegistryEvent ev) {
		for (Item item : RegenObjects.ITEMS) {
			RenderUtil.setItemRender(item);
		}
		RegenObjects.ITEMS = new ArrayList<>();
	}
	
}
