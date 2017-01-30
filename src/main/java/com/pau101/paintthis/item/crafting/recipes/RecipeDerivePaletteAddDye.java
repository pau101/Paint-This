package com.pau101.paintthis.item.crafting.recipes;

import com.pau101.paintthis.dye.Dye;
import com.pau101.paintthis.util.OreDictUtil;

import net.minecraft.item.ItemStack;

public class RecipeDerivePaletteAddDye extends RecipeDerivePalette {
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
