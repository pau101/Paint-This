package com.pau101.paintthis.network;

import io.netty.buffer.ByteBuf;

import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.painting.Painting;

public abstract class MessagePainting implements SelfProcessingMessage {
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
	public void toBytes(ByteBuf buf) {
		buf.writeInt(canvasId);
		buf.writeByte(x);
		buf.writeByte(y);
		buf.writeByte(width);
		buf.writeShort(data.length);
		buf.writeBytes(data);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		canvasId = buf.readInt();
		x = buf.readByte();
		y = buf.readByte();
		width = buf.readByte();
		data = new byte[buf.readShort()];
		buf.readBytes(data);
	}
}
