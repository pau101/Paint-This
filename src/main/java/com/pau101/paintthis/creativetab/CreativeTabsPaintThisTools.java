package com.pau101.paintthis.creativetab;

import net.minecraft.item.Item;

import com.pau101.paintthis.PaintThis;

public class CreativeTabsPaintThisTools extends CreativeTabsPaintThis {
	public CreativeTabsPaintThisTools() {
		super("tools");
	}

	@Override
	public Item getTabIconItem() {
		return PaintThis.paintbrushSmall;
	}
}
