package com.pau101.paintthis.server.net;

import com.pau101.paintthis.server.entity.item.EntityCanvas;
import com.pau101.paintthis.server.painting.Painting;

import net.minecraft.network.PacketBuffer;

public abstract class MessagePainting extends PTMessage {
	protected int canvasId;

	protected int x;

	protected int y;

	protected int width;

	protected byte[] data;

	public MessagePainting() {}

	public MessagePainting(EntityCanvas canvas, Painting.Change change) {
		this(canvas.getEntityId(), change.getX(), change.getY(), change.getWidth(), change.getData());
	}

	public MessagePainting(int canvasId, int x, int y, int width, byte[] data) {
		this.canvasId = canvasId;
		this.x = x;
		this.y = y;
		this.width = width;
		this.data = data;
	}

	@Override
	public void serialize(PacketBuffer buf) {
		buf.writeInt(canvasId);
		buf.writeByte(x);
		buf.writeByte(y);
		buf.writeByte(width);
		buf.writeShort(data.length);
		buf.writeBytes(data);
	}

	@Override
	public void deserialize(PacketBuffer buf) {
		canvasId = buf.readInt();
		x = buf.readByte();
		y = buf.readByte();
		width = buf.readByte();
		data = new byte[buf.readShort()];
		buf.readBytes(data);
	}
}
