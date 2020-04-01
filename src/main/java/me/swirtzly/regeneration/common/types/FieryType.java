package me.swirtzly.regeneration.common.types;

import me.swirtzly.regeneration.RegenConfig;
import me.swirtzly.regeneration.client.rendering.types.FieryRenderer;
import me.swirtzly.regeneration.common.capability.IRegen;
import me.swirtzly.regeneration.handlers.RegenObjects;
import me.swirtzly.regeneration.util.PlayerUtil;
import net.minecraft.block.FireBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;

import java.util.Iterator;

import static net.minecraft.util.math.BlockPos.getAllInBox;

/**
 * Created by Sub on 16/09/2018.
 */
public class FieryType implements RegenType<FieryRenderer> {

    private SoundEvent[] SOUNDS = new SoundEvent[]{RegenObjects.Sounds.REGENERATION_0, RegenObjects.Sounds.REGENERATION_1, RegenObjects.Sounds.REGENERATION_2, RegenObjects.Sounds.REGENERATION_3, RegenObjects.Sounds.REGENERATION_4, RegenObjects.Sounds.REGENERATION_5, RegenObjects.Sounds.REGENERATION_6,};
	
	@Override
    public void onUpdateMidRegen(PlayerEntity player, IRegen capability) {
		
		player.extinguish();
		
		if (!player.world.isRemote) {
			PlayerUtil.setPerspective((ServerPlayerEntity) player, true, false);
		}

        if (player.world.isRemote) return;

        if (player.world.getBlockState(player.getPosition()).getBlock() instanceof FireBlock)
            player.world.removeBlock(player.getPosition(), false);
		
		double x = player.posX + player.getRNG().nextGaussian() * 2;
		double y = player.posY + 0.5 + player.getRNG().nextGaussian() * 2;
		double z = player.posZ + player.getRNG().nextGaussian() * 2;
		player.world.createExplosion(player, x, y, z, 0.1F, RegenConfig.COMMON.fieryRegen.get(), Explosion.Mode.NONE);

        Iterator<BlockPos> iterator = getAllInBox(player.getPosition().north().west(), player.getPosition().south().east()).iterator();

        while (iterator.hasNext()) {
			iterator.forEachRemaining((blockPos -> {
				if (player.world.getBlockState(blockPos).getBlock() instanceof FireBlock) {
					player.world.removeBlock(blockPos, false);
				}
			}));
		}

    }
	
	@Override
    public void onFinishRegeneration(PlayerEntity player, IRegen capability) {
		PlayerUtil.setPerspective((ServerPlayerEntity) player, false, true);
		capability.setAnimationTicks(0);
	}
	
	@Override
	public int getAnimationLength() {
        return 280; // 14 seconds of 20 ticks
	}
	
	@Override
    public double getAnimationProgress(IRegen cap) {
		return Math.min(1, cap.getAnimationTicks() / (double) getAnimationLength());
	}

    @Override
    public TypeManager.Type getTypeID() {
        return TypeManager.Type.FIERY;
    }
	
	@Override
	public SoundEvent[] getRegeneratingSounds() {
		return SOUNDS;
	}

    @Override
	public Vec3d getDefaultPrimaryColor() {
		return new Vec3d(0.93F, 0.61F, 0F);
	}

    @Override
	public Vec3d getDefaultSecondaryColor() {
		return new Vec3d(1F, 0.5F, 0.18F);
	}

    @Override
	public FieryRenderer getRenderer() {
		return FieryRenderer.INSTANCE;
	}
	
}
