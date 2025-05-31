package meldexun.fastentityrender.util;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

public class ArrayStack<E> {

	@SuppressWarnings("unchecked")
	private E[] array = (E[]) new Object[8];
	private int size;

	public boolean isEmpty() {
		return size == 0;
	}

	public void add(E e) {
		ensureCapacity(size + 1);
		array[size++] = e;
	}

	@SuppressWarnings("unchecked")
	private void ensureCapacity(int minCapacity) {
		if (minCapacity <= array.length) return;
		array = (E[]) Arrays.copyOf(array, Math.max(array.length << 1, minCapacity), Object[].class);
	}

	public void addAll(List<E> elements) {
		if (elements == null || elements.isEmpty()) return;
		int n = elements.size();
		ensureCapacity(size + n);
		for (int i = 0; i < n; i++)
			array[size + i] = elements.get(i);
		size += n;
	}

	public E remove() {
		if (size == 0) throw new NoSuchElementException();
		return array[--size];
	}

}
