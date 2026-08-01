package com.skyframework.islandcore.api.network;

import org.jetbrains.annotations.Nullable;

/**
 * Structured result of a service-layer action, shared by every text command and network packet
 * handler that wraps one of the extracted service methods. {@code reason} is one of the
 * {@link ActionReason} constants on failure, {@code null} on success. {@code data} is whatever
 * extra information the specific method's javadoc documents (often {@code null}/unused) — kept
 * as a plain type parameter rather than a fixed shape since different actions need wildly
 * different extra data (an Island, a remaining cooldown in seconds, a list of tier ids...).
 *
 * <p>Factory methods are named {@code ok}/{@code fail} rather than {@code success}/{@code
 * failure}: the record component is itself named {@code success} (so callers can write {@code
 * outcome.success()}), and Java doesn't allow a static and instance method sharing one signature.
 */
public record ActionOutcome<T>(boolean success, @Nullable String reason, @Nullable T data) {

	public static <T> ActionOutcome<T> ok() {
		return new ActionOutcome<>(true, null, null);
	}

	public static <T> ActionOutcome<T> ok(T data) {
		return new ActionOutcome<>(true, null, data);
	}

	public static <T> ActionOutcome<T> fail(String reason) {
		return new ActionOutcome<>(false, reason, null);
	}
}
