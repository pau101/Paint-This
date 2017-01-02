package com.pau101.paintthis.painting;

import java.util.UUID;

import com.pau101.paintthis.dye.Dye;
import com.pau101.paintthis.util.nbtassist.NBTProperty;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.Vec3d;

public class Signature {
	@NBTProperty
	private UUID signer = Painting.UNASSIGNED_UUID;

	@NBTProperty
	private String signerName = "missingno";

	@NBTProperty
	private Dye dye = Dye.INK_SAC;

	@NBTProperty
	private Side side = Side.RIGHT;

	public Signature() {}

	public Signature(EntityPlayer player, Dye color, Side side) {
		this(player.getUniqueID(), player.getName(), color, side);
	}

	public Signature(UUID signer, String signerName, Dye dye, Side side) {
		this.signer = signer;
		this.signerName = signerName;
		this.dye = dye;
		this.side = side;
	}

	public UUID getSigner() {
		return signer;
	}

	public String getSignerName() {
		return signerName;
	}

	public Dye getDye() {
		return dye;
	}

	public Side getSide() {
		return side;
	}

	public void writeToBuffer(PacketBuffer buffer) {
		buffer.writeUuid(signer);
		buffer.writeString(signerName);
		buffer.writeEnumValue(dye);
		buffer.writeEnumValue(side);
	}

	public void readFromBuffer(PacketBuffer buffer) {
		signer = buffer.readUuid();
		signerName = buffer.readStringFromBuffer(64);
		dye = buffer.readEnumValue(Dye.class);
		side = buffer.readEnumValue(Side.class);
	}

	public enum Side {
		LEFT,
		RIGHT;

		public static Side forHit(Vec3d hit) {
			if (hit.xCoord < 0.5F) {
				return LEFT;
			}
			return RIGHT;
		}
	}
}
