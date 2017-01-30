package com.pau101.paintthis.item.brush;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.dye.Dye;
import com.pau101.paintthis.item.PainterUsable;
import com.pau101.paintthis.util.matrix.Matrix;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

public abstract class ItemBrush extends Item implements PainterUsable {
	public static final double REACH = 3;

	private static final Matrix MATRIX = new Matrix(4);

	public ItemBrush() {
		setHasSubtypes(true);
	}

	@Override
	public boolean isFull3D() {
		return true;
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return stack.getMetadata() > 0 ? 72000 : 0;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand) {
		EnumActionResult result;
		if (world.isRemote) {
			result = EnumActionResult.FAIL;
			if (PaintThis.proxy.isClientPainting(player) && stack.getMetadata() > 0) {
				perform(player, stack, hand);
			}
		} else {
			result = EnumActionResult.SUCCESS;
			player.setActiveHand(hand);
		}
		return new ActionResult<>(result, stack);
	}

	protected abstract void perform(EntityPlayer player, ItemStack stack, EnumHand hand);

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		String paintbrush = super.getItemStackDisplayName(stack);
		if (stack.getMetadata() > 0) {
			Dye dye = Dye.getDyeFromDamage(stack.getMetadata() - 1);
			String dyeName = I18n.translateToLocal(dye.getCompleteUnlocalizedName());
			paintbrush = I18n.translateToLocalFormatted("item.brushDyed", paintbrush, dyeName);
		}
		return paintbrush;
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return !(newStack.getItem() instanceof ItemBrush);
	}
}
