package com.pau101.paintthis.item.crafting;

import net.minecraft.item.crafting.IRecipe;

import com.google.common.base.CaseFormat;

public interface PaintThisRecipe extends IRecipe {
	public default String getRegistryName() {
		return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, getClass().getSimpleName());
	}
}
