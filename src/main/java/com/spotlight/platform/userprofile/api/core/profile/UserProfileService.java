package com.spotlight.platform.userprofile.api.core.profile;

import com.spotlight.platform.userprofile.api.core.exceptions.EntityNotFoundException;
import com.spotlight.platform.userprofile.api.core.profile.persistence.UserProfileDao;
import com.spotlight.platform.userprofile.api.dtos.UserProfileCommand;
import com.spotlight.platform.userprofile.api.model.profile.UserProfile;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserId;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfilePropertyName;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfilePropertyValue;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class UserProfileService {
    private final UserProfileDao userProfileDao;

    @Inject
    public UserProfileService(UserProfileDao userProfileDao) {
        this.userProfileDao = userProfileDao;
    }

    public UserProfile get(UserId userId) {
        return userProfileDao.get(userId).orElseThrow(EntityNotFoundException::new);
    }

    public void processCommands(UserProfileCommand command) {
        if(command.getUserId().toString() == ""){
            throw new IllegalArgumentException();
        }
        Optional<UserProfile> userProfile = userProfileDao.get(UserId.valueOf(command.getUserId().toString()));
        if(!userProfile.isPresent())
        {
            Map<UserProfilePropertyName, UserProfilePropertyValue> userProfilePropertyNameUserProfilePropertyMap = new ConcurrentHashMap<>();

            command.getProperties().forEach((userProfileKey, useProfilePropertyValue) -> {
                userProfilePropertyNameUserProfilePropertyMap.put(UserProfilePropertyName.valueOf(userProfileKey), UserProfilePropertyValue.valueOf(useProfilePropertyValue));
            });
            UserProfile userNewProfile = new UserProfile(command.getUserId(), LocalDateTime.now().toInstant(ZoneOffset.UTC), userProfilePropertyNameUserProfilePropertyMap);

            userProfileDao.put(userNewProfile);

        }else {
            switch (command.getType()) {
                case REPLACE:
                    replaceProperty(command.getUserId(), command.getProperties());
                    break;
                case INCREMENT:
                    incrementProperty(command.getUserId(), command.getProperties());
                    break;
                case COLLECT:
                    collectProperty(command.getUserId(), command.getProperties());
                    break;
                default:
                    // handle unknown command type
            }
        }

    }

    private void replaceProperty(UserId userId, Map<String, Object> properties) {
        UserProfile userProfile = get(userId);

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            UserProfilePropertyName propertyName = UserProfilePropertyName.valueOf(entry.getKey());
            UserProfilePropertyValue propertyValue = UserProfilePropertyValue.valueOf(entry.getValue());
            userProfile.userProfileProperties().put(propertyName, propertyValue);
        }

        userProfileDao.put(userProfile);
    }

    private void incrementProperty(UserId userId, Map<String, Object> properties) {
        UserProfile userProfile = get(userId);

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            UserProfilePropertyName propertyName = UserProfilePropertyName.valueOf(entry.getKey());
            UserProfilePropertyValue currentValue = userProfile.userProfileProperties().get(propertyName);

            if (currentValue == null) {
                currentValue = UserProfilePropertyValue.valueOf(0);
            }

            if (!(currentValue.getValue() instanceof Number)) {
                throw new IllegalArgumentException();
            }

            Number value = (Number) entry.getValue();
            Number newValue = ((Number) currentValue.getValue()).doubleValue() + value.doubleValue();
            UserProfilePropertyValue newPropertyValue = UserProfilePropertyValue.valueOf(newValue.intValue());

            if(!userProfile.userProfileProperties().containsKey(propertyName)){
                userProfile.userProfileProperties().put(propertyName, newPropertyValue);
            }
            else
            {
                userProfile.userProfileProperties().replace(propertyName, newPropertyValue);
            }

        }

        userProfileDao.put(userProfile);

    }

    private void collectProperty(UserId userId, Map<String, Object> properties) {
        UserProfile userProfile = get(userId);

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            UserProfilePropertyName propertyName = UserProfilePropertyName.valueOf(entry.getKey());
            UserProfilePropertyValue currentValue = userProfile.userProfileProperties().get(propertyName);

            if (currentValue == null) {
                currentValue = UserProfilePropertyValue.valueOf(new ArrayList<>());
            }

            if (!(currentValue.getValue() instanceof List<?>)) {
                throw new IllegalArgumentException();
            }


            @SuppressWarnings("unchecked")
            List<?> value = (List<?>) entry.getValue();
            @SuppressWarnings("unchecked")
            List<Object> newValue = (List<Object>) currentValue.getValue();
            newValue.addAll(value);



            UserProfilePropertyValue newPropertyValue = UserProfilePropertyValue.valueOf(newValue);
            userProfile.userProfileProperties().put(propertyName, newPropertyValue);
        }

        userProfileDao.put(userProfile);
    }


}
