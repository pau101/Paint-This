package com.pau101.paintthis.network.client;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.item.brush.ItemBrush;
import com.pau101.paintthis.item.brush.ItemPaintbrush;
import com.pau101.paintthis.network.MessagePainting;
import com.pau101.paintthis.network.server.MessageUpdatePainting;
import com.pau101.paintthis.painting.Painting;

public class MessagePainterPainting extends MessagePainting {
	public MessagePainterPainting() {}

	public MessagePainterPainting(EntityCanvas canvas, Painting.Change change) {
		super(canvas, change);
	}

	@Override
	public IMessage process(MessageContext ctx) {
		EntityPlayerMP player = ctx.getServerHandler().playerEntity;
		World world = player.worldObj;
		Entity entity = world.getEntityByID(canvasId);
		if (!(entity instanceof EntityCanvas)) {
			return null;
		}
		EntityCanvas canvas = (EntityCanvas) entity;
		// Can't check if actually using since a short use won't be occuring on the server at the moment the packet is recieved
		if (player.getHeldItem() == null || !(player.getHeldItem().getItem() instanceof ItemPaintbrush)) {
			return null;
		}
		if (player.getDistanceToEntity(canvas) > ItemBrush.REACH * 2) {
			return null;
		}
		canvas.getPainting().update(x, y, width, data);
		PaintThis.sendToWatchingEntity(canvas, new MessageUpdatePainting(canvasId, x, y, width, data), player);
		return null;
	}
}
