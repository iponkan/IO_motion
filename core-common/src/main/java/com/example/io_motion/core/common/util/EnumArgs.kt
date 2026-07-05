package com.example.io_motion.core.common.util

/**
 * Parses [value] as a constant of enum [T], falling back to [default] for null, blank, or
 * unrecognized values (e.g. a stale deep link, a restored nav backstack referencing a renamed
 * constant, or a `SavedStateHandle` entry from an older app version) instead of throwing.
 */
inline fun <reified T : Enum<T>> parseEnumOrDefault(value: String?, default: T): T =
    value?.let { raw -> enumValues<T>().firstOrNull { it.name == raw } } ?: default
