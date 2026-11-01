package com.edwinc125.minuterush.task;

import java.util.Set;
import java.util.random.RandomGenerator;

public final class TaskSelection {
	private TaskSelection() {}

	public static int chooseIndex(RandomGenerator random, int size, Set<Integer> recent) {
		if (size < 1) {
			throw new IllegalArgumentException("Task pool must not be empty");
		}
		if (recent.size() >= size) {
			return random.nextInt(size);
		}
		int candidate;
		do {
			candidate = random.nextInt(size);
		} while (recent.contains(candidate));
		return candidate;
	}
}
