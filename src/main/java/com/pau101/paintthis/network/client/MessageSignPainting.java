package com.pau101.paintthis.network.client;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.item.brush.ItemBrush;
import com.pau101.paintthis.item.brush.ItemSigningBrush;
import com.pau101.paintthis.network.SelfProcessingMessage;
import com.pau101.paintthis.network.server.MessageUpdateSign;

public class MessageSignPainting implements SelfProcessingMessage {
	private int canvasId;

	private Vec3 hit;

	public MessageSignPainting() {}

	public MessageSignPainting(EntityCanvas canvas, Vec3 hit) {
		canvasId = canvas.getEntityId();
		this.hit = hit;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(canvasId);
		buf.writeDouble(hit.xCoord);
		buf.writeDouble(hit.yCoord);
		buf.writeDouble(hit.zCoord);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		canvasId = buf.readInt();
		hit = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
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
		if (player.getHeldItem() == null || !(player.getHeldItem().getItem() instanceof ItemSigningBrush)) {
			return null;
		}
		if (player.getDistanceToEntity(canvas) > ItemBrush.REACH * 2) {
			return null;
		}
		canvas.sign(player, hit);
		PaintThis.sendToWatchingEntity(canvas, new MessageUpdateSign(canvas));
		return null;
	}
}
