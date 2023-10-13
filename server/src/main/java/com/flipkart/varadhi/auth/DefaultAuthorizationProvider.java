package com.flipkart.varadhi.auth;

import com.flipkart.varadhi.entities.UserContext;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;


@Slf4j
public class DefaultAuthorizationProvider implements AuthorizationProvider {
    private DefaultAuthorizationConfiguration configuration;

    @Override
    public Future<Boolean> init(JsonObject configuration) {
        this.configuration = configuration.mapTo(DefaultAuthorizationConfiguration.class);
        return Future.succeededFuture(true);
    }

    /**
     * Checks if the user {@code UserContext} is authorized to perform action {@code ResourceAction} on resource path.
     *
     * @param userContext Information on the user, contains the user identifier
     * @param action      Action being performed by the user which is to be authorized
     * @param resource    Full schemaless URI of the resource on which the action is to be authorized.
     *                    Must be of format: {org_id}/{team_id}/{project_id}/{topic|queue|subscription}
     *
     * @return {@code Future<Boolean>} a future result expressing True/False decision
     */
    @Override
    public Future<Boolean> isAuthorized(UserContext userContext, ResourceAction action, String resource) {
        List<Pair<ResourceType, String>> leafToRootResourceIds = resolveOrderedFromLeaf(action, resource);
        return Future.succeededFuture(leafToRootResourceIds.stream()
                .anyMatch(entry -> isAuthorizedInternal(userContext.getSubject(), action, entry.getValue())));
    }

    /**
     * Parse the resource path based on the action and resolve the final resourceIDs for each resourceType.
     * @param resourcePath uri of the resource
     * @return List of pairs having resource type to its id. List is used so that we can impose ordering from leaf to root nodes.
     */
    private List<Pair<ResourceType, String>> resolveOrderedFromLeaf(ResourceAction action, String resourcePath) {
        String[] segments = resourcePath.split("/");

        // build the list in reverse order specified: ROOT -> ORG -> TEAM -> PROJECT -> TOPIC|SUBSCRIPTION|QUEUE
        List<Pair<ResourceType, String>> resourceIdTuples = new ArrayList<>();
        // handle leaf node case
        if (isActionOnLeafNode(action)) {
            resourceIdTuples.add(Pair.of(action.getResourceType(), getLeaf(segments)));
        }
        resourceIdTuples.add(Pair.of(ResourceType.PROJECT, getProject(segments)));
        resourceIdTuples.add(Pair.of(ResourceType.TEAM, getTeam(segments)));
        resourceIdTuples.add(Pair.of(ResourceType.ORG, getOrg(segments)));
        resourceIdTuples.add(Pair.of(ResourceType.ROOT, ResourceType.ROOT.toString()));

        return resourceIdTuples;
    }

    /**
     * Authorize subject against a single resourceId
     * @param subject user identifier to be authorized
     * @param action action requested by the subject which needs authorization
     * @param resourceId resource id under whose scope the check will be performed
     * @return True, if subject is allowed to perform action under this resource node, else False
     */
    private boolean isAuthorizedInternal(String subject, ResourceAction action, String resourceId) {
        log.debug(
                "Checking authorization for subject [{}] and action [{}] on resource [{}]", subject, action,
                resourceId
        );
        return getRolesForSubject(subject, resourceId).stream()
                .anyMatch(role -> doesActionBelongToRole(subject, role, action));
    }

    private List<String> getRolesForSubject(String subject, String resourceId) {
        return configuration.getRoleBindings()
                .getOrDefault(resourceId, Map.of())
                .getOrDefault(subject, List.of());
    }

    private boolean doesActionBelongToRole(String subject, String roleId, ResourceAction action) {
        log.debug("Evaluating action [{}] for subject [{}] against role [{}]", action, subject, roleId);
        boolean matching = configuration.getRoles().getOrDefault(roleId, List.of()).contains(action);
        if (matching) {
            log.debug("Successfully matched action [{}] for subject [{}] against role [{}]", action, subject, roleId);
        }
        return matching;
    }

    private String getOrg(String[] segments) {
        if (segments.length > 1) {
            return segments[0];
        }
        else return "";
    }

    private String getTeam(String[] segments) {
        if (segments.length > 2) {
            return segments[0] + ":" + segments[1]; //{org_id}:{team_id}
        }
        else return "";
    }

    private String getProject(String[] segments) {
        if (segments.length > 3) {
            return segments[2];
        }
        else return "";
    }

    private String getLeaf(String[] segments) {
        if (segments.length > 4) {
            return segments[2] + ":" + segments[3]; //{project_id}:{[topic|sub|queue]_id}
        }
        else return "";
    }

    private boolean isActionOnLeafNode(ResourceAction action) {
        ResourceType resourceType = action.getResourceType();
        return ResourceType.TOPIC.equals(resourceType)
                || ResourceType.SUBSCRIPTION.equals(resourceType);
    }
}
