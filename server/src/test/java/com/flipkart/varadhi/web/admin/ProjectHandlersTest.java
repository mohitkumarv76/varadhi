package com.flipkart.varadhi.web.admin;

import com.flipkart.varadhi.entities.Org;
import com.flipkart.varadhi.entities.Project;
import com.flipkart.varadhi.entities.Team;
import com.flipkart.varadhi.exceptions.DuplicateResourceException;
import com.flipkart.varadhi.exceptions.InvalidOperationForResourceException;
import com.flipkart.varadhi.exceptions.ResourceNotFoundException;
import com.flipkart.varadhi.services.ProjectService;
import com.flipkart.varadhi.spi.db.MetaStoreException;
import com.flipkart.varadhi.web.ErrorResponse;
import com.flipkart.varadhi.web.WebTestBase;
import com.flipkart.varadhi.web.v1.admin.ProjectHandlers;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.client.HttpRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ProjectHandlersTest extends WebTestBase {

    ProjectHandlers projectHandlers;
    ProjectService projectService;

    Org o1 = new Org("OrgOne", 0);
    Team t1 = new Team("TeamOne", 0, o1.getName());


    @BeforeEach
    public void PreTest() throws InterruptedException {
        super.setUp();
        projectService = mock(ProjectService.class);
        projectHandlers = new ProjectHandlers(projectService);

        Route routeCreate =
                router.post("/projects").handler(bodyHandler).handler(wrapBlocking(projectHandlers::create));
        setupFailureHandler(routeCreate);
        Route routePut = router.put("/projects").handler(bodyHandler).handler(wrapBlocking(projectHandlers::update));
        setupFailureHandler(routePut);
        Route routeGet = router.get("/projects/:project").handler(wrapBlocking(projectHandlers::get));
        setupFailureHandler(routeGet);
        Route routeDelete = router.delete("/projects/:project").handler(wrapBlocking(projectHandlers::delete));
        setupFailureHandler(routeDelete);
    }

    private String getProjectsUrl() {
        return "/projects";
    }

    private String getProjectUrl(String projectName) {
        return String.join("/", getProjectsUrl(), projectName);
    }


    @AfterEach
    public void PostTest() throws InterruptedException {
        super.tearDown();
    }

    private Project getProject(String name) {
        return new Project(name, 0, "Some random value", t1.getName(), t1.getOrg());
    }


    @Test
    public void testProjectCreate() throws InterruptedException {

        HttpRequest<Buffer> request = createRequest(HttpMethod.POST, getProjectsUrl());
        Project p1 = getProject("Project1");

        doReturn(p1).when(projectService).createProject(p1);
        Project p1Created = sendRequestWithBody(request, p1, Project.class);
        Assertions.assertEquals(p1, p1Created);
        verify(projectService, times(1)).createProject(eq(p1));

        String orgNotFoundError = String.format("Org(%s) not found.", t1.getOrg());
        doThrow(new ResourceNotFoundException(orgNotFoundError)).when(projectService).createProject(p1);
        ErrorResponse response = sendRequestWithBody(request, p1, 404, orgNotFoundError, ErrorResponse.class);
        Assertions.assertEquals(orgNotFoundError, response.reason());

        String duplicateOrgError =
                String.format("Project(%s) already exists.  Projects are globally unique.", p1.getName());
        doThrow(new DuplicateResourceException(duplicateOrgError)).when(projectService).createProject(p1);
        response = sendRequestWithBody(request, p1, 409, duplicateOrgError, ErrorResponse.class);
        Assertions.assertEquals(duplicateOrgError, response.reason());

        String someInternalError = "Some random error";
        doThrow(new MetaStoreException(someInternalError)).when(projectService).createProject(p1);
        response = sendRequestWithBody(request, p1, 500, someInternalError, ErrorResponse.class);
        Assertions.assertEquals(someInternalError, response.reason());
    }


    @Test
    public void testProjectGet() throws InterruptedException {
        Project p1 = getProject("Project1");

        HttpRequest<Buffer> request = createRequest(HttpMethod.GET, getProjectUrl(p1.getName()));
        doReturn(p1).when(projectService).getProject(p1.getName());

        Project p1Get = sendRequestWithoutBody(request, Project.class);
        Assertions.assertEquals(p1, p1Get);
        verify(projectService, times(1)).getProject(p1.getName());

        String notFoundError = String.format("Project(%s) not found.", p1.getName());
        doThrow(new ResourceNotFoundException(notFoundError)).when(projectService).getProject(p1.getName());
        ErrorResponse response = sendRequestWithoutBody(request, 404, notFoundError, ErrorResponse.class);
        Assertions.assertEquals(notFoundError, response.reason());
    }


    @Test
    public void testProjectUpdate() throws Exception {
        Project p1 = getProject("Project1");
        HttpRequest<Buffer> request = createRequest(HttpMethod.PUT, getProjectsUrl());
        doReturn(p1).when(projectService).updateProject(p1);
        Project p1Updated = sendRequestWithBody(request, p1, Project.class);
        Assertions.assertEquals(p1, p1Updated);

        String argumentError = String.format("Project(%s) can not be moved across organisation.", p1.getName());
        doThrow(new IllegalArgumentException(argumentError)).when(projectService).updateProject(p1);
        ErrorResponse response = sendRequestWithBody(request, p1, 400, argumentError, ErrorResponse.class);
        Assertions.assertEquals(argumentError, response.reason());

        String invalidOpError =
                String.format(
                        "Conflicting update, Project(%s) has been modified. Fetch latest and try again.",
                        p1.getName()
                );
        doThrow(new InvalidOperationForResourceException(invalidOpError)).when(projectService).updateProject(p1);
        response = sendRequestWithBody(request, p1, 409, invalidOpError, ErrorResponse.class);
        Assertions.assertEquals(invalidOpError, response.reason());
    }

    @Test
    public void testProjectDelete() throws InterruptedException {
        Project p1 = getProject("Project1");

        HttpRequest<Buffer> request = createRequest(HttpMethod.DELETE, getProjectUrl(p1.getName()));
        doNothing().when(projectService).deleteProject(p1.getName());
        sendRequestWithoutBody(request, null);
        verify(projectService, times(1)).deleteProject(p1.getName());

        String notFoundError = String.format("Project(%s) not found.", p1.getName());
        doThrow(new ResourceNotFoundException(notFoundError)).when(projectService)
                .deleteProject(p1.getName());
        ErrorResponse response = sendRequestWithoutBody(request, 404, notFoundError, ErrorResponse.class);
        Assertions.assertEquals(notFoundError, response.reason());

        String invalidOpError =
                String.format("Can not delete Project(%s), it has associated entities.", p1.getName());
        doThrow(new InvalidOperationForResourceException(invalidOpError)).when(projectService)
                .deleteProject(p1.getName());
        response = sendRequestWithoutBody(request, 409, invalidOpError, ErrorResponse.class);
        Assertions.assertEquals(invalidOpError, response.reason());
    }
}
