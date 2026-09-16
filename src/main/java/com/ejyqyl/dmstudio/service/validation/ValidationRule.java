package com.ejyqyl.dmstudio.service.validation;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;

import java.util.List;

/**
 * Interface for an isolated validation rule.
 */
public interface ValidationRule {
    String getCode();
    void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues);
}
