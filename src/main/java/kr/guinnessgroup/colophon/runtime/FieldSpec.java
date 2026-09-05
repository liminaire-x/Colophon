package kr.guinnessgroup.colophon.runtime;

/** Describes a single configurable field of a node type (for the editor palette). */
public record FieldSpec(String name, String type, String defaultValue) {}
