package com.pau101.paintthis.server.util;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;

public final class Pool<T> {
	private Supplier<T> instanceProvider;

	private int size;

	private Deque<T> instances;

	public Pool(Supplier<T> instanceProvider, int size) {
		this.instanceProvider = instanceProvider;
		this.size = size;
		instances = new ConcurrentLinkedDeque();
	}

	public T getInstance() {
		if (instances.isEmpty()) {
			return instanceProvider.get();
		}
		return instances.poll();
	}

	public void freeInstance(T instance) {
		if (instances.size() < size) {
			instances.add(instance);
		}
	}
}
