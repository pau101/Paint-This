package com.pau101.paintthis.server.item.brush;

import java.util.Locale;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.server.capability.CapabilityHandler;
import com.pau101.paintthis.server.util.Util;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public final class ItemPaintbrush extends ItemBrush {
	private final Size size;

	public ItemPaintbrush(Size size) {
		this.size = size;
		setUnlocalizedName("paintbrush." + size.unlocalizedName);
	}

	public int getSize() {
		return size.value;
	}

	@Override
	protected void perform(EntityPlayer player, ItemStack stack, EnumHand hand) {
		PaintThis.proxy.paint(player, stack, hand);
	}

	@Override
	public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entity, int timeLeft) {
		if (entity instanceof EntityPlayer && PaintThis.proxy.isClientPainting((EntityPlayer) entity)) {
			entity.getCapability(CapabilityHandler.PAINTER_CAP, null).finishStroke();
		}
	}

	@Override
	public void onUsingTick(ItemStack stack, EntityLivingBase entity, int count) {
		if (entity instanceof EntityPlayer && PaintThis.proxy.isClientPainting((EntityPlayer) entity) && stack.getMetadata() > 0) {
			EntityPlayer player = (EntityPlayer) entity;
			PaintThis.proxy.paint(player, stack, player.getActiveHand());
		}
	}

	public enum Size {
		SMALL,
		MEDIUM,
		LARGE;

		private final int value;

		private final String filename;

		private final String unlocalizedName;

		private Size() {
			value = ordinal() + 1;
			filename = name().toLowerCase(Locale.ROOT);
			unlocalizedName = Util.getEnumLowerCamelName(this);
		}

		public String getName() {
			return filename;
		}
	}
}
