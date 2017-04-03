package com.pau101.paintthis.server.asm;

import java.util.Iterator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;

public final class PaintThisClassTransformer implements IClassTransformer {
	private static final String CLIENT_PROXY = "com/pau101/paintthis/proxy/ClientProxy";

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		boolean obf;
		if ((obf = "ayl".equals(name)) || "net.minecraft.client.gui.inventory.GuiContainer".equals(name)) {
			return writeClass(transformGuiContainer(readClass(bytes), obf));
		}
		return bytes;
	}

	private ClassNode readClass(byte[] classBytes) {
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(classBytes);
		classReader.accept(classNode, 0);
		return classNode;
	}

	private byte[] writeClass(ClassNode classNode) {
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(classWriter);
		return classWriter.toByteArray();
	}

	/*
	 * We want the dyes on a palette in a crafting table to render in their
	 * positions that correspond to a slot that can be removed by a palette
	 * knife or are free to a new dye, therefore we must hook
	 * renderItemOverlayIntoGUI.
	 */
	private ClassNode transformGuiContainer(ClassNode clazz, boolean obf) {
		MethodNode method = findDrawSlotMethod(clazz, obf);
		String renderItemOverlayIntoGUI = obf ? "a" : "renderItemOverlayIntoGUI";
		String renderItemOverlayIntoGUIDesc = obf ? "(Lavn;Lzx;IILjava/lang/String;)V" : "(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V";
		String renderPaletteDesc = obf ? "(Layl;Lyg;)V" : "(Lnet/minecraft/client/gui/inventory/GuiContainer;Lnet/minecraft/inventory/Slot;)V";
		InsnList insns = method.instructions;
		for (AbstractInsnNode insn : iterate(insns)) {
			if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
				MethodInsnNode marker = (MethodInsnNode) insn;
				if (marker.desc.equals(renderItemOverlayIntoGUIDesc) && marker.name.equals(renderItemOverlayIntoGUI)) {
					InsnList hookInsns = new InsnList();
					hookInsns.add(new VarInsnNode(Opcodes.ALOAD, 0));
					hookInsns.add(new VarInsnNode(Opcodes.ALOAD, 1));
					hookInsns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CLIENT_PROXY, "renderPalette", renderPaletteDesc, false));
					insns.insert(insn, hookInsns);
					return clazz;
				}
			}
		}
		throw new RuntimeException("Failed to transform drawSlot!");
	}

	private MethodNode findDrawSlotMethod(ClassNode clazz, boolean obf) {
		String drawSlot = obf ? "a" : "drawSlot";
		String drawSlotDesc = obf ? "(Lyg;)V" : "(Lnet/minecraft/inventory/Slot;)V";
		return findMethod(clazz, drawSlot, drawSlotDesc);
	}

	private MethodNode findMethod(ClassNode clazz, String name, String desc) {
		for (MethodNode method : clazz.methods) {
			if (method.desc.equals(desc) && method.name.equals(name)) {
				return method;
			}
		}
		throw new RuntimeException("Failed to find " + name + desc);
	}

	private LabelNode getFirstLabel(InsnList insns) {
		LabelNode label = null;
		for (AbstractInsnNode insn : iterate(insns)) {
			if (insn.getType() == AbstractInsnNode.LABEL) {
				return (LabelNode) insn;
			}
		}
		throw new RuntimeException("No labels found!");
	}

	/*
	 * InsnList doesn't implement Iterable... So we wrap!
	 */
	private static Iterable<AbstractInsnNode> iterate(InsnList insns) {
		return () -> new Iterator<AbstractInsnNode>() {
			private int i;

			@Override
			public boolean hasNext() {
				return i < insns.size();
			}

			@Override
			public AbstractInsnNode next() {
				return insns.get(i++);
			}
		};
	}
}
