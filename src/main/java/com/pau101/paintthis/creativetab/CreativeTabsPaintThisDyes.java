package com.pau101.paintthis.creativetab;

import java.util.Collections;
import java.util.List;

import com.pau101.paintthis.dye.Dye;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

public class CreativeTabsPaintThisDyes extends CreativeTabsPaintThis {
	private static final int ITEM_SECONDS = 3;

	private ItemStack[] iconStacks;

	private int time;

	public CreativeTabsPaintThisDyes() {
		super("dyes");
		setBackgroundImageName("item_search.png");
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	public ItemStack getIconItemStack() {
		ItemStack[] iconStacks = getIconStacks();
		return iconStacks[time / (20 * ITEM_SECONDS) % iconStacks.length];
	}

	@Override
	public Item getTabIconItem() {
		return Items.DYE;
	}

	@Override
	public boolean hasSearchBar() {
		return true;
	}

	@Override
	public void displayAllRelevantItems(List<ItemStack> stacks) {
		Items.DYE.getSubItems(Items.DYE, this, stacks);
		// Reverse the vanilla dyes so they follow enum
		Collections.reverse(stacks);
		super.displayAllRelevantItems(stacks);
	}

	@SubscribeEvent
	public void tick(ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			time++;
		}
	}

	private ItemStack[] getIconStacks() {
		if (iconStacks == null) {
			Dye[] dyes = Dye.values();
			iconStacks = new ItemStack[dyes.length];
			for (int i = 0; i < dyes.length; i++) {
				iconStacks[i] = dyes[i].createItemStack();
			}
		}
		return iconStacks;
	}
}
