package com.spotlight.platform.userprofile.api.web.validation;

import com.spotlight.platform.userprofile.api.dtos.UserProfileCommand;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Map;


public class UserProfileCommandValidator implements ConstraintValidator<ValidUserProfileCommand, UserProfileCommand> {
    @Override
    public boolean isValid(UserProfileCommand command, ConstraintValidatorContext context) {
        if (command == null || command.getType() == null) {
            return false;
        }
        if ((command.getType() == UserProfileCommand.CommandType.INCREMENT)
                && !isAllValuesInteger(command.getProperties())) {
            return false;
        }

        if (command.getType() == UserProfileCommand.CommandType.COLLECT
                && !isAllValuesList(command.getProperties())) {
            return false;
        }
        return true;
    }

    private boolean isAllValuesInteger(Map<String, Object> properties) {
        if (properties == null) {
            return true;
        }
        return properties.values().stream().allMatch(value -> isInteger(value.toString()));
    }

    private boolean isInteger(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isAllValuesList(Map<String, Object> properties) {
        if (properties == null) {
            return true;
        }
        return properties.values().stream().allMatch(value -> value instanceof List);
    }
}
