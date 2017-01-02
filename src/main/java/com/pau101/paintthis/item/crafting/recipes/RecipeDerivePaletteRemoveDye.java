package com.pau101.paintthis.item.crafting.recipes;

import java.util.Random;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.dye.Dye;
import com.pau101.paintthis.item.ItemPalette;

public class RecipeDerivePaletteRemoveDye extends RecipeDerivePalette {
	private static final Random RANDOM = new Random();

	private ItemStack replacementPalette;

	@Override
	protected boolean getDyeRequirement() {
		return true;
	}

	@Override
	protected boolean isOperand(ItemStack stack) {
		return stack.getItem() == PaintThis.paletteKnife;
	}

	@Override
	protected boolean supportsMultipleOperands() {
		return false;
	}

	@Override
	protected byte getDyeReplacement(ItemStack operand) {
		return Dye.NO_DYE;
	}

	@Override
	protected ItemStack getOutput(ItemStack derivedPalette, byte prevValue) {
		replacementPalette = derivedPalette;
		ItemPalette.removeDyesIfNone(derivedPalette);
		return Dye.getDyeFromByte(prevValue).createItemStack();
	}

	@Override
	public ItemStack[] getRemainingItems(InventoryCrafting inventory) {
		ItemStack[] remaining = new ItemStack[inventory.getSizeInventory()];
		for (int i = 0; i < remaining.length; i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (stack != null) {
				Item item = stack.getItem();
				if (item == PaintThis.palette) {
					stack = replacementPalette;
				} else if (item == PaintThis.paletteKnife && stack.attemptDamageItem(1, RANDOM)) {
					stack = null;
				}
			}
			remaining[i] = stack;
		}
		return remaining;
	}
}
