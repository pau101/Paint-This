package com.pau101.paintthis.server.item.crafting.recipes;

import com.pau101.paintthis.server.dye.Dye;
import com.pau101.paintthis.server.util.OreDictUtil;

import net.minecraft.item.ItemStack;

public final class RecipeDerivePaletteAddDye extends RecipeDerivePalette {
	@Override
	protected boolean getDyeRequirement() {
		return false;
	}

	@Override
	protected boolean isOperand(ItemStack stack) {
		return OreDictUtil.isDye(stack);
	}

	@Override
	protected boolean supportsMultipleOperands() {
		return true;
	}

	@Override
	protected byte getDyeReplacement(ItemStack operand) {
		return Dye.getDyeFromDyeItemStack(operand).getByteValue();
	}

	@Override
	protected ItemStack getOutput(ItemStack derivedPalette, byte prevValue) {
		return derivedPalette;
	}
}
