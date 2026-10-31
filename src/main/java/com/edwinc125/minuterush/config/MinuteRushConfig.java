package com.edwinc125.minuterush.config;

public record MinuteRushConfig(int targetScore, int roundDurationSeconds) {
	public static final MinuteRushConfig DEFAULT = new MinuteRushConfig(10, 60);

	public MinuteRushConfig {
		if (targetScore < 1 || roundDurationSeconds < 1) {
			throw new IllegalArgumentException("Score target and round duration must be positive");
		}
	}

	public MinuteRushConfig withTargetScore(int targetScore) {
		return new MinuteRushConfig(targetScore, roundDurationSeconds);
	}

	public MinuteRushConfig withRoundDurationSeconds(int roundDurationSeconds) {
		return new MinuteRushConfig(targetScore, roundDurationSeconds);
	}
}
