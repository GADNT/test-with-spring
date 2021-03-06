
package com.testwithspring.intermediate.web;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.testwithspring.intermediate.*;
import com.testwithspring.intermediate.common.ConstantDateTimeService;
import com.testwithspring.intermediate.config.Profiles;
import com.testwithspring.intermediate.task.TaskFormDTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestContext.class})
//@ContextConfiguration(locations = {"classpath:integration-test-context.xml"})
@WebAppConfiguration
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
        ServletTestExecutionListener.class,
        WithSecurityContextTestExecutionListener.class
})
@DatabaseSetup({
        "/com/testwithspring/intermediate/users.xml",
        "task.xml"
})
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader.class)
@Category(IntegrationTest.class)
@ActiveProfiles(Profiles.INTEGRATION_TEST)
public class UpdateTaskAsUserWhenValidationIsSuccessful {

    private static final String NEW_DESCRIPTION = "The old lesson was not good";
    private static final String NEW_TITLE = "Rewrite an existing lesson";

    @Autowired
    private WebApplicationContext webAppContext;

    private MockMvc mockMvc;

    @Before
    public void configureSystemUnderTest() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithUserDetails(Users.JohnDoe.EMAIL_ADDRESS)
    public void shouldReturnHttpStatusCodeOk() throws Exception {
        updateTask()
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(Users.JohnDoe.EMAIL_ADDRESS)
    public void shouldReturnUpdatedTaskAsJson() throws Exception {
        updateTask()
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8));
    }

    @Test
    @WithUserDetails(Users.JohnDoe.EMAIL_ADDRESS)
    public void shouldReturnInformationOfUpdatedTask() throws Exception {
        updateTask()
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.ASSIGNEE, nullValue()))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.CLOSER, nullValue()))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.CREATION_TIME, is(Tasks.WriteLesson.CREATION_TIME_STRING)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.Creator.ID, is(Tasks.WriteLesson.Creator.ID.intValue())))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.Creator.NAME, is(Tasks.WriteLesson.Creator.NAME)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.ID, is(Tasks.WriteLesson.ID.intValue())))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.MODIFICATION_TIME, is(ConstantDateTimeService.CURRENT_DATE_AND_TIME)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.Modifier.ID, is(Users.JohnDoe.ID.intValue())))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.Modifier.NAME, is(Users.JohnDoe.NAME)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.TITLE, is(NEW_TITLE)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.DESCRIPTION, is(NEW_DESCRIPTION)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.STATUS, is(Tasks.WriteLesson.STATUS.toString())))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.RESOLUTION, nullValue()));
    }

    @Test
    @WithUserDetails(Users.JohnDoe.EMAIL_ADDRESS)
    public void shouldReturnTaskThatHasOneTag() throws Exception {
        updateTask()
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.TAGS, hasSize(1)));
    }

    @Test
    @WithUserDetails(Users.JohnDoe.EMAIL_ADDRESS)
    public void shouldReturnInformationOfCorrectTag() throws Exception {
        updateTask()
                .andExpect(jsonPath("$.tags[0].id", is(Tasks.WriteLesson.Tags.Lesson.ID.intValue())))
                .andExpect(jsonPath("$.tags[0].name", is(Tasks.WriteLesson.Tags.Lesson.NAME)));
    }

    @Test
    @WithUserDetails(Users.JohnDoe.EMAIL_ADDRESS)
    @ExpectedDatabase(value = "update-task-should-update-title-and-description.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void shouldUpdateTitleAndDescription() throws Exception {
        updateTask();
    }

    @Test
    @WithUserDetails(Users.JohnDoe.EMAIL_ADDRESS)
    @ExpectedDatabase(value = "update-task-as-user-should-update-lifecycle-fields.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void shouldUpdateModificationTimeAndVersion() throws Exception {
        updateTask();
    }

    @Test
    @WithUserDetails(Users.JohnDoe.EMAIL_ADDRESS)
    @ExpectedDatabase(value = "update-task-should-not-change-status.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void shouldNotChangeStatus() throws Exception {
        updateTask();
    }

    @Test
    @WithUserDetails(Users.JohnDoe.EMAIL_ADDRESS)
    @ExpectedDatabase(value = "update-task-should-not-change-assignee.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void shouldNotChangeAssignee() throws Exception {
        updateTask();
    }

    @Test
    @WithUserDetails(Users.JohnDoe.EMAIL_ADDRESS)
    @ExpectedDatabase(value = "update-task-should-not-change-tags.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void shouldNotMakeAnyChangesToTagsOfUpdatedTask() throws Exception {
        updateTask();
    }

    private ResultActions updateTask() throws Exception {
        TaskFormDTO input = new TaskFormDTO();
        input.setDescription(NEW_DESCRIPTION);
        input.setId(Tasks.WriteLesson.ID);
        input.setTitle(NEW_TITLE);

        return mockMvc.perform(put("/api/task/{taskId}", Tasks.WriteLesson.ID)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(WebTestUtil.convertObjectToJsonBytes(input))
                .with(csrf())
        );
    }
}
