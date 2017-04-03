package com.pau101.paintthis.server.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import com.google.common.base.CaseFormat;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToFindMethodException;

public final class Util {
	private Util() {}

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	public static String getEnumLowerCamelName(Enum e) {
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, e.name());
	}

	public static boolean containsItemStack(IInventory inventory, ItemStack stack) {
		return indexOfItemStack(inventory, stack) > -1;
	}

	public static int indexOfItemStack(IInventory inventory, ItemStack stack) {
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			if (ItemStack.areItemStacksEqual(inventory.getStackInSlot(i), stack)) {
				return i;
			}
		}
		return -1;
	}

	public static <E> MethodHandle getHandle(Class<? super E> clazz, String[] names, Class<?>... parameterTypes) {
		Exception failed = null;
		for (String name : names) {
			try {
				Method m = clazz.getDeclaredMethod(name, parameterTypes);
				m.setAccessible(true);
				return LOOKUP.unreflect(m);
			} catch (Exception e) {
				failed = e;
			}
		}
		throw new UnableToFindMethodException(names, failed);
	}
}
