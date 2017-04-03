package com.pau101.paintthis.server.item;

import java.util.List;

import com.pau101.paintthis.server.entity.item.EntityEasel;
import com.pau101.paintthis.server.sound.PTSounds;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public final class ItemEasel extends Item {
	public ItemEasel() {
		setUnlocalizedName("easel");
		setMaxStackSize(16);
	}

	@Override
	public EnumActionResult onItemUse(ItemStack item, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (side == EnumFacing.DOWN) {
			return EnumActionResult.FAIL;
		}
		boolean isReplaceable = world.getBlockState(pos).getBlock().isReplaceable(world, pos);
		BlockPos placePos = isReplaceable ? pos : pos.offset(side);
		if (!player.canPlayerEdit(placePos, side, item)) {
			return EnumActionResult.FAIL;
		}
		BlockPos abovePos = placePos.up();
		BlockPos topPos = abovePos.up();
		if (cantPlace(world, placePos) || cantPlace(world, abovePos) || cantPlace(world, topPos)) {
			return EnumActionResult.FAIL;
		}
		return attemptSpawnEasel(item, player, world, placePos, abovePos, topPos);
	}

	private EnumActionResult attemptSpawnEasel(ItemStack item, EntityPlayer player, World world, BlockPos placePos, BlockPos abovePos, BlockPos topPos) {
		double x = placePos.getX();
		double y = placePos.getY();
		double z = placePos.getZ();
		List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(null, new AxisAlignedBB(x, y, z, x + 1, y + 3, z + 1));
		if (entities.size() > 0) {
			return EnumActionResult.FAIL;
		}
		if (!world.isRemote) {
			world.setBlockToAir(placePos);
			world.setBlockToAir(abovePos);
			world.setBlockToAir(topPos);
			float yaw = MathHelper.floor_float((MathHelper.wrapDegrees(player.rotationYaw - 180) + 22.5F) / 45) * 45;
			spawnEasel(item, world, x, y, z, yaw);
		}
		item.stackSize--;
		return EnumActionResult.SUCCESS;
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
		world.playSound(null, x, y, z, PTSounds.EASEL_PLACE, SoundCategory.BLOCKS, 0.7F + world.rand.nextFloat() * 0.2F, 0.8F + world.rand.nextFloat() * 0.3F);
	}

	private boolean cantPlace(World world, BlockPos pos) {
		return !world.isAirBlock(pos) && !world.getBlockState(pos).getBlock().isReplaceable(world, pos);
	}
}
