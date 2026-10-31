package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class JumpProgressTest {
	@Test
	void requiresARealAirborneTransition() {
		JumpProgress progress = new JumpProgress(true);
		assertFalse(progress.update(true));
		assertFalse(progress.update(false));
		assertTrue(progress.update(true));
	}

	@Test
	void doesNotAwardAPlayerWhoStartedInTheAir() {
		JumpProgress progress = new JumpProgress(false);
		assertFalse(progress.update(false));
		assertFalse(progress.update(true));
	}
}
