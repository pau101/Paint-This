package com.pau101.paintthis.item;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.painting.Painting;

public class ItemCanvas extends Item {
	public ItemCanvas() {
		setUnlocalizedName("canvas");
		setMaxStackSize(16);
		setHasSubtypes(true);
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		String canvas = super.getItemStackDisplayName(stack);
		int width = Painting.getPaintingWidth(stack);
		int height = Painting.getPaintingHeight(stack);
		if (width > 0 && height > 0) {
			if (stack.getMetadata() != 0) {
				canvas = StatCollector.translateToLocal("item.painting.name");
			}
			canvas = StatCollector.translateToLocalFormatted("item.canvas.size", width, height, canvas);
		}
		return canvas;
	}

	@Override
	public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> subItems) {
		for (int w = 1; w <= PaintThis.MAX_CANVAS_SIZE; w++) {
			for (int h = 1; h <= PaintThis.MAX_CANVAS_SIZE; h++) {
				ItemStack stack = new ItemStack(item, 1);
				Painting.createNewPainting(stack, w, h);
				subItems.add(stack);
			}
		}
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (side.getAxis() == Axis.Y) {
			return false;
		}
		BlockPos hangingPosition = pos.offset(side);
		if (!player.canPlayerEdit(hangingPosition, side, stack)) {
			return false;
		} else if (Painting.isPainting(stack)) {
			EntityCanvas canvas = new EntityCanvas(world, stack.copy(), false, hangingPosition, side);
			if (canvas.onValidSurface() || canvas.doALittleJigToValidSurface()) {
				if (!world.isRemote) {
					canvas.onCreated();
					world.spawnEntityInWorld(canvas);
				}
				stack.stackSize--;
			}
			return true;
		}
		return false;
	}
}
