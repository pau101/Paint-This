package com.pau101.paintthis.server.item.crafting;

import com.google.common.base.CaseFormat;

import net.minecraft.item.crafting.IRecipe;

public interface PaintThisRecipe extends IRecipe {
	public default String getRegistryName() {
		return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, getClass().getSimpleName());
	}
}
