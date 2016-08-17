package com.pau101.paintthis.item.crafting.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.dye.Dye;
import com.pau101.paintthis.item.ItemPalette;
import com.pau101.paintthis.item.crafting.PaintThisRecipe;
import com.pau101.paintthis.item.crafting.PositionedItemStack;
import com.pau101.paintthis.item.crafting.PositionedItemStack.NeighborItemStack;

public abstract class RecipeDerivePalette implements PaintThisRecipe {
	private ItemStack output;

	public RecipeDerivePalette() {}

	@Override
	public boolean matches(InventoryCrafting inventory, World world) {
		output = null;
		int width = inventory.getWidth();
		int height = inventory.getHeight();
		PositionedItemStack palettePos = null;
		List<PositionedItemStack> operandPositions = new ArrayList<PositionedItemStack>();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				ItemStack ingredient = inventory.getStackInRowAndColumn(x, y);
				if (ingredient != null) {
					Item item = ingredient.getItem();
					if (item == PaintThis.palette) {
						if (palettePos != null || getDyeRequirement() && !ItemPalette.hasDyes(ingredient)) {
							return false;
						}
						palettePos = new PositionedItemStack(ingredient, x, y);
					} else if (isOperand(ingredient)) {
						if (!operandPositions.isEmpty() && !supportsMultipleOperands()) {
							return false;
						}
						operandPositions.add(new PositionedItemStack(ingredient, x, y));
					} else {
						return false;
					}
				}
			}
		}
		if (palettePos == null || operandPositions.isEmpty()) {
			return false;
		}
		NeighborItemStack[] relativeOperands = new NeighborItemStack[operandPositions.size()];
		for (int i = 0; i < operandPositions.size(); i++) {
			PositionedItemStack operandPos = operandPositions.get(i);
			Optional<NeighborItemStack> relativeOperandPos = operandPos.asNeighborOf(palettePos);
			if (!relativeOperandPos.isPresent()) {
				return false;
			}
			relativeOperands[i] = relativeOperandPos.get();
		}
		ItemStack palette = palettePos.getItemStack();
		NBTTagCompound compound = palette.getTagCompound();
		if (ItemPalette.hasDyes(palette)) {
			byte[] dyes = compound.getByteArray("dyes");
			for (NeighborItemStack operandPos : relativeOperands) {
				if (dyes[operandPos.getNeighborIndex()] == Dye.NO_DYE == getDyeRequirement()) {
					return false;
				}
			}
		}
		ItemStack derivedPalette = palette.copy();
		byte[] dyes;
		if (ItemPalette.hasDyes(palette)) {
			dyes = derivedPalette.getTagCompound().getByteArray("dyes");
		} else {
			dyes = new byte[ItemPalette.DYE_COUNT];
			derivedPalette.setTagInfo("dyes", new NBTTagByteArray(dyes));
		}
		byte singularPrevValue = dyes[relativeOperands[0].getNeighborIndex()];
		for (NeighborItemStack operandPos : relativeOperands) {
			dyes[operandPos.getNeighborIndex()] = getDyeReplacement(operandPos.getItemStack());
		}
		output = getOutput(derivedPalette, singularPrevValue);
		return true;
	}

	protected abstract boolean getDyeRequirement();

	protected abstract boolean isOperand(ItemStack stack);

	protected abstract boolean supportsMultipleOperands();

	protected abstract byte getDyeReplacement(ItemStack operand);

	protected abstract ItemStack getOutput(ItemStack derivedPalette, byte prevValue);

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inventory) {
		return output.copy();
	}

	@Override
	public int getRecipeSize() {
		return 9;
	}

	@Override
	public ItemStack getRecipeOutput() {
		return null;
	}

	@Override
	public ItemStack[] getRemainingItems(InventoryCrafting inventory) {
		return ForgeHooks.defaultRecipeGetRemainingItems(inventory);
	}
}
