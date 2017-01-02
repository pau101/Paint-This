package com.pau101.paintthis.network.server;

import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.network.MessagePainting;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageUpdatePainting extends MessagePainting {
	public MessageUpdatePainting() {}

	public MessageUpdatePainting(int canvasId, int x, int y, int width, byte[] data) {
		super(canvasId, x, y, width, data);
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
		EntityCanvas canvas = (EntityCanvas) entity;
		canvas.getPainting().update(x, y, width, data);
	}
}
