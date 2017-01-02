package com.pau101.paintthis.item;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class ItemPaletteKnife extends Item {
	private static final ItemStack REPAIR = new ItemStack(Items.IRON_INGOT, 1, OreDictionary.WILDCARD_VALUE);

	public ItemPaletteKnife() {
		setUnlocalizedName("paletteKnife");
		setMaxStackSize(1);
		setMaxDamage(512);
	}

	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
		return OreDictionary.itemMatches(REPAIR, repair, false);
	}
}
