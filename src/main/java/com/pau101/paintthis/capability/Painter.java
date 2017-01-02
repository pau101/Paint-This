package com.pau101.paintthis.capability;

import com.pau101.paintthis.entity.item.EntityCanvas;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

public interface Painter extends ICapabilitySerializable<NBTTagCompound> {
	void stroke(EntityCanvas canvas, EnumHand hand, Vec3d stroke, ItemStack paintbrush);

	void finishStroke();
}
