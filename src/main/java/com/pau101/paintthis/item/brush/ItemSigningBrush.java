package com.pau101.paintthis.item.brush;

import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.net.serverbound.MessageSignPainting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ItemSigningBrush extends ItemBrush {
	public ItemSigningBrush() {
		setUnlocalizedName("signingBrush");
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand) {
		if (PaintThis.proxy.isClientPainting(player) && stack.getMetadata() > 0) {
			Optional<Pair<EntityCanvas, Vec3d>> hit = findHitCanvas(player);
			if (hit.isPresent()) {
				EntityCanvas canvas = hit.get().getLeft();
				if (canvas.isEditableBy(player)) {
					PaintThis.networkWrapper.sendToServer(new MessageSignPainting(canvas, hand, hit.get().getRight()));
				}
			}
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, stack);
	}
}
