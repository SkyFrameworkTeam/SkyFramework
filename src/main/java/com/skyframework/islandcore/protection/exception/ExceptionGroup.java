package com.skyframework.islandcore.protection.exception;

import java.util.List;

// Immutable definition of one exception group. Instances only ever come from
// ExceptionGroupRegistry, loaded from config/islandcore/exception_groups.json.
public final class ExceptionGroup {

	private final String id;
	private final ExceptionGroupCategory category;
	private final List<String> patterns;
	private final List<PatternMatcher> compiledPatterns;
	private final boolean allowInteract;
	private final boolean allowBreak;
	private final boolean requireEmptyHand;
	private final boolean defaultEnabled;
	private final boolean ownerConfigurable;

	public ExceptionGroup(
			String id,
			ExceptionGroupCategory category,
			List<String> patterns,
			boolean allowInteract,
			boolean allowBreak,
			boolean requireEmptyHand,
			boolean defaultEnabled,
			boolean ownerConfigurable
	) {
		this.id = id;
		this.category = category;
		this.patterns = List.copyOf(patterns);
		this.compiledPatterns = this.patterns.stream().map(PatternMatcher::compile).toList();
		this.allowInteract = allowInteract;
		this.allowBreak = allowBreak;
		this.requireEmptyHand = requireEmptyHand;
		this.defaultEnabled = defaultEnabled;
		this.ownerConfigurable = ownerConfigurable;
	}

	public String getId() {
		return id;
	}

	public ExceptionGroupCategory getCategory() {
		return category;
	}

	public List<String> getPatterns() {
		return patterns;
	}

	public List<PatternMatcher> getCompiledPatterns() {
		return compiledPatterns;
	}

	// canBreak (BLOCK) / canAttackEntity (ENTITY).
	public boolean isAllowBreak() {
		return allowBreak;
	}

	// canPlace/canInteractBlock/canOpenContainer (BLOCK) / canInteractEntity (ENTITY).
	public boolean isAllowInteract() {
		return allowInteract;
	}

	public boolean isRequireEmptyHand() {
		return requireEmptyHand;
	}

	public boolean isDefaultEnabled() {
		return defaultEnabled;
	}

	public boolean isOwnerConfigurable() {
		return ownerConfigurable;
	}
}
