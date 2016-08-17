package com.pau101.paintthis.item.brush;

import java.util.Locale;
import java.util.Optional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.apache.commons.lang3.tuple.Pair;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.property.Painter;
import com.pau101.paintthis.util.Util;

public class ItemPaintbrush extends ItemBrush {
	private final Size size;

	public ItemPaintbrush(Size size) {
		this.size = size;
		setUnlocalizedName("paintbrush." + size.getUnlocalizedName());
	}

	public int getSize() {
		return size.getValue();
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return stack.getMetadata() > 0 ? 72000 : 0;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		player.setItemInUse(stack, getMaxItemUseDuration(stack));
		return stack;
	}

	@Override
	public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
		if (PaintThis.proxy.isClientPainting(player)) {
			Painter painter = (Painter) player.getExtendedProperties(Painter.IDENTIFIER);
			painter.finishStroke();
		}
	}

	@Override
	public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
		if (PaintThis.proxy.isClientPainting(player) && stack.getMetadata() > 0) {
			Painter painter = (Painter) player.getExtendedProperties(Painter.IDENTIFIER);
			Optional<Pair<EntityCanvas, Vec3>> hit = findHitCanvas(player);
			if (hit.isPresent() && hit.get().getLeft().isEditableBy(player)) {
				painter.stroke(hit.get().getLeft(), hit.get().getRight(), stack);
			} else {
				painter.finishStroke();
			}
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

		public int getValue() {
			return value;
		}

		public String getName() {
			return filename;
		}

		public String getUnlocalizedName() {
			return unlocalizedName;
		}
	}
}
