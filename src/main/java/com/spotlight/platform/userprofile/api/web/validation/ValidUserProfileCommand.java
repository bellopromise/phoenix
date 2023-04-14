package com.spotlight.platform.userprofile.api.web.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UserProfileCommandValidator.class)
public @interface ValidUserProfileCommand {
    String message() default "Incorrect command. Please examine the request";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
