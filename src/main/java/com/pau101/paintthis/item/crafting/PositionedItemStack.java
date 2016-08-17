package com.pau101.paintthis.item.crafting;

import java.util.Optional;

import net.minecraft.item.ItemStack;

public class PositionedItemStack {
	private static final int[] MOORE_INDICES = { 1, 0, 7, 2, 867-5309, 6, 3, 4, 5 };

	private static final int[] INV_MOORE_INDICES = { 1, 0, 3, 6, 7, 8, 5, 2 };

	protected final ItemStack stack;

	protected final int x;

	protected final int y;

	public PositionedItemStack(ItemStack stack, int x, int y) {
		this.stack = stack;
		this.x = x;
		this.y = y;
	}

	public ItemStack getItemStack() {
		return stack;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public Optional<PositionedItemStack.NeighborItemStack> asNeighborOf(PositionedItemStack origin) {
		int x = this.x - origin.x;
		int y = this.y - origin.y;
		if (Math.abs(x) > 1 || Math.abs(y) > 1 || x == 0 && y == 0) {
			return Optional.empty();
		}
		return Optional.of(new NeighborItemStack(stack.copy(), x, y));
	}

	public static class NeighborItemStack extends PositionedItemStack {
		private NeighborItemStack(ItemStack itemStack, int x, int y) {
			super(itemStack, x, y);
		}

		public int getNeighborIndex() {
			return MOORE_INDICES[3 * x + y + 4];
		}
	}

	public static int getXYIndex(int i) {
		return INV_MOORE_INDICES[i];
	}
}
