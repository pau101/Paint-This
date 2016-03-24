package com.pau101.paintthis.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.entity.item.EntityEasel;

public class ItemEasel extends Item {
	public ItemEasel() {
		setUnlocalizedName("easel");
		setMaxStackSize(16);
	}

	@Override
	public boolean onItemUse(ItemStack item, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (side == EnumFacing.DOWN) {
			return false;
		} else {
			boolean isReplaceable = world.getBlockState(pos).getBlock().isReplaceable(world, pos);
			BlockPos placePos = isReplaceable ? pos : pos.offset(side);
			if (!player.canPlayerEdit(placePos, side, item)) {
				return false;
			} else {
				BlockPos abovePos = placePos.up();
				BlockPos topPos = abovePos.up();
				if (cantPlace(world, placePos) || cantPlace(world, abovePos) || cantPlace(world, topPos)) {
					return false;
				} else {
					return attemptSpawnEasel(item, player, world, placePos, abovePos, topPos);
				}
			}
		}
	}

	private boolean attemptSpawnEasel(ItemStack item, EntityPlayer player, World world, BlockPos placePos, BlockPos abovePos, BlockPos topPos) {
		double x = placePos.getX();
		double y = placePos.getY();
		double z = placePos.getZ();
		List entities = world.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB.fromBounds(x, y, z, x + 1, y + 3, z + 1));
		if (entities.size() > 0) {
			return false;
		} else {
			if (!world.isRemote) {
				world.setBlockToAir(placePos);
				world.setBlockToAir(abovePos);
				world.setBlockToAir(topPos);
				float yaw = MathHelper.floor_float((MathHelper.wrapAngleTo180_float(player.rotationYaw - 180) + 22.5F) / 45) * 45;
				spawnEasel(item, world, x, y, z, yaw);
			}
			item.stackSize--;
			return true;
		}
	}

	private void spawnEasel(ItemStack item, World world, double x, double y, double z, float yaw) {
		EntityEasel easel = new EntityEasel(world, x + 0.5, y, z + 0.5);
		easel.setLocationAndAngles(x + 0.5, y, z + 0.5, yaw, 0);
		NBTTagCompound compound = item.getTagCompound();
		if (compound != null && compound.hasKey("EntityTag", 10)) {
			NBTTagCompound currentCompound = new NBTTagCompound();
			easel.writeToNBTOptional(currentCompound);
			currentCompound.merge(compound.getCompoundTag("EntityTag"));
			easel.readFromNBT(currentCompound);
		}
		world.spawnEntityInWorld(easel);
		world.playSoundEffect(x, y, z, PaintThis.MODID + ":entity.easel.place", 0.7F + world.rand.nextFloat() * 0.2F, 0.8F + world.rand.nextFloat() * 0.3F);
	}

	private boolean cantPlace(World world, BlockPos pos) {
		return !world.isAirBlock(pos) && !world.getBlockState(pos).getBlock().isReplaceable(world, pos);
	}
}
