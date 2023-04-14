package com.spotlight.platform.userprofile.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserId;
import com.spotlight.platform.userprofile.api.web.validation.ValidUserProfileCommand;

import javax.validation.constraints.NotNull;
import java.util.Map;

@ValidUserProfileCommand
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfileCommand {
    private UserId userId;

    private CommandType type;


    private Map<String, Object> properties;



    public UserProfileCommand() {}

    @JsonCreator
    public UserProfileCommand(@JsonProperty("userId") UserId userId,
                              @JsonProperty("type") CommandType type,
                              @JsonProperty("properties") Map<String, Object> properties) {
        this.userId = userId;
        this.type = type;
        this.properties = properties;
    }

    // Getters and setters
    @JsonProperty("userId")
    @NotNull
    public UserId getUserId() { return userId; }
    public void setUserId(UserId userId) { this.userId = userId; }

    @JsonProperty("type")
    public CommandType getType() { return type; }
    public void setType(String type) {
        this.type = CommandType.valueOf(type.toUpperCase());
    }

    @JsonProperty("properties")
    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }

    public enum CommandType {
        REPLACE,
        INCREMENT,
        COLLECT
    }
}

