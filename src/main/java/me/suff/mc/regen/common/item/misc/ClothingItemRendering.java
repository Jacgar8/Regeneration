package me.suff.mc.regen.common.item.misc;

import me.suff.mc.regen.client.rendering.model.armor.LivingArmor;
import me.suff.mc.regen.util.ClientUtil;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.IItemRenderProperties;
import org.jetbrains.annotations.NotNull;

import static me.suff.mc.regen.util.ClientUtil.clothingModels;

public class ClothingItemRendering implements IItemRenderProperties {

    @NotNull
    @Override
    public Model getBaseArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot armorSlot, HumanoidModel<?> _default) {
        clothingModels();
        HumanoidModel<LivingEntity> model = (HumanoidModel<LivingEntity>) ClientUtil.getArmorModel(itemStack, entityLiving);
        if (model instanceof LivingArmor) {
            ((LivingArmor) model).setLiving(entityLiving);
        }
        return model;
    }
}
