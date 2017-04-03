package com.pau101.paintthis.server.net.clientbound;

import com.pau101.paintthis.server.entity.item.EntityCanvas;
import com.pau101.paintthis.server.net.PTMessage;
import com.pau101.paintthis.server.painting.Signature;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class MessageUpdateSign extends PTMessage {
	private int canvasId;

	private Signature signature;

	public MessageUpdateSign() {}

	public MessageUpdateSign(EntityCanvas canvas) {
		canvasId = canvas.getEntityId();
		signature = canvas.getSignature();
	}

	@Override
	public void serialize(PacketBuffer buf) {
		buf.writeInt(canvasId);
		signature.writeToBuffer(new PacketBuffer(buf));
	}

	@Override
	public void deserialize(PacketBuffer buf) {
		canvasId = buf.readInt();
		signature = new Signature();
		signature.readFromBuffer(new PacketBuffer(buf));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void process(MessageContext ctx) {
		World world = Minecraft.getMinecraft().theWorld;
		if (world == null) {
			return;
		}
		Entity entity = world.getEntityByID(canvasId);
		if (!(entity instanceof EntityCanvas)) {
			return;
		}
		((EntityCanvas) entity).setSignature(signature);
	}
}
