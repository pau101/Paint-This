package com.pau101.paintthis.server.creativetab;

import com.pau101.paintthis.PaintThis;

import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;

public abstract class CreativeTabsPaintThis extends CreativeTabs {
	private boolean inForeground;

	public CreativeTabsPaintThis(String category) {
		super(PaintThis.ID + '.' + category);
	}

	private String getShownTabLabel() {
		return PaintThis.NAME + " - " + I18n.format(super.getTranslatedTabLabel());
	}

	@Override
	public boolean drawInForegroundOfTab() {
		inForeground = true;
		return super.drawInForegroundOfTab();
	}

	@Override
	public String getTranslatedTabLabel() {
		String label = inForeground ? super.getTranslatedTabLabel() : getShownTabLabel();
		inForeground = false;
		return label;
	}
}
