package com.pau101.paintthis.sound;

import com.pau101.paintthis.PaintThis;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class PTSounds {
	private PTSounds() {}

	public static final SoundEvent EASEL_BREAK = create("entity.easel.break");

	public static final SoundEvent EASEL_FALL = create("entity.easel.fall");

	public static final SoundEvent EASEL_HIT = create("entity.easel.hit");

	public static final SoundEvent EASEL_PLACE = create("entity.easel.place");

	public static final SoundEvent CANVAS_BREAK = create("entity.canvas.break");

	public static final SoundEvent CANVAS_PLACE = create("entity.canvas.place");

	public static void init() {} 

	private static final SoundEvent create(String name) {
		ResourceLocation id = new ResourceLocation(PaintThis.ID, name);
		SoundEvent sound = new SoundEvent(id).setRegistryName(name);
		GameRegistry.register(sound);
		return sound;
	}
}
