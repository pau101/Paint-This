package com.pau101.paintthis.capability;

import java.util.Random;

import com.pau101.paintthis.PaintThis;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

public class HorseShearabilityDefault implements HorseShearability {
	private static final double ANNOYANCE_PER_SHEAR = 0.275;

	private static final double TOLERANCE_PER_TICK = 0.00025;

	private double tolerance = 1;

	private long lastBotheredTime = -1;

	@Override
	public boolean shear(EntityLiving horse, EntityPlayer player, EnumHand hand) {
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
			while (amount --> 0) {
				EntityItem hair = horse.entityDropItem(new ItemStack(PaintThis.horsehair), 1);
				hair.motionX += (rng.nextDouble() - rng.nextDouble()) * 0.1;
				hair.motionY += rng.nextDouble() * 0.05;
				hair.motionZ += (rng.nextDouble() - rng.nextDouble()) * 0.1;
			}
			player.getHeldItem(hand).damageItem(1, player);
			horse.playSound(SoundEvents.ENTITY_SHEEP_SHEAR, 1, 1);
			successful = true;
		} else {
			if (horse instanceof EntityHorse) {
				((EntityHorse) horse).makeHorseRearWithSound();
			} else {
				horse.playLivingSound();
			}
		}
		tolerance -= ANNOYANCE_PER_SHEAR;
		if (tolerance < 0) {
			tolerance = 0;
		}
		return successful;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityHandler.HORSER_SHEARABILITY_CAP;
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return hasCapability(capability, facing) ? CapabilityHandler.HORSER_SHEARABILITY_CAP.cast(this) : null;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setDouble("tolerance", tolerance);
		compound.setLong("lastBotheredTime", lastBotheredTime);
		return compound;
	}

	@Override
	public void deserializeNBT(NBTTagCompound compound) {
		tolerance = compound.getDouble("tolerance");
		lastBotheredTime = compound.getLong("lastBotheredTime");
	}
}
