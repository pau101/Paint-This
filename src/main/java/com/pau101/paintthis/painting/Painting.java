package com.pau101.paintthis.painting;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.dye.Dye;
import com.pau101.paintthis.util.nbtassist.NBTAssist;
import com.pau101.paintthis.util.nbtassist.NBTProperty;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class Painting {
	public static final int PIXELS_PER_BLOCK = 16;

	public static final UUID UNASSIGNED_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	@NBTProperty
	private int width;

	@NBTProperty
	private int height;

	@NBTProperty
	private boolean isFramed;

	@NBTProperty
	private byte[] data = new byte[0];

	@NBTProperty
	private UUID uuid = UNASSIGNED_UUID;

	@NBTProperty
	private Signature signature;

	private Optional<Change> changedRegion = Optional.<Change> empty();

	private boolean isRecordingChange;

	public void setDimensions(int width, int height) {
		this.width = width;
		this.height = height;
		initData(width, height);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getPixelWidth() {
		return width * PIXELS_PER_BLOCK;
	}

	public int getPixelHeight() {
		return height * PIXELS_PER_BLOCK;
	}

	public void assignUUID(UUID uuid) {
		this.uuid = uuid;
	}

	public UUID getUUID() {
		return uuid;
	}

	public boolean hasAssignedUUID() {
		return !UNASSIGNED_UUID.equals(uuid);
	}

	public byte[] getData() {
		return data;
	}

	public void setIsFramed(boolean isFramed) {
		this.isFramed = isFramed;
	}

	public boolean isFramed() {
		return isFramed;
	}

	public void sign(EntityPlayer player, EnumHand hand, Vec3d hit) {
		signature = new Signature(player, Dye.getDyeFromDamage(player.getHeldItem(hand).getMetadata() - 1), Signature.Side.forHit(hit));
	}

	public void setSignature(Signature signature) {
		this.signature = signature;
	}

	public boolean isSigned() {
		return signature != null;
	}

	public Signature getSignature() {
		return signature;
	}

	public void beginRecordingChange() {
		isRecordingChange = true;
		changedRegion = Optional.<Change> empty();
	}

	public Optional<Change> endAndRemoveChange() {
		isRecordingChange = false;
		Optional<Change> change = changedRegion;
		changedRegion = Optional.<Change> empty();
		return change;
	}

	public void stroke(Vec3d from, Vec3d to, int size, Dye dye) {}

	public void dot(Vec3d pos, int size, Dye dye) {}

	public void set(int x, int y, byte dye) {
		if (contains(x, y)) {
			int i = x + y * getPixelWidth();
			int curr = data[i];
			if (curr != dye) {
				data[i] = dye;
				if (isRecordingChange) {
					if (changedRegion.isPresent()) {
						changedRegion.get().include(x, y);
					} else {
						changedRegion = Optional.of(new Change(x, y));
					}
				}
			}
		}
	}

	public void update(int x, int y, int width, byte[] data) {
		int height = data.length / width;
		for (int py = 0; py < height; py++) {
			for (int px = 0; px < width; px++) {
				this.data[x + px + (y + py) * getPixelWidth()] = data[px + py * width];
			}
		}
	}

	public boolean contains(int x, int y) {
		return x >= 0 && y >= 0 && x < getPixelWidth() && y < getPixelHeight();
	}

	public void writeToNBT(NBTTagCompound compound) {
		NBTAssist.write(this, compound);
	}

	public void readFromNBT(NBTTagCompound compound) {
		NBTAssist.read(this, compound);
		if (data == null || data.length != getDataLength()) {
			if (width < 1 || height < 1) {
				width = height = 1;
			}
			initData(width, height);
		}
		assignUUID(uuid);
	}

	protected void initData(int width, int height) {
		data = new byte[getDataLength()];
		Arrays.fill(data, Dye.NO_DYE);
	}

	private int getDataLength() {
		return getPixelWidth() * getPixelHeight();
	}

	public void writeToBuffer(ByteBuf buffer) {
		buffer.writeByte(width);
		buffer.writeByte(height);
		buffer.writeBoolean(isFramed);
		buffer.writeShort(data.length);
		for (int i = 0; i < data.length; i++) {
			buffer.writeByte(data[i]);
		}
		buffer.writeLong(uuid.getMostSignificantBits());
		buffer.writeLong(uuid.getLeastSignificantBits());
		buffer.writeBoolean(isSigned());
		if (isSigned()) {
			signature.writeToBuffer(new PacketBuffer(buffer));
		}
	}

	public void readFromBuffer(ByteBuf buffer) {
		int width = buffer.readByte();
		int height = buffer.readByte();
		if (width < 1 || height < 1) {
			width = height = 1;
		}
		setDimensions(width, height);
		setIsFramed(buffer.readBoolean());
		int length = buffer.readShort();
		for (int i = 0; i < length; i++) {
			byte val = buffer.readByte();
			if (i < data.length) {
				data[i] = val;
			}
		}
		assignUUID(new UUID(buffer.readLong(), buffer.readLong()));
		if (buffer.readBoolean()) {
			signature = new Signature();
			signature.readFromBuffer(new PacketBuffer(buffer));
		}
	}

	public static void createNewPainting(ItemStack stack, int width, int height) {
		NBTTagCompound compound = new NBTTagCompound();
		Painting painting = new Painting();
		painting.setDimensions(width, height);
		painting.writeToNBT(compound);
		stack.setTagInfo("painting", compound);
	}

	public static Painting getPainting(World world, ItemStack stack) {
		Painting painting = createActive(world);
		NBTTagCompound compound = stack.getSubCompound("painting", false);
		if (compound != null) {
			painting.readFromNBT(compound);
			painting.setIsFramed(stack.getMetadata() != 0);
		}
		return painting;
	}

	public static Painting getNonActivePainting(ItemStack stack) {
		Painting painting = new Painting();
		NBTTagCompound compound = stack.getSubCompound("painting", false);
		if (compound != null) {
			painting.readFromNBT(compound);
			painting.setIsFramed(stack.getMetadata() != 0);
		}
		return painting;
	}

	public ItemStack getAsItemStack(ItemStack parent) {
		ItemStack stack = new ItemStack(PaintThis.canvas, 1, isFramed ? 1 : 0);
		stack.setTagCompound(parent.getTagCompound());
		NBTTagCompound compound = new NBTTagCompound();
		writeToNBT(compound);
		stack.setTagInfo("painting", compound);
		return stack;
	}

	public class Change {
		private int minX = getPixelWidth();

		private int minY = getPixelHeight();

		private int maxX;

		private int maxY;

		private Change(int x, int y) {
			include(x, y);
		}

		private void include(int x, int y) {
			if (x < minX) {
				minX = x;
			}
			if (y < minY) {
				minY = y;
			}
			if (x > maxX) {
				maxX = x;
			}
			if (y > maxY) {
				maxY = y;
			}
		}

		public int getX() {
			return minX;
		}

		public int getY() {
			return minY;
		}

		public int getWidth() {
			return maxX - minX + 1;
		}

		public int getHeight() {
			return maxY - minY + 1;
		}

		public byte[] getData() {
			int w = getWidth();
			int h = getHeight();
			byte[] data = new byte[w * h];
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					data[x + y * w] = Painting.this.data[x + minX + (y + minY) * getPixelWidth()];
				}
			}
			return data;
		}
	}

	public static boolean isPainting(ItemStack stack) {
		if (stack == null || stack.getItem() != PaintThis.canvas) {
			return false;
		}
		NBTTagCompound compound = stack.getSubCompound("painting", false);
		if (compound == null) {
			return false;
		}
		return compound.hasKey("width", 3) && compound.hasKey("height", 3) && compound.hasKey("data", 7) && compound.hasKey("isFramed", 1);
	}

	public static int getPaintingWidth(ItemStack stack) {
		return getPaintingDimension(stack, "width");
	}

	public static int getPaintingHeight(ItemStack stack) {
		return getPaintingDimension(stack, "height");
	}

	private static int getPaintingDimension(ItemStack stack, String dim) {
		NBTTagCompound compound = stack.getSubCompound("painting", false);
		return compound == null ? 0 : compound.getInteger(dim);
	}

	public static Painting createActive(World world) {
		return PaintThis.proxy.createActivePainting(world);
	}
}
