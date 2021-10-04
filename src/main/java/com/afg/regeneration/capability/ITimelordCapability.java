package com.afg.regeneration.capability;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Created by AFlyingGrayson on 12/30/17
 */
public interface ITimelordCapability {
    NBTTagCompound writeNBT();

    void readNBT(NBTTagCompound nbt);

    void syncToPlayer();

    boolean isTimelord();

    void setTimelord(boolean timelord);

    int getRegenTicks();

    void setRegenTicks(int ticks);

    int getRegenCount();

    void setRegenCount(int count);
}
