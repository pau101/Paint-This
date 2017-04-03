package com.pau101.paintthis.server.entity.item;

import java.util.Collections;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.server.painting.Painting;
import com.pau101.paintthis.server.sound.PTSounds;
import com.pau101.paintthis.server.util.Mth;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

public final class EntityEasel extends EntityLivingBase implements IEntityAdditionalSpawnData {
	private static final double RENDER_DISTANCE = 256;

	private static final int SUPPORTED_CANVAS_WIDTH = 3;

	private static final int SUPPORTED_CANVAS_HEIGHT = 2;

	private static final ItemStack[] INVENTORY = {};

	private long lastHitTime;

	public EntityEasel(World world) {
		super(world);
		setSize(0.8F, 3.2F);
	}

	public EntityEasel(World world, double x, double y, double z) {
		this(world);
		setPosition(x, y, z);
	}

	@Override
	public boolean canBePushed() {
		return false;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return PTSounds.EASEL_BREAK;
	}

	@Override
	protected SoundEvent getFallSound(int damage) {
		return PTSounds.EASEL_FALL;
	}

	@Override
	protected SoundEvent getHurtSound() {
		return PTSounds.EASEL_HIT;
	}

	@Override
	public void addVelocity(double x, double y, double z) {}

	@Override
	public AxisAlignedBB getCollisionBoundingBox() {
		return getEntityBoundingBox();
	}

	@Override
	public String getName() {
		return "item.easel.name";
	}

	@Override
	public boolean processInitialInteract(EntityPlayer player, ItemStack heldStack, EnumHand hand) {
		if (!(getControllingPassenger() instanceof EntityCanvas) && Painting.isPainting(heldStack) && canSupportPainting(heldStack)) {
			if (!worldObj.isRemote) {
				EntityCanvas canvas = new EntityCanvas(worldObj, heldStack.copy(), true);
				canvas.startRiding(this);
				positionCanvas(canvas);
				worldObj.spawnEntityInWorld(canvas);
				canvas.onCreated();
				heldStack.stackSize--;
			}
			return true;
		}
		return false;
	}

	private boolean canSupportPainting(ItemStack stack) {
		Painting painting = Painting.getNonActivePainting(stack);
		return painting.getHeight() <= SUPPORTED_CANVAS_HEIGHT && painting.getWidth() <= SUPPORTED_CANVAS_WIDTH;
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (!worldObj.isRemote && !isEntityInvulnerable(source)) {
			if (DamageSource.outOfWorld.equals(source)) {
				setDead();
			} else if (source.isExplosion()) {
				setDead();
			} else if (DamageSource.inFire.equals(source)) {
				if (isBurning()) {
					thisKillsTheEasel(0.15F);
				} else {
					setFire(5);
				}
			} else if (DamageSource.onFire.equals(source) && getHealth() > 0.5F) {
				thisKillsTheEasel(4);
			} else {
				boolean isArrow = "arrow".equals(source.getDamageType());
				boolean isPlayer = "player".equals(source.getDamageType());
				if (isPlayer || isArrow) {
					attackEntityFrom(source, isArrow, isPlayer);
				}
			}
		}
		return false;
	}

	@Override
	public Entity getControllingPassenger() {
		return getPassengers().isEmpty() ? null : getPassengers().get(0);
	}

	private void attackEntityFrom(DamageSource source, boolean isArrow, boolean isPlayer) {
		if (source.getSourceOfDamage() instanceof EntityArrow) {
			source.getSourceOfDamage().setDead();
		}
		if (!(source.getEntity() instanceof EntityPlayer) || ((EntityPlayer) source.getEntity()).capabilities.allowEdit) {
			Entity entity = getControllingPassenger();
			if (entity instanceof EntityCanvas) {
				if (!worldObj.isRemote) {
					entity.setDead();
					((EntityCanvas) entity).dropCanvas((EntityPlayer) (source.getSourceOfDamage() instanceof EntityPlayer ? source.getSourceOfDamage() : null));
				}
			} else {
				if (source.isCreativePlayer()) {
					animateBreaking();
					setDead();
				} else {
					long currentTime = worldObj.getTotalWorldTime();
					if (currentTime - lastHitTime > 5 && !isArrow) {
						lastHitTime = currentTime;
						playSound(getHurtSound(), 0.7F + rand.nextFloat() * 0.2F, 0.8F + rand.nextFloat() * 0.3F);
					} else {
						dropEasel();
						animateBreaking();
						setDead();
					}
				}
			}
		}
	}

	@Override
	public void updatePassenger(Entity passenger) {
		if (passenger instanceof EntityCanvas) {
			positionCanvas((EntityCanvas) passenger);
		} else {
			super.updatePassenger(passenger);	
		}
	}

	public void positionCanvas(EntityCanvas canvas) {
		float slope = (float) Math.atan(-8 * Math.PI / 180);
		float x = canvas.getHeight() / 2F;
		if (canvas.isFramed()) {
			x += 0.0625;
		}
		double elv = 0.9 + x;
		double dist = elv * slope + 0.511;
		double dx = Math.cos((rotationYaw + 90) * Mth.DEG_TO_RAD) * dist;
		double dz = Math.sin((rotationYaw + 90) * Mth.DEG_TO_RAD) * dist;
		canvas.setPosition(posX + dx, posY + elv, posZ + dz);
		canvas.rotationYaw = rotationYaw;
		canvas.rotationPitch = -8;
		canvas.place();
	}

	private void animateBreaking() {
		if (worldObj instanceof WorldServer) {
			((WorldServer) worldObj).spawnParticle(EnumParticleTypes.BLOCK_DUST, posX, posY + height / 1.5, posZ, 10, width / 4, height / 4, width / 4, 0.05, Block.getStateId(Blocks.PLANKS.getDefaultState()));
		}
		playSound(getDeathSound(), 0.7F + rand.nextFloat() * 0.2F, 0.8F + rand.nextFloat() * 0.3F);
	}

	@Override
	public boolean isInRangeToRenderDist(double distance) {
		return distance < RENDER_DISTANCE * RENDER_DISTANCE;
	}

	private void thisKillsTheEasel(float amount) {
		float health = getHealth();
		health -= amount;
		if (health <= 0.5F) {
			setDead();
		} else {
			setHealth(health);
		}
	}

	private void dropEasel() {
		Block.spawnAsEntity(worldObj, new BlockPos(this), new ItemStack(PaintThis.easel));
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {}

	@Override
	public void readSpawnData(ByteBuf additionalData) {
		// Do this so there is no yaw interpolation
		prevRotationYaw = rotationYawHead = rotationYaw;
	}

	@Override
	public Iterable<ItemStack> getArmorInventoryList() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public ItemStack getItemStackFromSlot(EntityEquipmentSlot slotIn) {
		return null;
	}

	@Override
	public void setItemStackToSlot(EntityEquipmentSlot slot, ItemStack stack) {
	}

	@Override
	public EnumHandSide getPrimaryHand() {
		return EnumHandSide.RIGHT;
	}
}
