package com.skyframework.islandcore.protection.exception;

import java.util.regex.Pattern;

// Translates one ExceptionGroup pattern string into either a tag reference (resolved at match time
// against the Block/EntityType registry, never via regex) or a compiled, fully-anchored regex.
//
// Rules for a non-tag pattern: '*' becomes '.*'; '(', ')', '|' pass through unchanged (already
// valid regex group/alternation syntax); every other regex metacharacter (. \ + ? [ ] ^ $ { }) is
// escaped; everything else is copied literally. The result is anchored with ^...$ so a pattern only
// ever matches the WHOLE identifier, never a substring of it.
public final class PatternMatcher {

	private final String rawPattern;
	private final boolean tagReference;
	private final String tagPath;
	private final Pattern compiled;

	private PatternMatcher(String rawPattern, boolean tagReference, String tagPath, Pattern compiled) {
		this.rawPattern = rawPattern;
		this.tagReference = tagReference;
		this.tagPath = tagPath;
		this.compiled = compiled;
	}

	public static PatternMatcher compile(String pattern) {
		if (pattern.startsWith("#")) {
			return new PatternMatcher(pattern, true, pattern.substring(1), null);
		}
		return new PatternMatcher(pattern, false, null, Pattern.compile(translateToRegex(pattern)));
	}

	private static String translateToRegex(String pattern) {
		StringBuilder regex = new StringBuilder("^");

		for (int i = 0; i < pattern.length(); i++) {
			char c = pattern.charAt(i);
			switch (c) {
				case '*' -> regex.append(".*");
				case '(', ')', '|' -> regex.append(c);
				case '.', '\\', '+', '?', '[', ']', '^', '$', '{', '}' -> regex.append('\\').append(c);
				default -> regex.append(c);
			}
		}

		regex.append('$');
		return regex.toString();
	}

	public boolean isTagReference() {
		return tagReference;
	}

	// Only meaningful when isTagReference() is true: the tag's own path, without the leading '#'.
	public String getTagPath() {
		return tagPath;
	}

	// Only meaningful when isTagReference() is false.
	public boolean matchesLiteral(String identifier) {
		return compiled.matcher(identifier).matches();
	}

	public String getRawPattern() {
		return rawPattern;
	}
}
