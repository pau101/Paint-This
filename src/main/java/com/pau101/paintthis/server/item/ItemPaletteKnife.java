package com.pau101.paintthis.server.item;

import com.pau101.paintthis.server.util.OreDictUtil;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class ItemPaletteKnife extends Item implements PainterUsable {
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
