package com.pau101.paintthis.entity.item;

import java.util.UUID;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.painting.Painting;
import com.pau101.paintthis.painting.Signature;
import com.pau101.paintthis.sound.PTSounds;
import com.pau101.paintthis.util.Mth;
import com.pau101.paintthis.util.Util;
import com.pau101.paintthis.util.nbtassist.NBTAssist;
import com.pau101.paintthis.util.nbtassist.NBTMutatorProperty;
import com.pau101.paintthis.util.nbtassist.NBTProperty;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

public class EntityCanvas extends Entity implements IEntityAdditionalSpawnData {
	private static final BlockPos NON_HANGING_POS = new BlockPos(0, -1, 0);

	private static final EnumFacing NON_HANGING_FACING = EnumFacing.UP;

	private static final double RENDER_DISTANCE = 256;

	private static final int HANGING_POS_VALIDATE_RATE = 40;

	@NBTMutatorProperty(name = "item", type = ItemStack.class)
	private static final DataParameter<Optional<ItemStack>> ITEM = EntityDataManager.createKey(EntityCanvas.class, DataSerializers.OPTIONAL_ITEM_STACK); 

	@NBTProperty
	private Painting painting;

	@NBTProperty
	private BlockPos hangingPosition;

	@NBTProperty
	private EnumFacing hangingDirection;

	private boolean isBeingPlaced;

	private int hangingPositionUpdateTick;

	public boolean hit = false;

	public EntityCanvas(World world) {
		this(world, new ItemStack(Blocks.STONE), false);
	}

	public EntityCanvas(World world, ItemStack paintingItem, boolean isBeingPlaced) {
		this(world, paintingItem, isBeingPlaced, NON_HANGING_POS, NON_HANGING_FACING);
	}

	public EntityCanvas(World world, ItemStack paintingItem, boolean isBeingPlaced, BlockPos hangingPosition, EnumFacing hangingDirection) {
		super(world);
		setItem(paintingItem);
		painting = Painting.getPainting(worldObj, paintingItem);
		this.isBeingPlaced = isBeingPlaced;
		this.hangingPosition = hangingPosition;
		this.hangingDirection = hangingDirection;
		setSize(0.5F, 0.5F);
		if (isOnBlock()) {
			updateHangingDirection(hangingDirection);
		}
	}

	@Override
	protected void entityInit() {
		getDataManager().register(ITEM, Optional.of(new ItemStack(Blocks.STONE)));
	}

	private void setItem(ItemStack item) {
		getDataManager().set(ITEM, Optional.of(item));
	}

	private ItemStack getItem() {
		return getDataManager().get(ITEM).orNull();
	}

	public Painting getPainting() {
		return painting;
	}

	public int getWidth() {
		return painting.getWidth();
	}

	public int getHeight() {
		return painting.getHeight();
	}

	public boolean isFramed() {
		return painting.isFramed();
	}

	public int getSize() {
		return getSizeIndex(getWidth(), getHeight());
	}

	public boolean isSigned() {
		return painting.isSigned();
	}

	public Signature getSignature() {
		return painting.getSignature();
	}

	public BlockPos getHangingPosition() {
		return hangingPosition;
	}

	public EnumFacing getHangingDirection() {
		return hangingDirection;
	}

	public boolean isBeingPlacedOnEasel() {
		return isBeingPlaced;
	}

	public boolean isOnEasel() {
		return getRidingEntity() instanceof EntityEasel;
	}

	public boolean isOnBlock() {
		return !NON_HANGING_POS.equals(hangingPosition);
	}

	@Override
	public boolean canBeCollidedWith() {
		return isOnBlock();
	}

	@Override
	public float getCollisionBorderSize() {
		return 0;
	}

	@Override
	protected boolean shouldSetPosAfterLoading() {
		return false;
	}

	private int getWidthPixels() {
		return painting.getWidth() * Painting.PIXELS_PER_BLOCK + (painting.isFramed() ? 2 : 0);
	}

	private int getHeightPixels() {
		return painting.getHeight() * Painting.PIXELS_PER_BLOCK + (painting.isFramed() ? 2 : 0);
	}

	public void sign(EntityPlayer player, EnumHand hand, Vec3d hit) {
		painting.sign(player, hand, hit);
	}

	public void setSignature(Signature signature) {
		painting.setSignature(signature);
	}

	public boolean isEditableBy(EntityPlayer player) {
		return !isSigned() || player.getUniqueID().equals(getSignature().getSigner());
	}

	@Override
	public void onUpdate() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		if (!worldObj.isRemote && !isDead) {
			boolean isKill = false;
			if (isOnBlock()) {
				if (hangingPositionUpdateTick++ == HANGING_POS_VALIDATE_RATE) {
					hangingPositionUpdateTick = 0;
					isKill = !onValidSurface();
				}
			} else {
				isKill = !isOnEasel();
			}
			if (isKill) {
				setDead();
				dropCanvas(null);
			}
		}
	}

	@Override
	public boolean startRiding(Entity entity, boolean force) {
		if (super.startRiding(entity, force)) {
			if (entity != null) {
				hangingPosition = NON_HANGING_POS;
				hangingDirection = NON_HANGING_FACING;
			}
			return true;
		}
		return false;
	}

	protected void updateHangingDirection(EnumFacing hangingDirection) {
		Preconditions.checkNotNull(hangingDirection, "The hanging direction must not be null!");
		Preconditions.checkArgument(hangingDirection.getAxis() != Axis.Y, "The hanging direction must be horizontal!");
		this.hangingDirection = hangingDirection;
		prevRotationYaw = rotationYaw = hangingDirection.getHorizontalIndex() * 90;
		updateHangingBoundingBox();
	}

	public boolean doALittleJigToValidSurface() {
		BlockPos origin = hangingPosition;
		int width = Math.max(1, getWidthPixels() / 16);
		int height = Math.max(1, getHeightPixels() / 16);
		EnumFacing side = hangingDirection.rotateYCCW();
		int maxX = (width + 1) / 2;
		int maxY = (height + 1) / 2;
		int smallestDist = width * width / 4 + height * height / 4 + 1;
		BlockPos newPos = origin;
		for (int x = -width / 2; x < maxX; x++) {
			for (int y = -height / 2; y < maxY; y++) {
				BlockPos pos = origin.offset(side, x).up(y);
				setHangingPosition(pos);
				int dist = x * x + y * y;
				if (onValidSurface() && dist < smallestDist) {
					newPos = pos;
					smallestDist = dist;
				}
			}
		}
		setHangingPosition(newPos);
		return !newPos.equals(origin);
	}

	public boolean onValidSurface() {
		if (isOnEasel()) {
			return true;
		}
		if (!worldObj.getCollisionBoxes(this, getEntityBoundingBox()).isEmpty()) {
			return false;
		}
		int width = Math.max(1, getWidthPixels() / 16);
		int height = Math.max(1, getHeightPixels() / 16);
		BlockPos back = hangingPosition.offset(hangingDirection.getOpposite());
		EnumFacing side = hangingDirection.rotateYCCW();
		for (int x = (-width + 1) / 2; x <= width / 2; x++) {
			for (int y = (-height + 1) / 2; y <= height / 2; y++) {
				BlockPos pos = back.offset(side, x).up(y);
				if (worldObj.isSideSolid(pos, hangingDirection)) {
					continue;
				}
				IBlockState state = worldObj.getBlockState(pos);
				if (!state.getMaterial().isSolid() && !BlockRedstoneDiode.isDiode(state)) {
					return false;
				}
			}
		}
		for (Entity entity : worldObj.getEntitiesWithinAABBExcludingEntity(this, getEntityBoundingBox())) {
			if (entity instanceof EntityHanging || entity instanceof EntityCanvas) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (isOnEasel()) {
			return super.attackEntityFrom(source, amount);
		}
		if (isEntityInvulnerable(source)) {
			return false;
		} else {
			if (!isDead && !worldObj.isRemote) {
				setDead();
				setBeenAttacked();
				if (source.getEntity() instanceof EntityPlayer) {
					dropCanvas((EntityPlayer) source.getEntity());
				}
			}
			return true;
		}
	}

	@Override
	public void moveEntity(double x, double y, double z) {
		breakIfMoveAttempted(x, y, z);
	}

	@Override
	public void addVelocity(double x, double y, double z) {
		breakIfMoveAttempted(x, y, z);
	}

	private void breakIfMoveAttempted(double x, double y, double z) {
		if (!worldObj.isRemote && isOnBlock() && !isDead && x * x + y * y + z * z > 0) {
			setDead();
			dropCanvas(null);
		}
	}

	@Override
	public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
		setPosition(x, y, z);
		setRotation(yaw, pitch);
	}

	@Override
	public void setPosition(double x, double y, double z) {
		posX = x;
		posY = y;
		posZ = z;
		if (painting != null) {
			if (isOnBlock()) {
				setHangingPosition(new BlockPos(x, y, z));
			} else if (isOnEasel()) {
				float size = getWidth() / 2F;
				float height = getHeight() / 2F;
				AxisAlignedBB bb = new AxisAlignedBB(-size, -height, -size, size, height, size);
				setEntityBoundingBox(bb.offset(posX, posY, posZ));
			}
		}
	}

	public void setHangingPosition(BlockPos hangingPosition) {
		if (!this.hangingPosition.equals(hangingPosition)) {
			this.hangingPosition = hangingPosition;
			updateHangingBoundingBox();
			isAirBorne = true;
		}
	}

	private void updateHangingBoundingBox() {
		double px = hangingPosition.getX() + 0.5;
		double py = hangingPosition.getY() + 0.5;
		double pz = hangingPosition.getZ() + 0.5;
		final double away = 0.46875;
		double centerX = getCenterOffset(getWidthPixels());
		double centerY = getCenterOffset(getHeightPixels());
		px = px - hangingDirection.getFrontOffsetX() * away;
		pz = pz - hangingDirection.getFrontOffsetZ() * away;
		py = py + centerY;
		EnumFacing enumfacing = hangingDirection.rotateYCCW();
		px = px + centerX * enumfacing.getFrontOffsetX();
		pz = pz + centerX * enumfacing.getFrontOffsetZ();
		posX = px;
		posY = py;
		posZ = pz;
		double dx = getWidthPixels();
		double dy = getHeightPixels();
		double dz = getWidthPixels();
		if (hangingDirection.getAxis() == EnumFacing.Axis.Z) {
			dz = 1;
		} else {
			dx = 1;
		}
		dx /= 32;
		dy /= 32;
		dz /= 32;
		setEntityBoundingBox(new AxisAlignedBB(px - dx, py - dy, pz - dz, px + dx, py + dy, pz + dz));
	}

	private double getCenterOffset(int length) {
		return (length + 8) / 16 % 2 == 0 ? 0.5 : 0;
	}

	public void place() {
		if (isBeingPlaced && worldObj.isRemote) {
			lastTickPosX = prevPosX = posX;
			lastTickPosY = prevPosY = posY;
			lastTickPosZ = prevPosZ = posZ;
			prevRotationPitch = rotationPitch;
			prevRotationYaw = rotationYaw;
			isBeingPlaced = false;
		}
	}

	public void dropCanvas(EntityPlayer player) {
		if (painting.getWidth() > 0 && painting.getHeight() > 0) {
			ItemStack stack = painting.getAsItemStack(getItem().copy());
			if (player == null || !(Util.containsItemStack(player.inventory, stack) && player.capabilities.isCreativeMode) && !player.inventory.addItemStackToInventory(stack)) {
				float fx = MathHelper.cos((rotationYaw + 90) * Mth.DEG_TO_RAD);
				float fz = MathHelper.sin((rotationYaw + 90) * Mth.DEG_TO_RAD);
				EnumFacing dir = EnumFacing.getFacingFromVector(fx, 0, fz);
				Block.spawnAsEntity(worldObj, new BlockPos(this).offset(dir), stack);
			}
			playSound(PTSounds.CANVAS_BREAK, 0.7F + rand.nextFloat() * 0.2F, 0.8F + rand.nextFloat() * 0.3F);
		}
	}

	@Override
	public boolean isInRangeToRenderDist(double distance) {
		return distance < RENDER_DISTANCE * RENDER_DISTANCE;
	}

	public void onCreated() {
		if (!painting.hasAssignedUUID()) {
			painting.assignUUID(UUID.randomUUID());
		}
		playSound(PTSounds.CANVAS_PLACE, 0.7F + rand.nextFloat() * 0.2F, 0.8F + rand.nextFloat() * 0.3F);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {
		NBTAssist.write(this, compound);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {
		NBTAssist.read(this, compound);
		if (painting == null) {
			painting = Painting.createActive(worldObj);
		}
		if (isOnBlock()) {
			updateHangingBoundingBox();
		}
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		getPainting().writeToBuffer(buffer);
		buffer.writeBoolean(isBeingPlaced);
		buffer.writeLong(hangingPosition.toLong());
		buffer.writeByte(hangingDirection.ordinal());
	}

	@Override
	public void readSpawnData(ByteBuf buffer) {
		getPainting().readFromBuffer(buffer);
		isBeingPlaced = buffer.readBoolean();
		hangingPosition = BlockPos.fromLong(buffer.readLong());
		EnumFacing hangingDirection = EnumFacing.values()[buffer.readByte()];
		if (isOnBlock()) {
			updateHangingDirection(hangingDirection);
		}
	}

	public static int getSizeIndex(int width, int height) {
		return width - 1 + (height - 1) * PaintThis.MAX_CANVAS_SIZE;
	}
}
