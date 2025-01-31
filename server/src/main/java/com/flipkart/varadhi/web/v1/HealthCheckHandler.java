package com.flipkart.varadhi.web.v1;

import com.flipkart.varadhi.exceptions.ServerNotAvailableException;
import com.flipkart.varadhi.web.Extensions.RoutingContextExtension;
import com.flipkart.varadhi.web.routes.RouteDefinition;
import com.flipkart.varadhi.web.routes.RouteProvider;
import com.google.common.collect.Sets;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import lombok.experimental.ExtensionMethod;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

@ExtensionMethod({RoutingContextExtension.class})
public class HealthCheckHandler implements Handler<RoutingContext>, RouteProvider {

    // TODO: add appropriate checks

    private volatile int responseCode = HTTP_OK;
    private volatile String responseMsg = "iam_ok";

    @Override
    public void handle(RoutingContext ctx) {
        if (responseCode == HTTP_OK) {
            ctx.endApiWithResponse(responseMsg);
        } else {
            throw new ServerNotAvailableException(responseMsg);
        }
    }

    public void bringOOR() {
        responseCode = HTTP_UNAVAILABLE;
        responseMsg = "not_ok: under orr";
    }

    @Override
    public List<RouteDefinition> get() {
        return List.of(
                new RouteDefinition(
                        HttpMethod.GET,
                        "/v1/health-check",
                        Set.of(),
                        Sets.newLinkedHashSet(),
                        this::handle,
                        true,
                        Optional.empty()
                )

        );
    }
}
