package com.pau101.paintthis.creativetab;

import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;

import com.pau101.paintthis.PaintThis;

public abstract class CreativeTabsPaintThis extends CreativeTabs {
	private String tabLabel;

	private boolean inForeground;

	public CreativeTabsPaintThis(String category) {
		super(PaintThis.ID + '.' + category);
	}

	private String getShownTabLabel() {
		if (tabLabel == null) {
			tabLabel = PaintThis.NAME + " - " + I18n.format(super.getTranslatedTabLabel());
		}
		return tabLabel;
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
