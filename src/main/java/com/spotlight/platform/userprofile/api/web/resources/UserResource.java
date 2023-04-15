package com.spotlight.platform.userprofile.api.web.resources;

import com.spotlight.platform.userprofile.api.core.profile.UserProfileService;
import com.spotlight.platform.userprofile.api.dtos.UserProfileCommand;
import com.spotlight.platform.userprofile.api.model.profile.UserProfile;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserId;

import javax.inject.Inject;
import javax.validation.*;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/users/{userId}/profile")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    private final UserProfileService userProfileService;

    @Inject
    public UserResource(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @Path("/")
    @GET
    public UserProfile getUserProfile(@Valid @PathParam("userId") UserId userId) {
        return userProfileService.get(userId);
    }

    @Path("command")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response processCommands( UserProfileCommand command) {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        Set<ConstraintViolation<UserProfileCommand>> violations = validator.validate(command);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(message)
                    .build();
        }

        try {
            userProfileService.processCommands(command);
            return Response.noContent().build();
        }
        catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @Path("commands")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response processCommands(List<UserProfileCommand> commands) {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        StringBuilder errorMessages = new StringBuilder();

        for (UserProfileCommand command : commands) {
            Set<ConstraintViolation<UserProfileCommand>> violations = validator.validate(command);
            if (!violations.isEmpty()) {
                String message = violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
                errorMessages.append(message).append("; ");
            } else {
                try {
                    userProfileService.processCommands(command);
                } catch (Exception e) {
                    errorMessages.append("Error processing command: ").append(e.getMessage()).append("; ");
                }
            }
        }

        if (errorMessages.length() > 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorMessages.toString())
                    .build();
        } else {
            return Response.noContent().build();
        }
    }

}
