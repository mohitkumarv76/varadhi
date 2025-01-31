package com.flipkart.varadhi.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import static com.flipkart.varadhi.Constants.REST_DEFAULTS.*;

@Data
public class RestOptions {
    @NotBlank
    private String deployedRegion;
    @NotNull
    private String projectCacheBuilderSpec = "expireAfterWrite=3600s";
    private int payloadSizeMax = PAYLOAD_SIZE_MAX;
    private int headersAllowedMax = HEADERS_ALLOWED_MAX;
    private int headerNameSizeMax = HEADER_NAME_SIZE_MAX;
    private int headerValueSizeMax = HEADER_VALUE_SIZE_MAX;
    private String defaultOrg = DEFAULT_ORG;
    private String defaultTeam = DEFAULT_TEAM;
    private String defaultProject = DEFAULT_PROJECT;

}
