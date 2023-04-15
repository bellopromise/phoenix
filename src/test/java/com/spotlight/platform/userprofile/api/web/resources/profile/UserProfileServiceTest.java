package com.spotlight.platform.userprofile.api.web.resources.profile;

import com.spotlight.platform.userprofile.api.core.profile.UserProfileService;
import com.spotlight.platform.userprofile.api.core.profile.persistence.UserProfileDao;
import com.spotlight.platform.userprofile.api.dtos.UserProfileCommand;
import com.spotlight.platform.userprofile.api.model.profile.UserProfile;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserId;

import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfilePropertyName;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfilePropertyValue;
import com.spotlight.platform.userprofile.api.web.UserProfileApiApplication;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
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


@Execution(ExecutionMode.SAME_THREAD)
@TestDropwizardApp(value = UserProfileApiApplication.class, randomPorts = true)
class UserProfileServiceTest {

    Map<UserProfilePropertyName, UserProfilePropertyValue> theUserProperties;

    @BeforeEach
    void setup() {
        theUserProperties = new HashMap<>();
    }





    @Test
    void validInput_replace_ProfileNotFound(UserProfileService userProfileService, UserProfileDao userProfileDao) {
        String userId = "de4310e5-b139-441a-99db-77c9c4a5fada";

        theUserProperties = new HashMap<>();
        theUserProperties.put(UserProfilePropertyName.valueOf("currentGold"), UserProfilePropertyValue.valueOf(250));
        UserProfile newUserProfile  = new UserProfile(UserId.valueOf(userId), LocalDateTime.MAX.toInstant(ZoneOffset.UTC), theUserProperties);

        userProfileDao.put(newUserProfile);


        UserProfileCommand command = new UserProfileCommand();

        command.setType(String.valueOf(UserProfileCommand.CommandType.REPLACE));
        command.setUserId(UserId.valueOf(userId));
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("currentGold", 350);
        command.setProperties(properties);


        userProfileService.processCommands(command);

        UserProfile updatedUserProfile = userProfileService.get(UserId.valueOf(userId));

        assertEquals(UserProfilePropertyValue.valueOf(350), updatedUserProfile.userProfileProperties().get(UserProfilePropertyName.valueOf("currentGold")));
    }

    @Test
    void validInput_replace_New(UserProfileService userProfileService) {
        String userId = "de4310e5-b139-441a-99db-77c9c4a5fada";
        UserProfileCommand command = new UserProfileCommand();

        command.setType(String.valueOf(UserProfileCommand.CommandType.REPLACE));
        command.setUserId(UserId.valueOf(userId));
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("currentSilver", 75);
        command.setProperties(properties);


        userProfileService.processCommands(command);

        UserProfile updatedUserProfile = userProfileService.get(UserId.valueOf(userId));

        assertEquals(UserProfilePropertyValue.valueOf(75), updatedUserProfile.userProfileProperties().get(UserProfilePropertyName.valueOf("currentSilver")));
    }

    @Test
    void validInput_increment(UserProfileService userProfileService, UserProfileDao userProfileDao) {
        String userId = "de4310e5-b139-441a-99db-77c9c4a5fada";
        Map<UserProfilePropertyName, UserProfilePropertyValue> theUserProperties = new HashMap<>();
        theUserProperties.put(UserProfilePropertyName.valueOf("battleFought"), UserProfilePropertyValue.valueOf(10));
        theUserProperties.put(UserProfilePropertyName.valueOf("questsNotCompleted"), UserProfilePropertyValue.valueOf(5));
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

        UserProfilePropertyValue expected = UserProfilePropertyValue.valueOf(30);
        UserProfilePropertyValue actual = updatedUserProfile.userProfileProperties().get(UserProfilePropertyName.valueOf("battleFought"));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void validInput_increment_NewProperty(UserProfileService userProfileService, UserProfileDao userProfileDao) {
        String userId = "de4310e5-b139-441a-99db-77c9c4a5fada";
        theUserProperties = new HashMap<>();

        theUserProperties.put(UserProfilePropertyName.valueOf("battleFought"), UserProfilePropertyValue.valueOf(10));
        theUserProperties.put(UserProfilePropertyName.valueOf("questsNotCompleted"), UserProfilePropertyValue.valueOf(5));
        UserProfile newUserProfile = new UserProfile(UserId.valueOf(userId), LocalDateTime.MAX.toInstant(ZoneOffset.UTC), theUserProperties);

        userProfileDao.put(newUserProfile);


        UserProfileCommand command = new UserProfileCommand();

        command.setType(String.valueOf(UserProfileCommand.CommandType.INCREMENT));
        command.setUserId(UserId.valueOf(userId));
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("battleFought", 20);
        properties.put("questsNotCompleted", -1);
        properties.put("newProp", 4);
        command.setProperties(properties);

        userProfileService.processCommands(command);

        UserProfile updatedUserProfile = userProfileService.get(UserId.valueOf(userId));

        assertEquals(UserProfilePropertyValue.valueOf(4), updatedUserProfile.userProfileProperties().get(UserProfilePropertyName.valueOf("newProp")));

    }

    @Test
    void validInput_collect(UserProfileService userProfileService, UserProfileDao userProfileDao) {
        String userId = "de4310e5-b139-441a-99db-77c9c4a5fada";
        theUserProperties = new HashMap<>();
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
        updatedInventory.add("shield2");
        properties.put("inventory", updatedInventory);
        command.setProperties(properties);

        userProfileService.processCommands(command);

        UserProfile updatedUserProfile = userProfileService.get(UserId.valueOf(userId));
        UserProfilePropertyValue theInventory = updatedUserProfile.userProfileProperties().get(UserProfilePropertyName.valueOf("inventory"));
        List<String> updatedInventoryList = (List<String>) theInventory.getValue();



        assertEquals(Boolean.TRUE, updatedInventoryList.contains("shield2"));
    }


    @Test
    void validInput_collect_NewProperty(UserProfileService userProfileService, UserProfileDao userProfileDao) {
        String userId = "de4310e5-b139-441a-99db-77c9c4a5fada";
        theUserProperties = new HashMap<>();
        List<String> inventory = new ArrayList<>();
        inventory.add("sword1");
        inventory.add("sword2");
        inventory.add("shield1");

        theUserProperties.put(UserProfilePropertyName.valueOf("inventory"), UserProfilePropertyValue.valueOf(inventory));
        UserProfile newUserProfile = new UserProfile(UserId.valueOf(userId), LocalDateTime.MAX.toInstant(ZoneOffset.UTC), theUserProperties);

        userProfileDao.put(newUserProfile);


        UserProfileCommand command = new UserProfileCommand();

        command.setType(String.valueOf(UserProfileCommand.CommandType.COLLECT));
        command.setUserId(UserId.valueOf(userId));
        HashMap<String, Object> properties = new HashMap<>();
        List<String> schoolInventory = new ArrayList<>();
        schoolInventory.add("weapon1");
        properties.put("weapons", schoolInventory);

        command.setProperties(properties);


        userProfileService.processCommands(command);

        UserProfile updatedUserProfile = userProfileService.get(UserId.valueOf(userId));
        UserProfilePropertyValue updateInventory = updatedUserProfile.userProfileProperties().get(UserProfilePropertyName.valueOf("weapons"));
        List<String> kitList = (List<String>) updateInventory.getValue();

        assertEquals(Boolean.TRUE, kitList.contains("weapon1"));
    }

    @Test
    void not_valid_input(UserProfileService userProfileService, UserProfileDao userProfileDao) {
        String userId = "de4310e5-b139-441a-99db-77c9c4a5fada";
        theUserProperties = new HashMap<>();
        theUserProperties.put(UserProfilePropertyName.valueOf("currentBronze"), UserProfilePropertyValue.valueOf(200));
        UserProfile newUserProfile = new UserProfile(UserId.valueOf(userId), LocalDateTime.MAX.toInstant(ZoneOffset.UTC), theUserProperties);

        userProfileDao.put(newUserProfile);


        UserProfileCommand command = new UserProfileCommand();


        assertThrows(IllegalArgumentException.class, () -> command.setType("newProperty"));
    }

    @Test
    void not_valid_input_empty_userId(UserProfileService userProfileService, UserProfileDao userProfileDao) {
        String userId = "";
        UserProfileCommand command = new UserProfileCommand();

        command.setType(String.valueOf(UserProfileCommand.CommandType.REPLACE));
        command.setUserId(UserId.valueOf(userId));
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("currentSilver", 250);
        command.setProperties(properties);





        assertThrows(IllegalArgumentException.class, () ->  userProfileService.processCommands(command));
    }

    @Test
    void not_validInput_collect(UserProfileService userProfileService, UserProfileDao userProfileDao) {
        String userId = "de4310e5-b139-441a-99db-77c9c4a5fada";
        Map<UserProfilePropertyName, UserProfilePropertyValue> theUserProperties = new HashMap<>();

        theUserProperties.put(UserProfilePropertyName.valueOf("inventory"), UserProfilePropertyValue.valueOf("inventory"));
        UserProfile newUserProfile = new UserProfile(UserId.valueOf(userId), LocalDateTime.MAX.toInstant(ZoneOffset.UTC), theUserProperties);

        userProfileDao.put(newUserProfile);


        UserProfileCommand command = new UserProfileCommand();
        // Set valid properties for the command object
        command.setType(String.valueOf(UserProfileCommand.CommandType.COLLECT));
        command.setUserId(UserId.valueOf(userId));
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("inventory", "new");
        command.setProperties(properties);

        assertThrows(IllegalArgumentException.class, () ->  userProfileService.processCommands(command));
    }

    @Test
    void not_validInput_collect_classCast(UserProfileService userProfileService, UserProfileDao userProfileDao) {
        String userId = "de4310e5-b139-441a-99db-77c9c4a5fada";
        Map<UserProfilePropertyName, UserProfilePropertyValue> theUserProperties = new HashMap<>();
        List<String> inventory = new ArrayList<>();

        theUserProperties.put(UserProfilePropertyName.valueOf("inventory"), UserProfilePropertyValue.valueOf(inventory));
        UserProfile newUserProfile = new UserProfile(UserId.valueOf(userId), LocalDateTime.MAX.toInstant(ZoneOffset.UTC), theUserProperties);

        userProfileDao.put(newUserProfile);


        UserProfileCommand command = new UserProfileCommand();
        // Set valid properties for the command object
        command.setType(String.valueOf(UserProfileCommand.CommandType.COLLECT));
        command.setUserId(UserId.valueOf(userId));
        HashMap<String, Object> properties = new HashMap<>();
        List<String> updatedInventory = new ArrayList<>();
        properties.put("inventory", "new");
        command.setProperties(properties);

        assertThrows(ClassCastException.class, () ->  userProfileService.processCommands(command));
    }

    @Test
    void not_validInput_increment(UserProfileService userProfileService, UserProfileDao userProfileDao) {
        String userId = "de4310e5-b139-441a-99db-77c9c4a5fada";
        Map<UserProfilePropertyName, UserProfilePropertyValue> theUserProperties = new HashMap<>();
        theUserProperties.put(UserProfilePropertyName.valueOf("battleFought"), UserProfilePropertyValue.valueOf(10));
        theUserProperties.put(UserProfilePropertyName.valueOf("questsNotCompleted"), UserProfilePropertyValue.valueOf("new"));
        UserProfile newUserProfile  = new UserProfile(UserId.valueOf(userId), LocalDateTime.MAX.toInstant(ZoneOffset.UTC), theUserProperties);

        userProfileDao.put(newUserProfile);


        UserProfileCommand command = new UserProfileCommand();

        command.setType(String.valueOf(UserProfileCommand.CommandType.INCREMENT));
        command.setUserId(UserId.valueOf(userId));
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("battleFought", 10);
        properties.put("questsNotCompleted", "new");
        command.setProperties(properties);

        assertThrows(IllegalArgumentException.class, () ->  userProfileService.processCommands(command));
    }






}