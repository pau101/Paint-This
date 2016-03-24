package com.pau101.paintthis.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Pool<T> {
	private Supplier<T> instanceProvider;

	private int size;

	private List<T> instances;

	public Pool(Supplier<T> instanceProvider, int size) {
		this.instanceProvider = instanceProvider;
		this.size = size;
		instances = new ArrayList<T>();
	}

	public T getInstance() {
		if (instances.isEmpty()) {
			return instanceProvider.get();
		}
		return instances.remove(0);
	}

	public void freeInstance(T instance) {
		if (instances.size() < size) {
			instances.add(instance);
		}
	}
}
