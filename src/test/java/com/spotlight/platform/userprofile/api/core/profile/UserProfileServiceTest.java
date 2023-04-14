package com.spotlight.platform.userprofile.api.core.profile;

import com.spotlight.platform.userprofile.api.core.exceptions.EntityNotFoundException;
import com.spotlight.platform.userprofile.api.core.profile.persistence.UserProfileDao;
import com.spotlight.platform.userprofile.api.dtos.UserProfileCommand;
import com.spotlight.platform.userprofile.api.model.profile.UserProfile;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserId;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfileFixtures;

import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfilePropertyName;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfilePropertyValue;
import com.spotlight.platform.userprofile.api.web.UserProfileApiApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.vyarus.dropwizard.guice.test.jupiter.TestDropwizardApp;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@TestDropwizardApp(value = UserProfileApiApplication.class, randomPorts = true)
class UserProfileServiceTest {
    private final UserProfileDao userProfileDaoMock = mock(UserProfileDao.class);
    private final UserProfileService userProfileService = new UserProfileService(userProfileDaoMock);

    @Nested
    @DisplayName("get")
    class Get {
        @Test
        void getForExistingUser_returnsUser() {
            when(userProfileDaoMock.get(any(UserId.class))).thenReturn(Optional.of(UserProfileFixtures.USER_PROFILE));

            assertThat(userProfileService.get(UserProfileFixtures.USER_ID)).usingRecursiveComparison()
                    .isEqualTo(UserProfileFixtures.USER_PROFILE);
        }

        @Test
        void getForNonExistingUser_throwsException() {
            when(userProfileDaoMock.get(any(UserId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userProfileService.get(UserProfileFixtures.USER_ID)).isExactlyInstanceOf(
                    EntityNotFoundException.class);
        }

        @Test
        void replace_ProfileNotFound(UserProfileService userProfileService, UserProfileDao userProfileDao) {
            String userId = "de4310e5-b139-441a-99db-77c9c4a5fada";

            Map<UserProfilePropertyName, UserProfilePropertyValue> theUserProperties = new HashMap<>();
            theUserProperties.put(UserProfilePropertyName.valueOf("currentGold"), UserProfilePropertyValue.valueOf(200));
            UserProfile newUserProfile  = new UserProfile(UserId.valueOf(userId), LocalDateTime.MAX.toInstant(ZoneOffset.UTC), theUserProperties);

            userProfileDao.put(newUserProfile);


            UserProfileCommand command = new UserProfileCommand();

            command.setType(String.valueOf(UserProfileCommand.CommandType.REPLACE));
            command.setUserId(UserId.valueOf(userId));
            HashMap<String, Object> properties = new HashMap<>();
            properties.put("currentGold", 250);
            command.setProperties(properties);


            userProfileService.processCommands(command);

            UserProfile updatedUserProfile = userProfileService.get(UserId.valueOf(userId));

            assertEquals(UserProfilePropertyValue.valueOf(250), updatedUserProfile.userProfileProperties().get(UserProfilePropertyName.valueOf("currentGold")));
        }


        @Test
        void increment(UserProfileService userProfileService, UserProfileDao userProfileDao) {
            String userId = "de4310e5-b139-441a-99db-77c9c4a5fada";
            Map<UserProfilePropertyName, UserProfilePropertyValue> theUserProperties = new HashMap<>();
            theUserProperties.put(UserProfilePropertyName.valueOf("battleFought"), UserProfilePropertyValue.valueOf(10));
            theUserProperties.put(UserProfilePropertyName.valueOf("questsNotCompleted"), UserProfilePropertyValue.valueOf(1));
            UserProfile newUserProfile  = new UserProfile(UserId.valueOf(userId), LocalDateTime.MAX.toInstant(ZoneOffset.UTC), theUserProperties);

            userProfileDao.put(newUserProfile);


            UserProfileCommand command = new UserProfileCommand();

            command.setType(String.valueOf(UserProfileCommand.CommandType.INCREMENT));
            command.setUserId(UserId.valueOf(userId));
            HashMap<String, Object> properties = new HashMap<>();
            properties.put("battleFought", 20);
            properties.put("questsNotCompleted", -1);
            command.setProperties(properties);

            userProfileService.processCommands(command);

            UserProfile updatedUserProfile = userProfileService.get(UserId.valueOf(userId));

            assertEquals(UserProfilePropertyValue.valueOf(0), updatedUserProfile.userProfileProperties().get(UserProfilePropertyName.valueOf("questsNotCompleted")));
            assertEquals(UserProfilePropertyValue.valueOf(30), updatedUserProfile.userProfileProperties().get(UserProfilePropertyName.valueOf("battleFought")));
        }


        @Test
        void increment_NewProperty(UserProfileService userProfileService, UserProfileDao userProfileDao) {
            String userId = "de4310e5-b139-441a-99db-77c9c4a5fada";
            Map<UserProfilePropertyName, UserProfilePropertyValue> theUserProperties = new HashMap<>();
            theUserProperties.put(UserProfilePropertyName.valueOf("battleFought"), UserProfilePropertyValue.valueOf(10));
            theUserProperties.put(UserProfilePropertyName.valueOf("questsNotCompleted"), UserProfilePropertyValue.valueOf(1));
            UserProfile newUserProfile  = new UserProfile(UserId.valueOf(userId), LocalDateTime.MAX.toInstant(ZoneOffset.UTC), theUserProperties);

            userProfileDao.put(newUserProfile);


            UserProfileCommand command = new UserProfileCommand();

            command.setType(String.valueOf(UserProfileCommand.CommandType.INCREMENT));
            command.setUserId(UserId.valueOf(userId));
            HashMap<String, Object> properties = new HashMap<>();
            properties.put("battleFought", 20);
            properties.put("questsNotCompleted", 2);
            properties.put("losses", 2);
            command.setProperties(properties);

            userProfileService.processCommands(command);

            UserProfile updatedUserProfile = userProfileService.get(UserId.valueOf(userId));

            assertEquals(UserProfilePropertyValue.valueOf(2), updatedUserProfile.userProfileProperties().get(UserProfilePropertyName.valueOf("losses")));
        }

        @Test
        void collect(UserProfileService userProfileService, UserProfileDao userProfileDao) {
            String userId = "de4310e5-b139-441a-99db-77c9c4a5fada";
            Map<UserProfilePropertyName, UserProfilePropertyValue> theUserProperties = new HashMap<>();
            List<String> inventory = new ArrayList<>();
            inventory.add("sword1");
            inventory.add("sword2");
            inventory.add("shield1");

            theUserProperties.put(UserProfilePropertyName.valueOf("inventory"), UserProfilePropertyValue.valueOf(inventory));
            UserProfile newUserProfile  = new UserProfile(UserId.valueOf(userId), LocalDateTime.MAX.toInstant(ZoneOffset.UTC), theUserProperties);

            userProfileDao.put(newUserProfile);


            UserProfileCommand command = new UserProfileCommand();
            // Set valid properties for the command object
            command.setType(String.valueOf(UserProfileCommand.CommandType.COLLECT));
            command.setUserId(UserId.valueOf(userId));
            HashMap<String, Object> properties = new HashMap<>();
            List<String> updatedInventory = new ArrayList<>();
            inventory.add("shield2");
            properties.put("inventory", updatedInventory);
            command.setProperties(properties);

            userProfileService.processCommands(command);

            UserProfile updatedUserProfile = userProfileService.get(UserId.valueOf(userId));
            UserProfilePropertyValue theInventory = updatedUserProfile.userProfileProperties().get(UserProfilePropertyName.valueOf("inventory"));
            List<String> updatedInventoryList = (List<String>) theInventory.getValue();

            assertEquals(Boolean.TRUE, updatedInventoryList.contains("shield2") );
        }



        @Test
        void collect_NewProperty(UserProfileService userProfileService, UserProfileDao userProfileDao) {
            String userId = "de4310e5-b139-441a-99db-77c9c4a5fada";
            Map<UserProfilePropertyName, UserProfilePropertyValue> theUserProperties = new HashMap<>();
            List<String> inventory = new ArrayList<>();
            inventory.add("sword1");
            inventory.add("sword2");
            inventory.add("shield1");

            theUserProperties.put(UserProfilePropertyName.valueOf("inventory"), UserProfilePropertyValue.valueOf(inventory));
            UserProfile newUserProfile  = new UserProfile(UserId.valueOf(userId), LocalDateTime.MAX.toInstant(ZoneOffset.UTC), theUserProperties);

            userProfileDao.put(newUserProfile);


            UserProfileCommand command = new UserProfileCommand();

            command.setType(String.valueOf(UserProfileCommand.CommandType.COLLECT));
            command.setUserId(UserId.valueOf(userId));
            HashMap<String, Object> properties = new HashMap<>();
            List<String> schoolInventory = new ArrayList<>();
            schoolInventory.add("school1");
            properties.put("schools", schoolInventory);

            command.setProperties(properties);


            userProfileService.processCommands(command);

            UserProfile updatedUserProfile = userProfileService.get(UserId.valueOf(userId));
            UserProfilePropertyValue updateInventory = updatedUserProfile.userProfileProperties().get(UserProfilePropertyName.valueOf("schools"));
            List<String> kitList = (List<String>) updateInventory.getValue();

            assertEquals(Boolean.TRUE, kitList.contains("school1") );
        }


        @Test
        void not_valid(UserProfileService userProfileService, UserProfileDao userProfileDao) {
            String userId = "de4310e5-b139-441a-99db-77c9c4a5fada";
            Map<UserProfilePropertyName, UserProfilePropertyValue> theUserProperties = new HashMap<>();
            theUserProperties.put(UserProfilePropertyName.valueOf("currentGold"), UserProfilePropertyValue.valueOf(200));
            UserProfile newUserProfile  = new UserProfile(UserId.valueOf(userId), LocalDateTime.MAX.toInstant(ZoneOffset.UTC), theUserProperties);

            userProfileDao.put(newUserProfile);


            UserProfileCommand command = new UserProfileCommand();


            assertThrows(IllegalArgumentException.class, () -> command.setType("newProperty"));
        }
    }
}