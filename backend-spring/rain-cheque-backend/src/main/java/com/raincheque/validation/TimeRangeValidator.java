package com.raincheque.validation;

import com.raincheque.dto.EventRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates that an EventRequest's endTime is strictly after its startTime.
 * If either value is null, this validator passes (null checks are handled by @NotNull).
 */
public class TimeRangeValidator implements ConstraintValidator<ValidTimeRange, EventRequest> {

    @Override
    public void initialize(ValidTimeRange constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(EventRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        if (request.startTime() == null || request.endTime() == null) {
            // Let @NotNull handle null fields
            return true;
        }

        return request.endTime().isAfter(request.startTime());
    }
}
