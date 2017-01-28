package com.pau101.paintthis.item.brush;

import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.capability.CapabilityHandler;
import com.pau101.paintthis.capability.Painter;
import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.util.Util;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand) {
//		player.setActiveHand(hand);
		return new ActionResult<>(EnumActionResult.FAIL, stack);
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
			EnumHand hand = player.getActiveHand();
			EnumHand opposite = hand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
			if (!PaintThis.proxy.isLookingAtDye(opposite)) {
				Painter painter = entity.getCapability(CapabilityHandler.PAINTER_CAP, null);
				Optional<Pair<EntityCanvas, Vec3d>> hit = findHitCanvas(player);
				if (hit.isPresent() && hit.get().getLeft().isEditableBy(player)) {
					painter.stroke(hit.get().getLeft(), hand, hit.get().getRight(), stack);
				} else {
					painter.finishStroke();
				}
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
