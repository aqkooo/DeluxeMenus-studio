package com.ejyqyl.dmstudio.model.validation;

/**
 * A validation issue found by the ValidationEngine.
 */
public record ValidationIssue(
        String code,
        ValidationLevel level,
        String message,
        String itemKey,
        Integer slot,
        String fieldName
) {
    public static ValidationIssue error(String code, String message, String itemKey, Integer slot, String fieldName) {
        return new ValidationIssue(code, ValidationLevel.ERROR, message, itemKey, slot, fieldName);
    }

    public static ValidationIssue warning(String code, String message, String itemKey, Integer slot, String fieldName) {
        return new ValidationIssue(code, ValidationLevel.WARNING, message, itemKey, slot, fieldName);
    }

    public static ValidationIssue info(String code, String message, String itemKey, Integer slot, String fieldName) {
        return new ValidationIssue(code, ValidationLevel.INFO, message, itemKey, slot, fieldName);
    }
}
