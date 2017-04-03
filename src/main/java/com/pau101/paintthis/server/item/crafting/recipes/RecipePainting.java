package com.pau101.paintthis.server.item.crafting.recipes;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.server.item.crafting.PaintThisRecipe;
import com.pau101.paintthis.server.painting.Painting;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.ShapedOreRecipe;

public final class RecipePainting extends ShapedOreRecipe implements PaintThisRecipe {
	public RecipePainting() {
		super(new ItemStack(PaintThis.canvas, 1, 1), "SSS", "SCS", "SSS", 'S', "stickWood", 'C', PaintThis.canvas);
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inventory) {
		ItemStack painting = super.getCraftingResult(inventory);
		for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (Painting.isPainting(stack)) {
				painting.setTagCompound((NBTTagCompound) stack.getTagCompound().copy());
				painting.getSubCompound("painting", false).setBoolean("isFramed", true);
				break;
			}
		}
		return painting;
	}
}
