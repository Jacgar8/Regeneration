package me.swirtzly.regeneration.common.item;

import me.swirtzly.regeneration.RegenConfig;
import me.swirtzly.regeneration.RegenerationMod;
import me.swirtzly.regeneration.common.capability.RegenCap;
import me.swirtzly.regeneration.common.entity.OverrideEntity;
import me.swirtzly.regeneration.handlers.RegenObjects;
import me.swirtzly.regeneration.util.PlayerUtil;
import me.swirtzly.regeneration.util.client.ClientUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.tardis.mod.items.SonicItem;

import java.util.Iterator;

/**
 * Created by Sub on 16/09/2018.
 */
public class FobWatchItem extends OverrideItem {

    public FobWatchItem() {
		super(new Item.Properties().setNoRepair().maxStackSize(1).group(ItemGroup.MISC));
		addPropertyOverride(new ResourceLocation("open"), (stack, worldIn, entityIn) -> {
			if (getStackTag(stack) == null || !getStackTag(stack).contains("open")) {
                return 0F; // Closed
			}
			return getOpen(stack);
		});

        addPropertyOverride(new ResourceLocation("engrave"), (stack, worldIn, entityIn) -> {
			if (getStackTag(stack) == null || !getStackTag(stack).contains("engrave")) {
                return 0F; // Default
			}
			return getEngrave(stack);
		});

    }
	
	public static CompoundNBT getStackTag(ItemStack stack) {
		if (stack.getTag() == null) {
			stack.setTag(new CompoundNBT());
			stack.getTag().putInt("open", 0);
			stack.getTag().putInt("engrave", random.nextInt(2));
		}
		return stack.getTag();
	}

    public static int getEngrave(ItemStack stack) {
		return getStackTag(stack).getInt("engrave");
	}

    public static void setEngrave(ItemStack stack, int engrave) {
		getStackTag(stack).putInt("engrave", engrave);
	}

    public static int getOpen(ItemStack stack) {
		return getStackTag(stack).getInt("open");
	}

    public static void setOpen(ItemStack stack, int amount) {
		getStackTag(stack).putInt("open", amount);
	}

    @Override
	public void onCreated(ItemStack stack, World worldIn, PlayerEntity playerIn) {
		super.onCreated(stack, worldIn, playerIn);
		stack.setDamage(0);
	}

    @Override
	public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
		if (stack.getTag() == null) {
			stack.setTag(new CompoundNBT());
			stack.getTag().putBoolean("live", false);
		} else {
			stack.getTag().putBoolean("live", false);
		}

        if (getOpen(stack) == 1) {
			if (entityIn.ticksExisted % 600 == 0) {
				setOpen(stack, 0);
			}
		}

        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
	}

    @Override
	public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {

		RayTraceResult result = SonicItem.getPosLookingAt(player, 5);
		if (result != null && result.getType() == RayTraceResult.Type.BLOCK) {
			BlockRayTraceResult blockRayTraceResult = (BlockRayTraceResult) result;
			BlockPos clickedBlock = blockRayTraceResult.getPos();
			Direction direction = Direction.getFacingFromVector((float) (player.posX - clickedBlock.getX()), (float) (player.posY - clickedBlock.getY()), (float) (player.posZ - clickedBlock.getZ()));

			AxisAlignedBB box = null;

			switch (direction) {
				case EAST:
				case WEST:
					box = new AxisAlignedBB(clickedBlock.up().north(), clickedBlock.down().south());
					break;
				case NORTH:
				case SOUTH:
					box = new AxisAlignedBB(clickedBlock.up().west(), clickedBlock.down().east());
					break;
				case DOWN:
				case UP:
					box = new AxisAlignedBB(clickedBlock.north().west(), clickedBlock.south().east());
					break;
				default:
					break;
			}

			for (Iterator<BlockPos> iterator = BlockPos.getAllInBox(new BlockPos(box.maxX, box.maxY, box.maxZ), new BlockPos(box.minX, box.minY, box.minZ)).iterator(); iterator.hasNext(); ) {
				BlockPos pos = iterator.next();
				BlockState blockState = world.getBlockState(pos);
				world.removeBlock(pos, false);
			}
		}


		ItemStack stack = player.getHeldItem(hand);

        RegenCap.get(player).map((cap) -> {

            if (!player.isSneaking()) { // transferring watch->player
				if (stack.getDamage() == RegenConfig.COMMON.regenCapacity.get())
					return msgUsageFailed(player, "regeneration.messages.transfer.empty_watch", stack);
                else if (cap.getRegenerationsLeft() == RegenConfig.COMMON.regenCapacity.get())
                    return msgUsageFailed(player, "regeneration.messages.transfer.max_regens", stack);
				
				int supply = RegenConfig.COMMON.regenCapacity.get() - stack.getDamage(), needed = RegenConfig.COMMON.regenCapacity.get() - cap.getRegenerationsLeft(), used = Math.min(supply, needed);

                if (cap.canRegenerate()) {
					setOpen(stack, 1);
					PlayerUtil.sendMessage(player, new TranslationTextComponent("regeneration.messages.gained_regens", used), true);
				} else if (!world.isRemote) {
					setOpen(stack, 1);
					PlayerUtil.sendMessage(player, new TranslationTextComponent("regeneration.messages.now_timelord"), true);
				}

                if (used < 0)
                    RegenerationMod.LOG.warn(player.getName().getUnformattedComponentText() + " Fob watch used <0 regens (supply: " + supply + ", needed:" + needed + ", used:" + used + ", capacity:" + RegenConfig.COMMON.regenCapacity.get() + ", damage:" + stack.getDamage() + ", regens:" + cap.getRegenerationsLeft());
				
				if (!cap.getPlayer().isCreative()) {
					stack.setDamage(stack.getDamage() + used);
				}

                if (world.isRemote) {
					setOpen(stack, 1);
					ClientUtil.playPositionedSoundRecord(RegenObjects.Sounds.FOB_WATCH, 1.0F, 2.0F);
				} else {
					cap.receiveRegenerations(used);
				}

                return new ActionResult<>(ActionResultType.SUCCESS, stack);
			} else { // transferring player->watch
                if (!cap.canRegenerate())
                    return msgUsageFailed(player, "regeneration.messages.transfer.no_regens", stack);
				
				if (cap.getState() != PlayerUtil.RegenState.ALIVE) {
					return msgUsageFailed(player, "regeneration.messages.transfer.not_alive", stack);
				}

                if (stack.getDamage() == 0)
                    return msgUsageFailed(player, "regeneration.messages.transfer.full_watch", stack);
				
				stack.setDamage(stack.getDamage() - 1);
				PlayerUtil.sendMessage(player, "regeneration.messages.transfer.success", true);

                if (world.isRemote) {
					ClientUtil.playPositionedSoundRecord(SoundEvents.BLOCK_FIRE_EXTINGUISH, 5.0F, 2.0F);
				} else {
					setOpen(stack, 1);
					cap.extractRegeneration(1);
				}

                return new ActionResult<>(ActionResultType.SUCCESS, stack);
			}
		});
		return new ActionResult<>(ActionResultType.SUCCESS, stack);

    }
	
	private ActionResult<ItemStack> msgUsageFailed(PlayerEntity player, String message, ItemStack stack) {
		PlayerUtil.sendMessage(player, message, true);
		return ActionResult.newResult(ActionResultType.FAIL, stack);
	}

    @Override
	public void update(OverrideEntity itemOverride) {
		if (!itemOverride.world.isRemote) return;
		ItemStack itemStack = itemOverride.getItem();
		if (itemStack.getItem() == this && itemStack.getDamage() != RegenConfig.COMMON.regenCapacity.get()) {
			if (itemOverride.ticksExisted % 5000 == 0 || itemOverride.ticksExisted == 2) {
				ClientUtil.playSound(itemOverride, RegenObjects.Sounds.FOB_WATCH_DIALOGUE.getRegistryName(), SoundCategory.AMBIENT, false, () -> !itemOverride.isAlive(), 1.5F);
			}
		}
	}

    @Override
	public int getMaxDamage(ItemStack stack) {
		return RegenConfig.COMMON.regenCapacity.get();
	}
}
