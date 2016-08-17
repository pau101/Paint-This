package com.pau101.paintthis.item.brush;

import java.util.Optional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.apache.commons.lang3.tuple.Pair;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.network.client.MessageSignPainting;

public class ItemSigningBrush extends ItemBrush {
	public ItemSigningBrush() {
		setUnlocalizedName("signingBrush");
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if (PaintThis.proxy.isClientPainting(player) && stack.getMetadata() > 0) {
			Optional<Pair<EntityCanvas, Vec3>> hit = findHitCanvas(player);
			if (hit.isPresent()) {
				EntityCanvas canvas = hit.get().getLeft();
				if (canvas.isEditableBy(player)) {
					PaintThis.networkWrapper.sendToServer(new MessageSignPainting(canvas, hit.get().getRight()));
				}
			}
		}
		return super.onItemRightClick(stack, world, player);
	}
}
