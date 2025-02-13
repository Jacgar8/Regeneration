package me.suff.mc.regen.handlers;

import me.suff.mc.regen.common.advancement.TriggerManager;
import me.suff.mc.regen.common.commands.RegenCommand;
import me.suff.mc.regen.common.item.HandItem;
import me.suff.mc.regen.common.objects.REntities;
import me.suff.mc.regen.common.regen.IRegen;
import me.suff.mc.regen.common.regen.RegenCap;
import me.suff.mc.regen.common.regen.state.RegenStates;
import me.suff.mc.regen.common.traits.RegenTraitRegistry;
import me.suff.mc.regen.config.RegenConfig;
import me.suff.mc.regen.util.PlayerUtil;
import me.suff.mc.regen.util.RConstants;
import me.suff.mc.regen.util.RegenSources;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEvents {

    /* Attach Capability to all LivingEntities */
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (canBeGiven(event.getObject())) {
            event.addCapability(RConstants.CAP_REGEN_ID, new ICapabilitySerializable<CompoundTag>() {
                final RegenCap regen = new RegenCap((LivingEntity) event.getObject());
                final LazyOptional<IRegen> regenInstance = LazyOptional.of(() -> regen);

                @Nonnull
                @Override
                public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @javax.annotation.Nullable Direction side) {
                    return cap == RegenCap.CAPABILITY ? (LazyOptional<T>) regenInstance : LazyOptional.empty();
                }

                @Override
                public CompoundTag serializeNBT() {
                    return regen.serializeNBT();
                }

                @Override
                public void deserializeNBT(CompoundTag nbt) {
                    regen.deserializeNBT(nbt);
                }
            });
        }
    }

    public static boolean canBeGiven(Entity entity) {
        boolean isLiving = entity instanceof LivingEntity && entity.getType() != EntityType.ARMOR_STAND;
        boolean ignoresConfig = entity.getType() == REntities.TIMELORD.get() || entity.getType() == EntityType.PLAYER;

        if (isLiving && ignoresConfig) {
            return true;
        }

        if (isLiving) { //Always make sure the entity is living, because we are explicility casting to LivingEntity later on
            return RegenConfig.COMMON.mobsHaveRegens.get();    //Base on the config value
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void noFire(LivingAttackEvent event) {
        if (event.getEntityLiving() == null) return;
        RegenCap.get(event.getEntityLiving()).ifPresent((iRegen -> {
            if (iRegen.regenState() == RegenStates.REGENERATING && RegenConfig.COMMON.regenFireImmune.get() && event.getSource().isFire() || iRegen.regenState() == RegenStates.REGENERATING && event.getSource().isExplosion()) {
                event.setCanceled(true);
            }
        }));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void noFall(LivingFallEvent event) {
        if (event.getEntityLiving() == null) return;
        RegenCap.get(event.getEntityLiving()).ifPresent((iRegen -> {
            if (iRegen.trait().getRegistryName().toString().equals(RegenTraitRegistry.LEAP.get().getRegistryName().toString())) {
                event.setCanceled(true);
            }
        }));
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity livingEntity = event.getEntityLiving();

        if (livingEntity == null) return;

        RegenCap.get(livingEntity).ifPresent(iRegen -> {

            Entity trueSource = event.getSource().getEntity();
            if (event.getSource().isFire() && iRegen.trait().getRegistryName().toString().equals(RegenTraitRegistry.FIRE.get().getRegistryName().toString())) {
                event.setCanceled(true);
                event.setAmount(0.0F);
                return;
            }

            if (trueSource instanceof Player player && event.getEntityLiving() != null) {
                RegenCap.get(player).ifPresent((data) -> data.stateManager().onPunchEntity(event));
            }

            // Stop certain damages
            if (event.getSource() == RegenSources.REGEN_DMG_KILLED)
                return;

            //Update Death Message
            iRegen.setDeathMessage(event.getSource().getLocalizedDeathMessage(livingEntity).getString());

            //Stop falling for leap trait
            if (iRegen.trait().getRegistryName().toString().equals(RegenTraitRegistry.LEAP.get().getRegistryName().toString())) {
                if (event.getSource() == DamageSource.FALL) {
                    event.setCanceled(true);//cancels damage, in case the above didn't cut it
                    return;
                }
            }

            //Handle Post
            if (iRegen.regenState() == RegenStates.POST && event.getSource() != DamageSource.OUT_OF_WORLD && event.getSource() != RegenSources.REGEN_DMG_HAND) {
                event.setAmount(1.5F);
                PlayerUtil.sendMessage(livingEntity, new TranslatableComponent("regen.messages.reduced_dmg"), true);
            }

            //Handle Death
            if (iRegen.regenState() == RegenStates.REGENERATING && RegenConfig.COMMON.regenFireImmune.get() && event.getSource().isFire() || iRegen.regenState() == RegenStates.REGENERATING && event.getSource().isExplosion()) {
                event.setCanceled(true);//cancels damage, in case the above didn't cut it
                return;
            }

            //regen and death checks moved to LivingDamageEvent and LivingDeathEvent
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void adMortemInimicusButForGrace(LivingDamageEvent event) {
        if (event.getEntityLiving() == null) return;
        RegenCap.get(event.getEntityLiving()).ifPresent((cap -> {
            if ((cap.regenState().isGraceful()) && event.getEntityLiving().getHealth() - event.getAmount() < 0) {
                //uh oh, we're dying in grace. Forcibly regenerate before all (?) death prevention mods
                boolean notDead = cap.stateManager().onKilled(event.getSource());
                event.setCanceled(notDead);
            }
        }));
    }


    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void adMortemInimicus(LivingDeathEvent event) {
        if (event.getEntityLiving() == null) return;
        RegenCap.get(event.getEntityLiving()).ifPresent((cap) -> {
            if ((event.getSource() == RegenSources.REGEN_DMG_CRITICAL || event.getSource() == RegenSources.REGEN_DMG_KILLED)) {
                cap.setTrait(RegenTraitRegistry.BORING.get());
                if (RegenConfig.COMMON.loseRegensOnDeath.get()) {
                    cap.extractRegens(cap.regens());
                }
                if (event.getEntityLiving() instanceof ServerPlayer)
                    cap.syncToClients((ServerPlayer) event.getEntityLiving());
                return;
            }
            if (cap.stateManager() == null) return;
            boolean notDead = cap.stateManager().onKilled(event.getSource());
            event.setCanceled(notDead);
        });

    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        LivingEntity livingEntity = event.getEntityLiving();
        RegenCap.get(livingEntity).ifPresent((data) -> event.setCanceled(data.regenState() == RegenStates.REGENERATING));
    }


    //TODO
 /*   @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Capability.ICapabilitySerializable <IRegen> storage = RegenCap.CAPABILITY.getStorage();
        event.getOriginal().revive();
        RegenCap.get(event.getOriginal()).ifPresent((old) -> RegenCap.get(event.getPlayer()).ifPresent((data) -> {
            CompoundTag nbt = (CompoundTag) storage.writeNBT(RegenCap.CAPABILITY, old, null);
            storage.readNBT(RegenCap.CAPABILITY, data, null, nbt);
        }));
    }*/

    @SubscribeEvent
    public static void onTrackPlayer(PlayerEvent.StartTracking startTracking) {
        RegenCap.get(startTracking.getPlayer()).ifPresent(iRegen -> {
            iRegen.syncToClients(null);
        });
    }

    @SubscribeEvent
    public static void onPunchBlock(PlayerInteractEvent.LeftClickBlock e) {
        if (e.getPlayer().level.isClientSide) return;
        RegenCap.get(e.getPlayer()).ifPresent((data) -> data.stateManager().onPunchBlock(e));
    }

    @SubscribeEvent
    public static void onLive(LivingEvent.LivingUpdateEvent livingUpdateEvent) {
        RegenCap.get(livingUpdateEvent.getEntityLiving()).ifPresent(IRegen::tick);

        if (livingUpdateEvent.getEntityLiving() instanceof ServerPlayer) {
            if (shouldGiveCouncilAdvancement((ServerPlayer) livingUpdateEvent.getEntity())) {
                TriggerManager.COUNCIL.trigger((ServerPlayer) livingUpdateEvent.getEntityLiving());
            }
        }
    }

    public static boolean shouldGiveCouncilAdvancement(ServerPlayer serverPlayerEntity) {
        EquipmentSlot[] equipmentSlotTypes = new EquipmentSlot[]{EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET};
        for (EquipmentSlot equipmentSlotType : equipmentSlotTypes) {
            if (!serverPlayerEntity.getItemBySlot(equipmentSlotType).getItem().getRegistryName().getPath().contains("robes")) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        RegenCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onCut(PlayerInteractEvent.RightClickItem event) {
        if (event.getItemStack().getItem() instanceof DiggerItem || event.getItemStack().getItem() instanceof SwordItem) {
            Player player = event.getPlayer();
            RegenCap.get(player).ifPresent((data) -> {
                if (data.regenState() == RegenStates.POST && player.isShiftKeyDown() & data.handState() == IRegen.Hand.NO_GONE) {
                    HandItem.createHand(player);
                }
            });
        }
    }


}
