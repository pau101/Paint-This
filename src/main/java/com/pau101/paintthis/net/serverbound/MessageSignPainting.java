package com.pau101.paintthis.net.serverbound;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.item.brush.ItemBrush;
import com.pau101.paintthis.item.brush.ItemSigningBrush;
import com.pau101.paintthis.net.PTMessage;
import com.pau101.paintthis.net.clientbound.MessageUpdateSign;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageSignPainting extends PTMessage {
	private int canvasId;

	private EnumHand hand;

	private Vec3d hit;

	public MessageSignPainting() {}

	public MessageSignPainting(EntityCanvas canvas, EnumHand hand, Vec3d hit) {
		canvasId = canvas.getEntityId();
		this.hand = hand;
		this.hit = hit;
	}

	@Override
	public void serialize(PacketBuffer buf) {
		buf.writeInt(canvasId);
		buf.writeEnumValue(hand);
		buf.writeDouble(hit.xCoord);
		buf.writeDouble(hit.yCoord);
		buf.writeDouble(hit.zCoord);
	}

	@Override
	public void deserialize(PacketBuffer buf) {
		canvasId = buf.readInt();
		hand = buf.readEnumValue(EnumHand.class);
		hit = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
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
		if (player.getHeldItem(hand) == null || !(player.getHeldItem(hand).getItem() instanceof ItemSigningBrush)) {
			return;
		}
		if (player.getDistanceToEntity(canvas) > ItemBrush.REACH * 2) {
			return;
		}
		canvas.sign(player, hand, hit);
		PaintThis.sendToWatchingEntity(canvas, new MessageUpdateSign(canvas));
	}
}
