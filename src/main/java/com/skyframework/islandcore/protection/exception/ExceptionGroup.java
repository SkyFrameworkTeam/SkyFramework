package com.skyframework.islandcore.protection.exception;

import com.skyframework.islandcore.protection.flag.FlagPreset;

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
	// The "código" layer in the exception resolution chain (isla -> servidor -> código), same role
	// Flag's own hardcoded roleDefaults table plays for flags — see ExceptionResolver. Translated
	// to VISITOR/ALLY/MEMBER/TRUSTED values via FlagPreset#toRoleValues, exactly like every other
	// preset in this codebase.
	private final FlagPreset defaultPreset;
	private final boolean ownerConfigurable;

	public ExceptionGroup(
			String id,
			ExceptionGroupCategory category,
			List<String> patterns,
			boolean allowInteract,
			boolean allowBreak,
			boolean requireEmptyHand,
			FlagPreset defaultPreset,
			boolean ownerConfigurable
	) {
		this.id = id;
		this.category = category;
		this.patterns = List.copyOf(patterns);
		this.compiledPatterns = this.patterns.stream().map(PatternMatcher::compile).toList();
		this.allowInteract = allowInteract;
		this.allowBreak = allowBreak;
		this.requireEmptyHand = requireEmptyHand;
		this.defaultPreset = defaultPreset;
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

	public FlagPreset getDefaultPreset() {
		return defaultPreset;
	}

	public boolean isOwnerConfigurable() {
		return ownerConfigurable;
	}
}
