package com.pau101.paintthis.server.item;

import java.util.List;

import com.pau101.paintthis.server.dye.Dye;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;

public final class ItemPalette extends Item {
	public static final int DYE_COUNT = 8;

	public ItemPalette() {
		setUnlocalizedName("palette");
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return !(newStack.getItem() instanceof ItemPalette);
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
		if (stack.hasTagCompound()) {
			byte[] dyes = stack.getTagCompound().getByteArray("dyes");
			for (int i = 0; i < dyes.length; i++) {
				if (dyes[i] != Dye.NO_DYE) {
					tooltip.add(I18n.translateToLocal(Dye.getDyeFromByte(dyes[i]).getCompleteUnlocalizedName()));
				}
			}
		}
	}

	public static boolean hasDyes(ItemStack stack) {
		if (stack == null) {
			return false;
		}
		NBTTagCompound compound = stack.getTagCompound();
		if (compound == null) {
			return false;
		}
		byte[] dyes = compound.getByteArray("dyes");
		if (dyes.length != DYE_COUNT) {
			return false;
		}
		for (int i = 0; i < DYE_COUNT; i++) {
			if (dyes[i] != Dye.NO_DYE) {
				return true;
			}
		}
		return false;
	}

	public static void removeDyesIfNone(ItemStack stack) {
		if (stack.hasTagCompound()) {
			NBTTagCompound compound = stack.getTagCompound();
			byte[] dyes = compound.getByteArray("dyes");
			for (byte dye : dyes) {
				if (dye != Dye.NO_DYE) {
					return;
				}
			}
			compound.removeTag("dyes");
			if (compound.hasNoTags()) {
				stack.setTagCompound(null);
			}
		}
	}

	public static Dye getDye(ItemStack stack, int index) {
		NBTTagCompound compound = stack.getTagCompound();
		if (compound == null) {
			return null;
		}
		byte[] dyes = compound.getByteArray("dyes");
		if (dyes.length != DYE_COUNT) {
			return null;
		}
		byte val = dyes[index];
		return val == Dye.NO_DYE ? null : Dye.getDyeFromByte(val);
	}
}
