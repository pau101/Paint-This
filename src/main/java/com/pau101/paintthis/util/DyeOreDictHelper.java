package com.pau101.paintthis.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class DyeOreDictHelper {
	private static final String[] DYE_NAMES = { "dyeBlack", "dyeRed", "dyeGreen", "dyeBrown", "dyeBlue", "dyePurple", "dyeCyan", "dyeLightGray", "dyeGray", "dyePink", "dyeLime", "dyeYellow", "dyeLightBlue", "dyeMagenta", "dyeOrange", "dyeWhite" };

	private static List<ItemStack>[] dyeItemStacks;

	private static List<ItemStack> allDyeItemStacks;

	private DyeOreDictHelper() {}

	public static boolean isDye(ItemStack stack) {
		if (stack != null) {
			if (stack.getItem() == Items.dye) {
				return true;
			}
			initDyeItemStacks();
			for (ItemStack dye : allDyeItemStacks) {
				if (OreDictionary.itemMatches(dye, stack, false)) {
					return true;
				}
			}
		}
		return false;
	}

	public static int getDyeDamage(ItemStack stack) {
		if (stack != null) {
			if (stack.getItem() == Items.dye) {
				return stack.getMetadata();
			}
			initDyeItemStacks();
			for (int i = 0; i < DYE_NAMES.length; i++) {
				List<ItemStack> dyes = dyeItemStacks[i];
				for (ItemStack dye : dyes) {
					if (OreDictionary.itemMatches(dye, stack, false)) {
						return i;
					}
				}
			}
		}
		return -1;
	}

	public static String getDyeNameFromDamage(int damage) {
		if (damage < 0 || damage >= DYE_NAMES.length) {
			return "";
		}
		return DYE_NAMES[damage];
	}

	private static void initDyeItemStacks() {
		if (dyeItemStacks == null) {
			dyeItemStacks = new List[DYE_NAMES.length];
			allDyeItemStacks = new ArrayList<ItemStack>(OreDictionary.getOres("dye"));
			for (int i = 0; i < DYE_NAMES.length; i++) {
				dyeItemStacks[i] = OreDictionary.getOres(DYE_NAMES[i]);
				allDyeItemStacks.addAll(dyeItemStacks[i]);
			}
		}
	}
}
