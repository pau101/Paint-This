package com.pau101.paintthis.server.item.crafting.recipes;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.server.item.crafting.PaintThisRecipe;
import com.pau101.paintthis.server.painting.Painting;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

public final class RecipeCanvas extends ShapedOreRecipe implements PaintThisRecipe {
	public RecipeCanvas() {
		super(new ItemStack(PaintThis.canvas, 4), "SSS", "TWT", "SSS", 'S', "stickWood", 'T', Items.STRING, 'W', Blocks.WOOL);
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inventory) {
		ItemStack stack = super.getCraftingResult(inventory);
		Painting.createNewPainting(stack, 1, 1);
		return stack;
	}
}
