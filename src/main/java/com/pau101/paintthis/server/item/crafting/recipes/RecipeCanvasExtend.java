package com.pau101.paintthis.server.item.crafting.recipes;

import java.util.Random;
import java.util.UUID;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.server.item.crafting.PaintThisRecipe;
import com.pau101.paintthis.server.painting.Painting;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

public final class RecipeCanvasExtend implements PaintThisRecipe {
	private ItemStack output;

	@Override
	public boolean matches(InventoryCrafting inventory, World world) {
		int width = inventory.getWidth();
		int height = inventory.getHeight();
		ItemStack[] found = new ItemStack[width * height];
		boolean foundRectangle = false;
		int canvasX = 0, canvasY = 0;
		int canvasesAcross = 0, canvasesDown = 0;
		int mergedWidth = 0, mergedHeight = 0;
		output = null;
		for (int testX = 0; testX < width; testX++) {
			for (int testY = 0; testY < height; testY++) {
				ItemStack stack = inventory.getStackInRowAndColumn(testX, testY);
				if (foundRectangle) {
					if (found[testX + testY * width] != null) {
						// This is apart of the already found rectangle of canvases
						continue;
					}
					if (stack != null) {
						// Extra stacks are present
						return false;
					}
				}
				if (stack != null) {
					canvasX = testX;
					canvasY = testY;
					rectangle:
						for (int x = testX; x < width; x++, canvasesAcross++) {
							int down = 0;
							boolean first = true;
							for (int y = testY; y < height; y++, down++) {
								stack = inventory.getStackInRowAndColumn(x, y);
								if (!Painting.isPainting(stack) || stack.getMetadata() != 0) {
									if (first) {
										// We got a rectangle of canvases which didn't end in the last column
										break rectangle;
									} else {
										// Finished this column
										break;
									}
								}
								int canvasHeight = Painting.getPaintingHeight(stack);
								if (x > 0) {
									ItemStack left = found[x - 1 + y * width];
									if (left != null) {
										if (Painting.getPaintingHeight(left) != canvasHeight) {
											// Unequal merge heights
											return false;
										}
									}
								}
								if (x == testX) {
									mergedHeight += canvasHeight;
									if (mergedHeight > PaintThis.MAX_CANVAS_SIZE) {
										// Surpassed maximum height
										return false;
									}
								}
								int canvasWidth = Painting.getPaintingWidth(stack);
								if (y > 0) {
									ItemStack right = found[x + (y - 1) * width];
									if (right != null) {
										if (Painting.getPaintingWidth(right) != canvasWidth) {
											// Unequal merge widths
											return false;
										}
									}
								}
								if (y == testY) {
									mergedWidth += canvasWidth;
									if (mergedWidth > PaintThis.MAX_CANVAS_SIZE) {
										// Surpassed maximum width
										return false;
									}
								}
								first = false;
								foundRectangle = true;
								found[x + y * width] = stack;
							}
							if (canvasesDown == 0) {
								canvasesDown = down;
							} else if (canvasesDown != down) {
								// Incomplete rectangle
								return false;
							}
						}
					if (canvasesAcross == 0) {
						// The first found stack was not a canvas
						return false;
					}
				}
			}
		}
		if (canvasesAcross == 0) {
			// There were no canvases
			return false;
		}
		if (canvasesAcross == 1 && canvasesDown == 1) {
			// single canvas
			return false;
		}
		output = new ItemStack(PaintThis.canvas);
		Painting mergedPainting = new Painting();
		mergedPainting.setDimensions(mergedWidth, mergedHeight);
		byte[] mergeData = mergedPainting.getData();
		long seed = 1;
		boolean needsUUIDAssignment = false;
		for (int down = 0, py = 0; down < canvasesDown; down++) {
			int stepY = 0;
			for (int across = 0, px = 0; across < canvasesAcross; across++) {
				ItemStack stack = found[canvasX + across + (canvasY + down) * width];
				Painting painting = Painting.getNonActivePainting(stack);
				byte[] data = painting.getData();
				for (int y = 0; y < painting.getPixelHeight(); y++) {
					for (int x = 0; x < painting.getPixelWidth(); x++) {
						mergeData[px + x + (py + y) * mergedPainting.getPixelWidth()] = data[x + y * painting.getPixelWidth()];
					}
				}
				px += painting.getPixelWidth();
				if (across == 0) {
					stepY = painting.getPixelHeight();
				}
				if (painting.hasAssignedUUID()) {
					seed = seed * 31 + painting.getUUID().hashCode();
					needsUUIDAssignment = true;
				}
			}
			py += stepY;
		}
		if (needsUUIDAssignment) {
			Random rand = new Random(seed);
			byte[] name = new byte[16];
			rand.nextBytes(name);
			mergedPainting.assignUUID(UUID.nameUUIDFromBytes(name));
		}
		NBTTagCompound compound = new NBTTagCompound();
		mergedPainting.writeToNBT(compound);
		output.setTagInfo("painting", compound);
		return true;
	}

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
		return new ItemStack(PaintThis.canvas);
	}

	@Override
	public ItemStack[] getRemainingItems(InventoryCrafting inventory) {
		return ForgeHooks.defaultRecipeGetRemainingItems(inventory);
	}
}
