package com.pau101.paintthis.server.creativetab;

import com.pau101.paintthis.PaintThis;

import net.minecraft.item.Item;

public final class CreativeTabsPaintThisTools extends CreativeTabsPaintThis {
	public CreativeTabsPaintThisTools() {
		super("tools");
	}

	@Override
	public Item getTabIconItem() {
		return PaintThis.paintbrushSmall;
	}
}
