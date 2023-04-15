package com.spotlight.platform.userprofile.api.web.resources;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.spotlight.platform.userprofile.api.core.profile.UserProfileService;
import com.spotlight.platform.userprofile.api.core.profile.persistence.UserProfileDao;
import com.spotlight.platform.userprofile.api.dtos.UserProfileCommand;
import com.spotlight.platform.userprofile.api.model.profile.UserProfile;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserId;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfileFixtures;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfilePropertyName;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfilePropertyValue;
import com.spotlight.platform.userprofile.api.web.UserProfileApiApplication;


import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.HashMap;
import java.util.Optional;

import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.SAME_THREAD)
class UserResourceIntegrationTest {
    @RegisterExtension
    static TestDropwizardAppExtension APP = TestDropwizardAppExtension.forApp(UserProfileApiApplication.class)
            .randomPorts()
            .hooks(builder -> builder.modulesOverride(new AbstractModule() {
                @Provides
                @Singleton
                public UserProfileDao getUserProfileDao() {
                    return mock(UserProfileDao.class);
                }
            }))
            .randomPorts()
            .create();

    @BeforeEach
    void beforeEach(UserProfileDao userProfileDao) {
        reset(userProfileDao);
    }

    @Nested
    @DisplayName("getUserProfile")
    class GetUserProfile {
        private static final String USER_ID_PATH_PARAM = "userId";
        private static final String URL = "/users/{%s}/profile".formatted(USER_ID_PATH_PARAM);

        @Test
        void existingUser_correctObjectIsReturned(ClientSupport client, UserProfileDao userProfileDao) {
            when(userProfileDao.get(any(UserId.class))).thenReturn(Optional.of(UserProfileFixtures.USER_PROFILE));

            var response = client.targetRest().path(URL).resolveTemplate(USER_ID_PATH_PARAM, UserProfileFixtures.USER_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
            assertThatJson(response.readEntity(UserProfile.class)).isEqualTo(UserProfileFixtures.SERIALIZED_USER_PROFILE);
        }

        @Test
        void nonExistingUser_returns404(ClientSupport client, UserProfileDao userProfileDao) {
            when(userProfileDao.get(any(UserId.class))).thenReturn(Optional.empty());

            var response = client.targetRest().path(URL).resolveTemplate(USER_ID_PATH_PARAM, UserProfileFixtures.USER_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
        }

        @Test
        void validationFailed_returns400(ClientSupport client) {
            var response = client.targetRest()
                    .path(URL)
                    .resolveTemplate(USER_ID_PATH_PARAM, UserProfileFixtures.INVALID_USER_ID)
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
        }

        @Test
        void unhandledExceptionOccured_returns500(ClientSupport client, UserProfileDao userProfileDao) {
            when(userProfileDao.get(any(UserId.class))).thenThrow(new RuntimeException("Some unhandled exception"));

            var response = client.targetRest().path(URL).resolveTemplate(USER_ID_PATH_PARAM, UserProfileFixtures.USER_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500);
        }


    }
    @Nested
    @DisplayName("Process Command(s)")
    class ProcessCommand {

        private static final String USER_ID_PATH_PARAM = "de4310e5-b139-441a-99db-77c9c4a5fada";
        private static final String URL = "/users/%s/profile/command".formatted(USER_ID_PATH_PARAM);;
        private static final String BATCH_URL = "/users/%s/profile/commands".formatted(USER_ID_PATH_PARAM);;

        @Test
        void invalidInput_Type_returns400(ClientSupport client) {
            JSONObject jsonProperties = new JSONObject();
            jsonProperties.put("battleFought", 10);
            jsonProperties.put("questsNotCompleted", -1);

            JSONObject payload = new JSONObject();
            payload.put("userId", "de4310e5-b139-441a-99db-77c9c4a5fada");
            payload.put("type", "anyType");
            payload.put("properties", jsonProperties);

            var response = client.targetRest()
                    .path(URL)
                    .request()
                    .post(Entity.json(payload.toString()));

            assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
        }

        @Test
        void invalidInput_collect_returns400(ClientSupport client) {

            JSONObject jsonProperties = new JSONObject();
            jsonProperties.put("inventory", "newWeapon");

            JSONObject payload = new JSONObject();
            payload.put("userId", "de4310e5-b139-441a-99db-77c9c4a5fada");
            payload.put("type", "collect");
            payload.put("properties", jsonProperties);


            var response = client.targetRest()
                    .path(URL)
                    .request()
                    .post(Entity.json(payload.toString()));

            assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
        }

        @Test
        void invalidInput_increment_returns400(ClientSupport client) {

            JSONObject jsonProperties = new JSONObject();
            jsonProperties.put("battleFought", 10);
            jsonProperties.put("questsNotCompleted", "new");

            JSONObject payload = new JSONObject();
            payload.put("userId", "de4310e5-b139-441a-99db-77c9c4a5fada");
            payload.put("type", "increment");
            payload.put("properties", jsonProperties);


            var response = client.targetRest()
                    .path(URL)
                    .request()
                    .post(Entity.json(payload.toString()));

            assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
        }

        @Test
        void validInput_processCommands(ClientSupport client, UserProfileDao userProfileDao) {
            userProfileDao.put(UserProfileFixtures.USER_PROFILE);


            JSONObject jsonProperties = new JSONObject();
            jsonProperties.put("battleFought", 10);
            jsonProperties.put("questsNotCompleted", -1);

            JSONObject payload = new JSONObject();
            payload.put("userId", "de4310e5-b139-441a-99db-77c9c4a5fada");
            payload.put("type", "increment");
            payload.put("properties", jsonProperties);

            try {
                var response = client.targetRest()
                        .path(URL)
                        .request()
                        .post(Entity.json(payload.toString()));


                assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

            } catch (ProcessingException e) {
                // handle exception
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error processing request: " + e.getMessage())
                        .build();
            }
        }

        @Test
        void validInput_processCommandsBatch(ClientSupport client, UserProfileDao userProfileDao) {
            userProfileDao.put(UserProfileFixtures.USER_PROFILE);

            JSONArray commandList = new JSONArray();

            JSONObject command1 = new JSONObject();
            JSONObject properties1 = new JSONObject();
            properties1.put("battleFought", 10);
            properties1.put("questsNotCompleted", -1);
            command1.put("userId", "de4310e5-b139-441a-99db-77c9c4a5fada");
            command1.put("type", "replace");
            command1.put("properties", properties1);

            JSONObject command2 = new JSONObject();
            JSONObject properties2 = new JSONObject();
            properties2.put("battleFought", 8);
            properties2.put("questsNotCompleted", 10);
            command2.put("userId", "de4310e5-b139-441a-99db-77c9c4a5fada");
            command2.put("type", "increment");
            command2.put("properties", properties2);

            commandList.add(command1);
            commandList.add(command2);

            try {
                var response = client.targetRest()
                        .path(BATCH_URL)
                        .request()
                        .post(Entity.json(commandList.toString()));


                assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

            } catch (ProcessingException e) {
                // handle exception
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error processing request: " + e.getMessage())
                        .build();
            }
        }

        @Test
        void not_validInput_processCommandsBatch(ClientSupport client, UserProfileDao userProfileDao) {
            userProfileDao.put(UserProfileFixtures.USER_PROFILE);

            JSONArray commandList = new JSONArray();

            JSONObject command1 = new JSONObject();
            JSONObject properties1 = new JSONObject();
            properties1.put("battleFought", 10);
            properties1.put("questsNotCompleted", -1);
            command1.put("userId", "de4310e5-b139-441a-99db-77c9c4a5fada");
            command1.put("type", "replace");
            command1.put("properties", properties1);

            JSONObject command2 = new JSONObject();
            JSONObject properties2 = new JSONObject();
            properties2.put("battleFought", "new");
            properties2.put("questsNotCompleted", 10);
            command2.put("userId", "de4310e5-b139-441a-99db-77c9c4a5fada");
            command2.put("type", "increment");
            command2.put("properties", properties2);



            commandList.add(command1);
            commandList.add(command2);

            try {
                var response = client.targetRest()
                        .path(BATCH_URL)
                        .request()
                        .post(Entity.json(commandList.toString()));


                assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

            } catch (ProcessingException e) {
                // handle exception
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error processing request: " + e.getMessage())
                        .build();
            }
        }


    }

}