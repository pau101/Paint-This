package com.pau101.paintthis.property;

import java.lang.invoke.MethodHandle;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

import com.google.common.base.Throwables;
import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.util.Util;

public class HorseShearability implements IExtendedEntityProperties {
	public static final String IDENTIFIER = PaintThis.MODID + ':' + "horse_shearability";

	private static final double ANNOYANCE_PER_SHEAR = 0.275;

	private static final double TOLERANCE_PER_TICK = 0.00025;

	private static final MethodHandle GET_LIVING_SOUND = Util.getHandle(EntityLiving.class, new String[] { "func_70639_aQ", "getLivingSound" });

	private static final MethodHandle GET_SOUND_VOLUME = Util.getHandle(EntityLivingBase.class, new String[] { "func_70599_aP", "getSoundVolume" });

	private static final MethodHandle GET_SOUND_PITCH = Util.getHandle(EntityLivingBase.class, new String[] { "func_70647_i", "getSoundPitch" });

	private double tolerance = 1;

	private long lastBotheredTime = -1;

	@Override
	public void init(Entity entity, World world) {}

	@Override
	public void saveNBTData(NBTTagCompound compound) {
		compound.setDouble("tolerance", tolerance);
		compound.setLong("lastBotheredTime", lastBotheredTime);
	}

	@Override
	public void loadNBTData(NBTTagCompound compound) {
		tolerance = compound.getDouble("tolerance");
		lastBotheredTime = compound.getLong("lastBotheredTime");
	}

	public boolean shear(EntityLiving horse, EntityPlayer player) {
		World world = player.worldObj;
		long time = world.getTotalWorldTime();
		if (lastBotheredTime > -1) {
			if (lastBotheredTime > time) {
				System.out.print("Great Scott, Marty!\n");
			} else {
				tolerance += (time - lastBotheredTime) * TOLERANCE_PER_TICK;
				if (tolerance > 1) {
					tolerance = 1;
				}
			}
		}
		lastBotheredTime = time;
		boolean successful = false;
		Random rng = world.rand;
		if (rng.nextDouble() < tolerance) {
			int amount = rng.nextInt(1 + (int) (tolerance + 0.5)) + 1;
			while (amount-- > 0) {
				EntityItem hair = horse.entityDropItem(new ItemStack(PaintThis.horsehair), 1);
				hair.motionX += (rng.nextDouble() - rng.nextDouble()) * 0.1;
				hair.motionY += rng.nextDouble() * 0.05;
				hair.motionZ += (rng.nextDouble() - rng.nextDouble()) * 0.1;
			}
			player.getHeldItem().damageItem(1, player);
			horse.playSound("mob.sheep.shear", 1, 1);
			successful = true;
		} else {
			if (horse instanceof EntityHorse) {
				((EntityHorse) horse).makeHorseRearWithSound();
			} else {
				try {
					horse.playSound((String) GET_LIVING_SOUND.invoke(horse), (float) GET_SOUND_VOLUME.invoke(horse), (float) GET_SOUND_PITCH.invoke(horse));
				} catch (Throwable e) {
					if (e instanceof Error) {
						Throwables.propagate(e);
					}
				}
			}
		}
		tolerance -= ANNOYANCE_PER_SHEAR;
		if (tolerance < 0) {
			tolerance = 0;
		}
		return successful;
	}
}
