package com.pau101.paintthis.capability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

public interface HorseShearability extends ICapabilitySerializable<NBTTagCompound> {
	boolean shear(EntityLiving horse, EntityPlayer player, EnumHand hand);
}
