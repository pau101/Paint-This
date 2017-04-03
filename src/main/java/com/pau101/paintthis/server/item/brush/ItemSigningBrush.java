package com.pau101.paintthis.server.item.brush;

import com.pau101.paintthis.PaintThis;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

public final class ItemSigningBrush extends ItemBrush {
	public ItemSigningBrush() {
		setUnlocalizedName("signingBrush");
	}

	@Override
	protected void perform(EntityPlayer player, ItemStack stack, EnumHand hand) {
		PaintThis.proxy.sign(player, stack, hand);
	}
}
