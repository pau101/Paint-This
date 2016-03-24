package com.pau101.paintthis.item;

import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import org.apache.commons.lang3.ArrayUtils;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.dye.Dye;
import com.pau101.paintthis.item.brush.ItemBrush;

public class ItemPalette extends Item {
	public static final int DYE_COUNT = 8;

	public ItemPalette() {
		setUnlocalizedName("palette");
	}

	@Override
	public int getColorFromItemStack(ItemStack stack, int renderPass) {
		if (renderPass > 0 && stack.hasTagCompound()) {
			byte[] dyes = stack.getTagCompound().getByteArray("dyes");
			byte dye;
			if (dyes.length == DYE_COUNT && (dye = dyes[renderPass - 1]) != Dye.NO_DYE) {
				return Dye.getDyeFromByte(dye).getColor();
			}
		}
		return super.getColorFromItemStack(stack, renderPass);
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
		if (stack.hasTagCompound()) {
			byte[] dyes = stack.getTagCompound().getByteArray("dyes");
			for (int i = 0; i < dyes.length; i++) {
				if (dyes[i] != Dye.NO_DYE) {
					tooltip.add(StatCollector.translateToLocal(Dye.getDyeFromByte(dyes[i]).getCompleteUnlocalizedName()));
				}
			}	
		}
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if (!world.isRemote && hasDyes(stack)) {
			byte[] dyes = stack.getTagCompound().getByteArray("dyes");
			for (int s = player.inventory.currentItem + 1; s < InventoryPlayer.getHotbarSize(); s++) {
				ItemStack barStack = player.inventory.getStackInSlot(s);
				if (barStack != null && barStack.getItem() instanceof ItemBrush) {
					int from = barStack.getMetadata() > 0 ? ArrayUtils.indexOf(dyes, Dye.getDyeFromDamage(barStack.getMetadata() - 1).getByteValue()) + 1 : 0;
					Dye newDye = Dye.getDyeFromByte(findNextDye(dyes, from));
					barStack.setItemDamage(newDye.getBrushValue());
					((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
					break;
				}
			}
		}
		return stack;
	}

	private static byte findNextDye(byte[] dyes, int from) {
		for (int i = 0; i < dyes.length; i++) {
			byte dye = dyes[(i + from) % dyes.length];
			if (dye != Dye.NO_DYE) {
				return dye;
			}
		}
		throw new IllegalStateException("Couldn't find next dye!? " + Arrays.toString(dyes) + " " + from);
	}

	public static boolean hasDyes(ItemStack stack) {
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
}
