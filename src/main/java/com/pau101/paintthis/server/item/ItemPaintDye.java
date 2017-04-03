package com.pau101.paintthis.server.item;

import java.util.List;

import com.pau101.paintthis.server.dye.Dye;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class ItemPaintDye extends Item implements PainterUsable {
	public ItemPaintDye() {
		setUnlocalizedName("dye");
		setHasSubtypes(true);
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return Dye.getDyeFromDyeItemStack(stack).getUnlocalizedName();
	}

	@Override
	public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> subItems) {
		for (Dye dye : Dye.values()) {
			if (!dye.isVanilla()) {
				subItems.add(new ItemStack(item, 1, dye.getDamage()));
			}
		}
	}
}
