package com.pau101.paintthis.item;

import com.pau101.paintthis.util.OreDictUtil;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemPaletteKnife extends Item implements PainterUsable {
	public ItemPaletteKnife() {
		setUnlocalizedName("paletteKnife");
		setMaxStackSize(1);
		setMaxDamage(512);
	}

	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
		return OreDictUtil.matches(repair, "ingotIron");
	}
}
