package com.pau101.paintthis.server.net.serverbound;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.server.entity.item.EntityCanvas;
import com.pau101.paintthis.server.item.brush.ItemBrush;
import com.pau101.paintthis.server.item.brush.ItemPaintbrush;
import com.pau101.paintthis.server.net.MessagePainting;
import com.pau101.paintthis.server.net.clientbound.MessageUpdatePainting;
import com.pau101.paintthis.server.painting.Painting;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MessagePainterPainting extends MessagePainting {
	private EnumHand hand;

	public MessagePainterPainting() {}

	public MessagePainterPainting(EntityCanvas canvas, EnumHand hand, Painting.Change change) {
		super(canvas, change);
		this.hand = hand;
	}

	@Override
	public void serialize(PacketBuffer buf) {
		super.serialize(buf);
		buf.writeEnumValue(hand);
	}

	@Override
	public void deserialize(PacketBuffer buf) {
		super.deserialize(buf);
		hand = buf.readEnumValue(EnumHand.class);
	}

	@Override
	public void process(MessageContext ctx) {
		EntityPlayerMP player = ctx.getServerHandler().playerEntity;
		World world = player.worldObj;
		Entity entity = world.getEntityByID(canvasId);
		if (!(entity instanceof EntityCanvas)) {
			return;
		}
		EntityCanvas canvas = (EntityCanvas) entity;
		// Can't check if actually using since a short use won't be occurring on the server at the moment the packet is received
		ItemStack held = player.getHeldItem(hand);
		if (held == null || !(held.getItem() instanceof ItemPaintbrush)) {
			return;
		}
		if (player.getDistanceToEntity(canvas) > ItemBrush.REACH * 2) {
			return;
		}
		canvas.getPainting().update(x, y, width, data);
		PaintThis.sendToWatchingEntity(canvas, new MessageUpdatePainting(canvasId, x, y, width, data), player);
	}
}
