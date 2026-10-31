package com.edwinc125.minuterush.task;

public final class JumpProgress {
	private boolean wasGrounded;
	private boolean jumped;

	public JumpProgress(boolean initiallyGrounded) {
		wasGrounded = initiallyGrounded;
	}

	public boolean update(boolean grounded) {
		if (wasGrounded && !grounded) {
			jumped = true;
		}
		wasGrounded = grounded;
		return jumped && grounded;
	}
}
