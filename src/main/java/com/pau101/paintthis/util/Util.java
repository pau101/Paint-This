package com.pau101.paintthis.util;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.google.common.base.CaseFormat;

public final class Util {
	private Util() {}

	public static String getEnumLowerCamelName(Enum e) {
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, e.name()); 
	}

	public static boolean containsItemStack(IInventory inventory, ItemStack stack) {
		return indexOfItemStack(inventory, stack) > -1;
	}

	public static int indexOfItemStack(IInventory inventory, ItemStack stack) {
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			if (ItemStack.areItemStacksEqual(inventory.getStackInSlot(i), stack)) {
				return i;
			}
		}
		return -1;
	}
}
