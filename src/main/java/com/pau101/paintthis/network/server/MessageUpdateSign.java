package com.pau101.paintthis.network.server;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.network.SelfProcessingMessage;
import com.pau101.paintthis.painting.Signature;

public class MessageUpdateSign implements SelfProcessingMessage {
	private int canvasId;

	private Signature signature;

	public MessageUpdateSign() {}

	public MessageUpdateSign(EntityCanvas canvas) {
		canvasId = canvas.getEntityId();
		signature = canvas.getSignature();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(canvasId);
		signature.writeToBuffer(new PacketBuffer(buf));
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		canvasId = buf.readInt();
		signature = new Signature();
		signature.readFromBuffer(new PacketBuffer(buf));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IMessage process(MessageContext ctx) {
		World world = Minecraft.getMinecraft().theWorld;
		if (world == null) {
			return null;
		}
		Entity entity = world.getEntityByID(canvasId);
		if (!(entity instanceof EntityCanvas)) {
			return null;
		}
		((EntityCanvas) entity).setSignature(signature);
		return null;
	}
}
