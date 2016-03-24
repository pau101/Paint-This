package com.pau101.paintthis.asm;

import java.util.Iterator;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class PaintThisClassTransformer implements IClassTransformer {
	private static final String CLIENT_PROXY = "com/pau101/paintthis/proxy/ClientProxy";

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		boolean obf;
		if ((obf = "ayl".equals(name)) || "net.minecraft.client.gui.inventory.GuiContainer".equals(name)) {
			return writeClass(transformGuiContainer(readClass(bytes), obf));
		} else if ((obf = "bfd".equals(name)) || "net.minecraft.client.renderer.WorldRenderer".equals(name)) {
			return writeClass(transformWorldRenderer(readClass(bytes), obf));
		} else if ((obf = "axu".equals(name)) || "net.minecraft.client.gui.GuiScreen".equals(name)) {
			return writeClass(transformGuiScreen(readClass(bytes), obf));
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

	/*
	 * Allow us to alter the color that is being written to the vertex buffer
	 * so we can lower the opacity for the dyes rendered around a palette in
	 * a crafting table.
	 */
	private ClassNode transformWorldRenderer(ClassNode clazz, boolean obf) {
		MethodNode method = findPutColor4Method(clazz, obf);
		InsnList insns = method.instructions;
		InsnList hookInsns = new InsnList();
		hookInsns.add(new VarInsnNode(Opcodes.ILOAD, 1));
		hookInsns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CLIENT_PROXY, "tweakPutColor4", "(I)I", false));
		hookInsns.add(new VarInsnNode(Opcodes.ISTORE, 1));
		insns.insertBefore(insns.get(0), hookInsns);
		return clazz;
	}

	private MethodNode findPutColor4Method(ClassNode clazz, boolean obf) {
		String putColor4 = obf ? "a" : "putColor4";
		String putColor4Desc = "(I)V";
		return findMethod(clazz, putColor4, putColor4Desc);
	}

	/*
	 * We need to be able to render our own recipes tooltip for the dyes so we
	 * need to hook this the renderToolTip method.
	 */
	private ClassNode transformGuiScreen(ClassNode clazz, boolean obf) {
		String desc = obf ? "(Laxu;Lzx;II)Z" : "(Lnet/minecraft/client/gui/GuiScreen;Lnet/minecraft/item/ItemStack;II)Z";
		MethodNode method = findRenderToolTipMethod(clazz, obf);
		InsnList insns = method.instructions;
		InsnList hookInsn = new InsnList();
		hookInsn.add(new VarInsnNode(Opcodes.ALOAD, 0));
		hookInsn.add(new VarInsnNode(Opcodes.ALOAD, 1));
		hookInsn.add(new VarInsnNode(Opcodes.ILOAD, 2));
		hookInsn.add(new VarInsnNode(Opcodes.ILOAD, 3));
		hookInsn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CLIENT_PROXY, "renderToolTip", desc, false));
		hookInsn.add(new JumpInsnNode(Opcodes.IFEQ, getFirstLabel(insns)));
		hookInsn.add(new InsnNode(Opcodes.RETURN));
		insns.insertBefore(insns.get(0), hookInsn);
		return clazz;
	}

	private MethodNode findRenderToolTipMethod(ClassNode clazz, boolean obf) {
		String renderTooltip = obf ? "a" : "renderToolTip";
		String renderTooltipDesc = obf ? "(Lzx;II)V" : "(Lnet/minecraft/item/ItemStack;II)V";
		return findMethod(clazz, renderTooltip, renderTooltipDesc);
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
