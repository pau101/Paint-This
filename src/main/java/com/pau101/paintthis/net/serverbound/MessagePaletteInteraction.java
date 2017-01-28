package com.pau101.paintthis.net.serverbound;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.dye.Dye;
import com.pau101.paintthis.item.ItemPalette;
import com.pau101.paintthis.item.brush.ItemBrush;
import com.pau101.paintthis.net.PTMessage;
import com.pau101.paintthis.util.OreDictUtil;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessagePaletteInteraction extends PTMessage {
	private PaletteAction action;

	private EnumHand hand;

	private int slot;

	public MessagePaletteInteraction() {}

	public MessagePaletteInteraction(PaletteAction action, EnumHand hand, int slot) {
		this.action = action;
		this.hand = hand;
		this.slot = slot;
	}

	@Override
	public void serialize(PacketBuffer buf) {
		buf.writeEnumValue(action);
		buf.writeEnumValue(hand);
		buf.writeByte(slot);
	}

	@Override
	public void deserialize(PacketBuffer buf) {
		action = buf.readEnumValue(PaletteAction.class);
		hand = buf.readEnumValue(EnumHand.class);
		slot = buf.readUnsignedByte();
	}

	@Override
	public void process(MessageContext ctx) {
		EntityPlayer player = ctx.getServerHandler().playerEntity;
		ItemStack stack = player.getHeldItem(hand);
		ItemStack palette = player.getHeldItem(hand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
		if (palette != null && palette.getItem() == PaintThis.palette && action.isItem(stack)) {
			action.perform(player, stack, palette, slot);
			if (stack.stackSize <= 0) {
				player.setHeldItem(hand, null);
			}
			player.inventoryContainer.detectAndSendChanges();
		}
	}

	public enum PaletteAction {
		PALETTE_KNIFE {
			@Override
			public boolean isItem(ItemStack stack) {
				return stack != null && stack.getItem() == PaintThis.paletteKnife;
			}

			@Override
			public void perform(EntityPlayer player, ItemStack stack, ItemStack palette, int slot) {
				NBTTagCompound compound = palette.getTagCompound();
				if (compound == null) {
					return;
				}
				byte[] dyes = compound.getByteArray("dyes");
				if (dyes.length != ItemPalette.DYE_COUNT) {
					return;
				}
				byte val = dyes[slot];
				if (val != Dye.NO_DYE) {
					dyes[slot] = Dye.NO_DYE;
					if (!player.capabilities.isCreativeMode) {
						stack.attemptDamageItem(1, player.getRNG());
					}
					player.inventory.addItemStackToInventory(Dye.getDyeFromByte(val).createItemStack());
					ItemPalette.removeDyesIfNone(palette);
				}
			}
		},
		DYE {
			@Override
			public boolean isItem(ItemStack stack) {
				return OreDictUtil.isDye(stack);
			}

			@Override
			public void perform(EntityPlayer player, ItemStack stack, ItemStack palette, int slot) {
				NBTTagCompound compound = palette.getTagCompound();
				byte[] dyes;
				if (compound == null) {
					compound = new NBTTagCompound();
					palette.setTagCompound(compound);
					dyes = new byte[ItemPalette.DYE_COUNT];
					compound.setTag("dyes", new NBTTagByteArray(dyes));
				} else {
					dyes = compound.getByteArray("dyes");
					if (dyes.length != ItemPalette.DYE_COUNT) {
						return;
					}	
				}
				byte val = dyes[slot];
				if (val != Dye.NO_DYE) {
					player.inventory.addItemStackToInventory(Dye.getDyeFromByte(val).createItemStack());
				}
				dyes[slot] = Dye.getDyeFromDyeItemStack(stack).getByteValue();
				if (!player.capabilities.isCreativeMode) {
					stack.stackSize--;
				}
			}
		},
		BRUSH {
			@Override
			public boolean isItem(ItemStack stack) {
				return stack != null && stack.getItem() instanceof ItemBrush;
			}

			@Override
			public void perform(EntityPlayer player, ItemStack stack, ItemStack palette, int slot) {
				NBTTagCompound compound = palette.getTagCompound();
				if (compound == null) {
					return;
				}
				byte[] dyes = compound.getByteArray("dyes");
				if (dyes.length != ItemPalette.DYE_COUNT) {
					return;
				}
				byte val = dyes[slot];
				if (val != Dye.NO_DYE) {
					Dye dye = Dye.getDyeFromByte(val);
					stack.setItemDamage(dye.getBrushValue());
				}
			}
		};

		public abstract boolean isItem(ItemStack stack);

		public abstract void perform(EntityPlayer player, ItemStack stack, ItemStack palette, int slot);
	}
}
