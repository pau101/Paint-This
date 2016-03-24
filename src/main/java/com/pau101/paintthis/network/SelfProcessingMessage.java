package com.pau101.paintthis.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public interface SelfProcessingMessage extends IMessage {
	public IMessage process(MessageContext ctx);
}
