package com.pau101.paintthis.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.ForgeHooksClient;

public final class ItemRendererPatch extends ItemRenderer {
	private ItemStack prevItemStackMainHand;

	private ItemStack prevItemStackOffHand;

	public ItemRendererPatch(Minecraft mc) {
		super(mc);
	}

	@Override
	public void updateEquippedItem() {
		prevEquippedProgressMainHand = equippedProgressMainHand;
		prevEquippedProgressOffHand = equippedProgressOffHand;
		EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
		ItemStack main = player.getHeldItemMainhand();
		ItemStack off = player.getHeldItemOffhand();
		boolean immediateMain = false, immediateOff = false;
		if (player.isRowingBoat()) {
			equippedProgressMainHand = MathHelper.clamp_float(equippedProgressMainHand - 0.4F, 0, 1);
			equippedProgressOffHand = MathHelper.clamp_float(equippedProgressOffHand - 0.4F, 0, 1);
		} else {
			if ((prevItemStackMainHand != main || itemStackMainHand == main && equippedProgressMainHand == 1) && !ForgeHooksClient.shouldCauseReequipAnimation(itemStackMainHand, main, player.inventory.currentItem)) {
				immediateMain = itemStackMainHand != main;
			} else if (ItemStack.areItemsEqual(itemStackMainHand, main)) {
				float strength = player.getCooledAttackStrength(1);
				equippedProgressMainHand += MathHelper.clamp_float(strength * strength * strength - equippedProgressMainHand, -0.4F, 0.4F);
			} else {
				equippedProgressMainHand += MathHelper.clamp_float(0 - equippedProgressMainHand, -0.4F, 0.4F);
			}
			if ((prevItemStackOffHand != off || itemStackOffHand == off && equippedProgressOffHand == 1) && !ForgeHooksClient.shouldCauseReequipAnimation(itemStackOffHand, off, -1)) {
				immediateOff = itemStackOffHand != off;
			} else if (ItemStack.areItemsEqual(itemStackOffHand, off)) {
				equippedProgressOffHand += MathHelper.clamp_float(1 - equippedProgressOffHand, -0.4F, 0.4F);
			} else {
				equippedProgressOffHand += MathHelper.clamp_float(0 - equippedProgressOffHand, -0.4F, 0.4F);
			}
		}
		prevItemStackMainHand = itemStackMainHand;
		prevItemStackOffHand = itemStackOffHand;
		if (immediateMain || equippedProgressMainHand < 0.1F) {
			itemStackMainHand = main;
			if (!immediateMain) {
				prevEquippedProgressMainHand = 0;	
			}
		}
		if (immediateOff || equippedProgressOffHand < 0.1F) {
			itemStackOffHand = off;
			if (!immediateOff) {
				prevEquippedProgressOffHand = 0;	
			}
		}
	}

	@Override
	public void resetEquippedProgress(EnumHand hand) {
		ItemStack stack = Minecraft.getMinecraft().thePlayer.getHeldItem(hand);
		if (ForgeHooksClient.shouldCauseReequipAnimation(stack, stack, -1)) {
			if (hand == EnumHand.MAIN_HAND) {
				equippedProgressMainHand = 0;
			} else {
				equippedProgressOffHand = 0;
			}
		}
	}
}
