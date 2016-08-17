package com.pau101.paintthis.util.nbtassist;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.BlockPos;

public final class NBTAssist {
	private NBTAssist() {}

	private static final String VERB_SET = "set";

	private static final String VERB_IS = "is";

	private static final String VERB_GET = "get";

	public static void write(Object object, NBTTagCompound compound) {
		Class<?> clazz = object.getClass();
		do {
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				if (field.isAnnotationPresent(NBTProperty.class)) {
					field.setAccessible(true);
					String name = field.getAnnotation(NBTProperty.class).name();
					if (name.isEmpty()) {
						name = field.getName();
					}
					NBTBase tag = writeFieldToNBT(object, field);
					if (tag != null) {
						compound.setTag(name, tag);
					}
				} else if (field.isAnnotationPresent(NBTMutatorProperty.class)) {
					NBTMutatorProperty mutatorProperty = field.getAnnotation(NBTMutatorProperty.class);
					String name = mutatorProperty.name();
					Class<?> type = mutatorProperty.type();
					Method getter = getGetter(clazz, name, type, mutatorProperty.getter());
					NBTBase tag = writeGetterValueToNBT(object, getter);
					if (tag != null) {
						compound.setTag(name, tag);
					}
				}
			}
		} while ((clazz = clazz.getSuperclass()) != null);
	}

	private static NBTBase writeFieldToNBT(Object object, Field field) {
		Object value;
		try {
			value = field.get(object);
		} catch (Exception e) {
			throw new NBTAssistAccessError(e);
		}
		return writeToNBT(field.getType(), value);
	}

	private static NBTBase writeGetterValueToNBT(Object object, Method getter) {
		Object value;
		try {
			value = getter.invoke(object);
		} catch (Exception e) {
			throw new NBTAssistAccessError(e);
		}
		return writeToNBT(getter.getReturnType(), value);
	}

	private static NBTBase writeToNBT(Class<?> type, Object value) {
		if (value == null) {
			return null;
		}
		if (type == boolean.class || type == Boolean.class) {
			return new NBTTagByte((byte) ((Boolean) value ? 1 : 0));
		} else if (type == byte.class || type == Byte.class) {
			return new NBTTagByte((Byte) value);
		} else if (type == char.class || type == Character.class) {
			return new NBTTagShort((short) ((Character) value).charValue());
		} else if (type == short.class || type == Short.class) {
			return new NBTTagShort((Short) value);
		} else if (type == int.class || type == Integer.class) {
			return new NBTTagInt((Integer) value);
		} else if (type == float.class || type == Float.class) {
			return new NBTTagFloat((Float) value);
		} else if (type == long.class || type == Long.class) {
			return new NBTTagLong((Long) value);
		} else if (type == double.class || type == Double.class) {
			return new NBTTagDouble((Double) value);
		} else if (type == String.class) {
			return new NBTTagString((String) value);
		} else if (type == boolean[].class) {
			boolean[] valueBoolArray = (boolean[]) value;
			byte[] valueByteArray = new byte[(valueBoolArray.length + 7) / 8];
			for (int i = 0; i < valueBoolArray.length; i++) {
				valueByteArray[i / 8] |= (byte) ((valueBoolArray[i] ? 1 : 0) << i % 8);
			}
			NBTTagCompound byteArrayCompound = new NBTTagCompound();
			byteArrayCompound.setInteger("length", valueBoolArray.length);
			byteArrayCompound.setByteArray("array", valueByteArray);
			return byteArrayCompound;
		} else if (type == Boolean[].class) {
			Boolean[] valueBoolArray = (Boolean[]) value;
			byte[] valueByteArray = new byte[(valueBoolArray.length + 7) / 8];
			for (int i = 0; i < valueBoolArray.length; i++) {
				valueByteArray[i / 8] |= (byte) ((valueBoolArray[i] ? 1 : 0) << i % 8);
			}
			NBTTagCompound byteArrayCompound = new NBTTagCompound();
			byteArrayCompound.setInteger("length", valueBoolArray.length);
			byteArrayCompound.setByteArray("array", valueByteArray);
			return byteArrayCompound;
		} else if (type == byte[].class) {
			return new NBTTagByteArray((byte[]) value);
		} else if (type == Byte[].class) {
			Byte[] valueByteArray = (Byte[]) value;
			byte[] valuePrimByteArray = new byte[valueByteArray.length];
			for (int i = 0; i < valueByteArray.length; i++) {
				valuePrimByteArray[i] = valueByteArray[i];
			}
			return new NBTTagByteArray(valuePrimByteArray);
		} else if (type == char[].class) {
			char[] valueCharArray = (char[]) value;
			NBTTagList list = new NBTTagList();
			for (int i = 0; i < valueCharArray.length; i++) {
				list.appendTag(new NBTTagShort((short) valueCharArray[i]));
			}
			return list;
		} else if (type == Character[].class) {
			Character[] valueCharArray = (Character[]) value;
			NBTTagList list = new NBTTagList();
			for (int i = 0; i < valueCharArray.length; i++) {
				list.appendTag(new NBTTagShort((short) valueCharArray[i].charValue()));
			}
			return list;
		} else if (type == short[].class) {
			short[] valueShortArray = (short[]) value;
			NBTTagList list = new NBTTagList();
			for (int i = 0; i < valueShortArray.length; i++) {
				list.appendTag(new NBTTagShort(valueShortArray[i]));
			}
			return list;
		} else if (type == Short[].class) {
			Short[] valueShortArray = (Short[]) value;
			NBTTagList list = new NBTTagList();
			for (int i = 0; i < valueShortArray.length; i++) {
				list.appendTag(new NBTTagShort(valueShortArray[i]));
			}
			return list;
		} else if (type == int[].class) {
			return new NBTTagIntArray((int[]) value);
		} else if (type == Integer[].class) {
			Integer[] valueIntArray = (Integer[]) value;
			int[] valuePrimIntArray = new int[valueIntArray.length];
			for (int i = 0; i < valueIntArray.length; i++) {
				valuePrimIntArray[i] = valueIntArray[i];
			}
			return new NBTTagIntArray(valuePrimIntArray);
		} else if (type == float[].class) {
			float[] valueFloatArray = (float[]) value;
			NBTTagList list = new NBTTagList();
			for (int i = 0; i < valueFloatArray.length; i++) {
				list.appendTag(new NBTTagFloat(valueFloatArray[i]));
			}
			return list;
		} else if (type == Float[].class) {
			Float[] valueFloatArray = (Float[]) value;
			NBTTagList list = new NBTTagList();
			for (int i = 0; i < valueFloatArray.length; i++) {
				list.appendTag(new NBTTagFloat(valueFloatArray[i]));
			}
			return list;
		} else if (type == long[].class) {
			long[] valueLongArray = (long[]) value;
			NBTTagList list = new NBTTagList();
			for (int i = 0; i < valueLongArray.length; i++) {
				list.appendTag(new NBTTagLong(valueLongArray[i]));
			}
			return list;
		} else if (type == Long[].class) {
			Long[] valueLongArray = (Long[]) value;
			NBTTagList list = new NBTTagList();
			for (int i = 0; i < valueLongArray.length; i++) {
				list.appendTag(new NBTTagLong(valueLongArray[i]));
			}
			return list;
		} else if (type == double[].class) {
			double[] valueDoubleArray = (double[]) value;
			NBTTagList list = new NBTTagList();
			for (int i = 0; i < valueDoubleArray.length; i++) {
				list.appendTag(new NBTTagDouble(valueDoubleArray[i]));
			}
			return list;
		} else if (type == Double[].class) {
			Double[] valueDoubleArray = (Double[]) value;
			NBTTagList list = new NBTTagList();
			for (int i = 0; i < valueDoubleArray.length; i++) {
				list.appendTag(new NBTTagDouble(valueDoubleArray[i]));
			}
			return list;
		} else if (type == String[].class) {
			String[] valueStringArray = (String[]) value;
			NBTTagList list = new NBTTagList();
			for (int i = 0; i < valueStringArray.length; i++) {
				list.appendTag(new NBTTagString(valueStringArray[i]));
			}
			return list;
		} else if (List.class.isAssignableFrom(type)) {
			List<?> valueList = (List<?>) value;
			NBTTagList list = new NBTTagList();
			for (Object valueElement : valueList) {
				list.appendTag(writeToNBT(valueElement.getClass(), valueElement));
			}
			return list;
		} else if (Map.class.isAssignableFrom(type)) {
			Map<?, ?> valueMap = (Map<?, ?>) value;
			NBTTagList entryList = new NBTTagList();
			for (Entry<?, ?> entry : valueMap.entrySet()) {
				NBTTagCompound entryCompound = new NBTTagCompound();
				Object entryKey = entry.getKey();
				Object entryValue = entry.getValue();
				entryCompound.setTag("key", writeToNBT(entryKey.getClass(), entryKey));
				entryCompound.setTag("value", writeToNBT(entryValue.getClass(), entryValue));
				entryList.appendTag(entryCompound);
			}
			return entryList;
		} else if (type == ItemStack.class) {
			NBTTagCompound itemStackCompound = new NBTTagCompound();
			((ItemStack) value).writeToNBT(itemStackCompound);
			return itemStackCompound;
		} else if (type == UUID.class) {
			NBTTagCompound uuidCompound = new NBTTagCompound();
			uuidCompound.setLong("most", ((UUID) value).getMostSignificantBits());
			uuidCompound.setLong("least", ((UUID) value).getLeastSignificantBits());
			return uuidCompound;
		} else if (type == BlockPos.class) {
			NBTTagCompound blockPosCompound = new NBTTagCompound();
			blockPosCompound.setInteger("x", ((BlockPos) value).getX());
			blockPosCompound.setInteger("y", ((BlockPos) value).getY());
			blockPosCompound.setInteger("z", ((BlockPos) value).getZ());
			return blockPosCompound;
		} else if (type.isEnum()) {
			return new NBTTagString(((Enum) value).name());
		} else if (type.isArray()) {
			Object[] valueArray = (Object[]) value;
			NBTTagList list = new NBTTagList();
			Class<?> arrayType = type.getComponentType();
			for (Object valueElement : valueArray) {
				list.appendTag(writeToNBT(arrayType, valueElement));
			}
			return list;
		} else {
			NBTTagCompound valueCompound = new NBTTagCompound();
			write(value, valueCompound);
			return valueCompound;
		}
	}

	public static void read(Object object, NBTTagCompound compound) {
		Class<?> clazz = object.getClass();
		do {
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				if (field.isAnnotationPresent(NBTProperty.class)) {
					field.setAccessible(true);
					String name = field.getAnnotation(NBTProperty.class).name();
					if (name.isEmpty()) {
						name = field.getName();
					}
					readFromNBTToField(object, field, name, compound);
				} else if (field.isAnnotationPresent(NBTMutatorProperty.class)) {
					NBTMutatorProperty mutatorProperty = field.getAnnotation(NBTMutatorProperty.class);
					String name = mutatorProperty.name();
					Class<?> type = mutatorProperty.type();
					Method setter = getSetter(clazz, name, type, mutatorProperty.setter());
					readFromNBTToSetter(object, setter, name, compound);
				}
			}
		} while ((clazz = clazz.getSuperclass()) != null);
	}

	private static void readFromNBTToField(Object object, Field field, String name, NBTTagCompound compound) {
		NBTBase valueNBT = compound.getTag(name);
		if (valueNBT == null) {
			return;
		}
		Object value = readFromNBT(field.getType(), field.getGenericType(), valueNBT);
		try {
			field.set(object, value);
		} catch (Exception e) {
			throw new NBTAssistAccessError(e);
		}
	}

	private static void readFromNBTToSetter(Object object, Method setter, String name, NBTTagCompound compound) {
		NBTBase valueNBT = compound.getTag(name);
		if (valueNBT == null) {
			return;
		}
		Object value = readFromNBT(setter.getParameters()[0].getType(), setter.getParameters()[0].getParameterizedType(), valueNBT);
		try {
			setter.invoke(object, value);
		} catch (Exception e) {
			throw new NBTAssistAccessError(e);
		}
	}

	private static Object readFromNBT(Class<?> type, Type parameterizedType, NBTBase tag) {
		if ((type == boolean.class || type == Boolean.class) && tag instanceof NBTTagByte) {
			return ((NBTTagByte) tag).getByte() != 0;
		} else if ((type == byte.class || type == Byte.class) && tag instanceof NBTTagByte) {
			return ((NBTTagByte) tag).getByte();
		} else if ((type == char.class || type == Character.class) && tag instanceof NBTTagShort) {
			return (char) ((NBTTagShort) tag).getShort();
		} else if ((type == short.class || type == Short.class) && tag instanceof NBTTagShort) {
			return ((NBTTagShort) tag).getShort();
		} else if ((type == int.class || type == Integer.class) && tag instanceof NBTTagInt) {
			return ((NBTTagInt) tag).getInt();
		} else if ((type == float.class || type == Float.class) && tag instanceof NBTTagFloat) {
			return ((NBTTagFloat) tag).getFloat();
		} else if ((type == long.class || type == Long.class) && tag instanceof NBTTagLong) {
			return ((NBTTagLong) tag).getLong();
		} else if ((type == double.class || type == Double.class) && tag instanceof NBTTagDouble) {
			return ((NBTTagDouble) tag).getDouble();
		} else if (type == String.class && tag instanceof NBTTagString) {
			return ((NBTTagString) tag).getString();
		} else if (type == boolean[].class && tag instanceof NBTTagCompound) {
			NBTTagCompound list = (NBTTagCompound) tag;
			boolean[] boolArray = new boolean[list.getInteger("length")];
			byte[] boolList = list.getByteArray("array");
			for (int i = 0; i < boolArray.length; i++) {
				boolArray[i] = (boolList[i / 8] >>> i % 8 & 1) != 0;
			}
			return boolArray;
		} else if (type == Boolean[].class && tag instanceof NBTTagCompound) {
			NBTTagCompound list = (NBTTagCompound) tag;
			Boolean[] boolArray = new Boolean[list.getInteger("length")];
			byte[] boolList = list.getByteArray("array");
			for (int i = 0; i < boolArray.length; i++) {
				boolArray[i] = (boolList[i / 8] >>> i % 8 & 1) != 0;
			}
			return boolArray;
		} else if (type == byte[].class && tag instanceof NBTTagByteArray) {
			return ((NBTTagByteArray) tag).getByteArray();
		} else if (type == Byte[].class && tag instanceof NBTTagByteArray) {
			byte[] bytePrimArray = ((NBTTagByteArray) tag).getByteArray();
			Byte[] byteArray = new Byte[bytePrimArray.length];
			for (int i = 0; i < byteArray.length; i++) {
				byteArray[i] = bytePrimArray[i];
			}
			return byteArray;
		} else if (type == char[].class && tag instanceof NBTTagList) {
			NBTTagList charList = (NBTTagList) tag;
			char[] charArray = new char[charList.tagCount()];
			for (int i = 0; i < charArray.length; i++) {
				charArray[i] = (char) ((NBTTagShort) charList.get(i)).getShort();
			}
			return charArray;
		} else if (type == Character[].class && tag instanceof NBTTagList) {
			NBTTagList charList = (NBTTagList) tag;
			Character[] charArray = new Character[charList.tagCount()];
			for (int i = 0; i < charArray.length; i++) {
				charArray[i] = (char) ((NBTTagShort) charList.get(i)).getShort();
			}
			return charArray;
		} else if (type == short[].class && tag instanceof NBTTagList) {
			NBTTagList charList = (NBTTagList) tag;
			short[] charArray = new short[charList.tagCount()];
			for (int i = 0; i < charArray.length; i++) {
				charArray[i] = ((NBTTagShort) charList.get(i)).getShort();
			}
			return charArray;
		} else if (type == Short[].class && tag instanceof NBTTagList) {
			NBTTagList charList = (NBTTagList) tag;
			Short[] charArray = new Short[charList.tagCount()];
			for (int i = 0; i < charArray.length; i++) {
				charArray[i] = ((NBTTagShort) charList.get(i)).getShort();
			}
			return charArray;
		} else if (type == int[].class && tag instanceof NBTTagIntArray) {
			return ((NBTTagIntArray) tag).getIntArray();
		} else if (type == Integer[].class && tag instanceof NBTTagIntArray) {
			NBTTagIntArray intList = (NBTTagIntArray) tag;
			int[] intPrimArray = intList.getIntArray();
			Integer[] intArray = new Integer[intPrimArray.length];
			for (int i = 0; i < intArray.length; i++) {
				intArray[i] = intPrimArray[i];
			}
			return intArray;
		} else if (type == float[].class && tag instanceof NBTTagList) {
			NBTTagList floatList = (NBTTagList) tag;
			float[] floatArray = new float[floatList.tagCount()];
			for (int i = 0; i < floatArray.length; i++) {
				floatArray[i] = ((NBTTagFloat) floatList.get(i)).getFloat();
			}
			return floatArray;
		} else if (type == Float[].class && tag instanceof NBTTagList) {
			NBTTagList floatList = (NBTTagList) tag;
			Float[] floatArray = new Float[floatList.tagCount()];
			for (int i = 0; i < floatArray.length; i++) {
				floatArray[i] = ((NBTTagFloat) floatList.get(i)).getFloat();
			}
			return floatArray;
		} else if (type == long[].class && tag instanceof NBTTagList) {
			NBTTagList longList = (NBTTagList) tag;
			long[] longArray = new long[longList.tagCount()];
			for (int i = 0; i < longArray.length; i++) {
				longArray[i] = ((NBTTagLong) longList.get(i)).getLong();
			}
			return longArray;
		} else if (type == Long[].class && tag instanceof NBTTagList) {
			NBTTagList longList = (NBTTagList) tag;
			Long[] longArray = new Long[longList.tagCount()];
			for (int i = 0; i < longArray.length; i++) {
				longArray[i] = ((NBTTagLong) longList.get(i)).getLong();
			}
			return longArray;
		} else if (type == double[].class && tag instanceof NBTTagList) {
			NBTTagList doubleList = (NBTTagList) tag;
			double[] doubleArray = new double[doubleList.tagCount()];
			for (int i = 0; i < doubleArray.length; i++) {
				doubleArray[i] = ((NBTTagDouble) doubleList.get(i)).getDouble();
			}
			return doubleArray;
		} else if (type == Double[].class && tag instanceof NBTTagList) {
			NBTTagList doubleList = (NBTTagList) tag;
			Double[] doubleArray = new Double[doubleList.tagCount()];
			for (int i = 0; i < doubleArray.length; i++) {
				doubleArray[i] = ((NBTTagDouble) doubleList.get(i)).getDouble();
			}
			return doubleArray;
		} else if (type == String[].class && tag instanceof NBTTagList) {
			NBTTagList stringList = (NBTTagList) tag;
			String[] stringArray = new String[stringList.tagCount()];
			for (int i = 0; i < stringArray.length; i++) {
				stringArray[i] = ((NBTTagString) stringList.get(i)).getString();
			}
			return stringArray;
		} else if (List.class.isAssignableFrom(type) && tag instanceof NBTTagList) {
			NBTTagList valueList = (NBTTagList) tag;
			List<Object> list;
			try {
				list = (List<Object>) (type == List.class ? ArrayList.class : type).newInstance();
			} catch (Exception e) {
				throw new NBTAssistObjectInstantiationError(e);
			}
			for (int i = 0; i < valueList.tagCount(); i++) {
				Object e = readFromNBT((Class<?>) ((ParameterizedType) parameterizedType).getActualTypeArguments()[0], null, valueList.get(i));
				list.add(e);
			}
			return list;
		} else if (Map.class.isAssignableFrom(type) && tag instanceof NBTTagList) {
			NBTTagList valueCompound = (NBTTagList) tag;
			ParameterizedType mapType = (ParameterizedType) parameterizedType;
			Class<?> keyType = (Class<?>) mapType.getActualTypeArguments()[0];
			Class<?> valueType = (Class<?>) mapType.getActualTypeArguments()[1];
			Map<Object, Object> map;
			try {
				map = (Map<Object, Object>) (type == Map.class ? HashMap.class : type).newInstance();
			} catch (Exception e) {
				throw new NBTAssistObjectInstantiationError(e);
			}
			for (int i = 0; i < valueCompound.tagCount(); i++) {
				NBTTagCompound entryCompound = valueCompound.getCompoundTagAt(i);
				NBTBase entryKeyCompound = entryCompound.getTag("key");
				NBTBase entryValueCompound = entryCompound.getTag("value");
				map.put(readFromNBT(keyType, null, entryKeyCompound), readFromNBT(valueType, null, entryValueCompound));
			}
			return map;
		} else if (type == ItemStack.class && tag instanceof NBTTagCompound) {
			return ItemStack.loadItemStackFromNBT((NBTTagCompound) tag);
		} else if (type == UUID.class && tag instanceof NBTTagCompound) {
			return new UUID(((NBTTagCompound) tag).getLong("most"), ((NBTTagCompound) tag).getLong("least"));
		} else if (type == BlockPos.class && tag instanceof NBTTagCompound) {
			NBTTagCompound blockPosCompound = (NBTTagCompound) tag;
			return new BlockPos(blockPosCompound.getInteger("x"), blockPosCompound.getInteger("y"), blockPosCompound.getInteger("z"));
		} else if (type.isEnum() && tag instanceof NBTTagString) {
			return Enum.valueOf((Class<Enum>) type, ((NBTTagString) tag).getString());
		} else if (type.isArray() && tag instanceof NBTTagList) {
			NBTTagList valueList = (NBTTagList) tag;
			Class<?> arrayType = type.getComponentType();
			Object[] valueArray = (Object[]) Array.newInstance(arrayType, valueList.tagCount());
			for (int i = 0; i < valueArray.length; i++) {
				NBTBase valueElement = valueList.get(i);
				valueArray[i] = readFromNBT(arrayType, null, valueElement);
			}
			return valueArray;
		} else if (tag instanceof NBTTagCompound) {
			Object objValue;
			try {
				Constructor constructor = null;
				try {
					constructor = type.getDeclaredConstructor();
				} catch (NoSuchMethodException e) {}
				if (constructor == null) {
					objValue = UnsafeAllocator.create().newInstance(type);
				} else {
					constructor.setAccessible(true);
					objValue = constructor.newInstance();
				}
			} catch (Exception e) {
				throw new NBTAssistObjectInstantiationError(e);
			}
			read(objValue, (NBTTagCompound) tag);
			return objValue;
		}
		return null;
	}

	private static Method getSetter(Class<?> clazz, String name, Class<?> type, String setter) {
		String setterName = getConventionalSetterName(name, type, setter);
		return resolveMutator(clazz, setterName, type);
	}

	private static Method getGetter(Class<?> clazz, String name, Class<?> type, String getter) {
		String getterName = getConventionalGetterName(name, type, getter);
		return resolveMutator(clazz, getterName);
	}

	private static Method resolveMutator(Class<?> clazz, String name, Class<?>... param) {
		Method method = null;
		Class<?> methodClass = clazz;
		do {
			try {
				method = methodClass.getDeclaredMethod(name, param);
				break;
			} catch (NoSuchMethodException e) {}
		} while ((methodClass = methodClass.getSuperclass()) != null);
		if (method == null) {
			StringBuilder message = new StringBuilder();
			message.append(clazz.getName());
			message.append('.');
			message.append(name);
			message.append('(');
			message.append(param.length == 0 ? "" : param[0] == null ? "null" : param[0].getName());
			message.append(')');
			throw new NBTAssitNoSuchMutatorError(message.toString());
		}
		method.setAccessible(true);
		return method;
	}

	private static String getConventionalSetterName(String name, Class<?> type, String setter) {
		return getConventionalMutatorName(VERB_SET, name, setter);
	}

	private static String getConventionalGetterName(String name, Class<?> type, String getter) {
		return getConventionalMutatorName(type == boolean.class || type == Boolean.class ? VERB_IS : VERB_GET, name, getter);
	}

	private static String getConventionalMutatorName(String verb, String name, String mutator) {
		if (mutator.isEmpty()) {
			return new StringBuilder(verb)
				.append(Character.toUpperCase(name.charAt(0)))
				.append(name.substring(1))
				.toString();
		}
		return mutator;
	}
}
