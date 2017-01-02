package com.pau101.paintthis.network.client;

import com.pau101.paintthis.dye.Dye;
import com.pau101.paintthis.item.brush.ItemBrush;
import com.pau101.paintthis.network.SelfProcessingMessage;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageDyeSelect implements SelfProcessingMessage {
	private Dye dye;

	private EnumHand hand;

	public MessageDyeSelect() {}

	public MessageDyeSelect(Dye dye, EnumHand hand) {
		this.dye = dye;
		this.hand = hand;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(dye == null ? Dye.NO_DYE : dye.getByteValue());
		buf.writeBoolean(hand == EnumHand.MAIN_HAND);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		byte val = buf.readByte();
		dye = val == Dye.NO_DYE ? null : Dye.getDyeFromByte(val);
		hand = buf.readBoolean() ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
	}

	@Override
	public void process(MessageContext ctx) {
		EntityPlayer player = ctx.getServerHandler().playerEntity;
		ItemStack stack = player.getHeldItem(hand);
		if (stack != null && stack.getItem() instanceof ItemBrush) {
			stack.setItemDamage(dye == null ? 0 : dye.getBrushValue());
			player.inventoryContainer.detectAndSendChanges();	
		}
	}
}
