package com.flipkart.varadhi.entities;

import jakarta.ws.rs.HttpMethod;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.net.URL;

public abstract sealed class Endpoint {

    abstract Protocol getProtocol();

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static final class HttpEndpoint extends Endpoint {
        private final URL url;
        private final HttpMethod method;
        private final String contentType;

        private final boolean http2Supported;

        @Override
        Protocol getProtocol() {
            return http2Supported ? Protocol.HTTP2 : Protocol.HTTP1_1;
        }
    }

    public enum Protocol {
        HTTP1_1,
        HTTP2,
    }
}
