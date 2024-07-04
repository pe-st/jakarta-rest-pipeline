package org.acme.rest.json;

import java.io.IOException;

import org.jboss.logging.Logger;

import io.vertx.core.http.HttpServerRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

@Provider
public class LoggingFilter implements ContainerRequestFilter,
        ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(LoggingFilter.class);

    @Context
    UriInfo info;

    @Context
    HttpServerRequest request;

    @Override
    public void filter(ContainerRequestContext context) {

        final String method = context.getMethod();
        final String path = info.getPath();
        final String address = request.remoteAddress().toString();

        LOG.infof("Request %s %s from IP %s", method, path, address);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

        final String entity = responseContext.getEntity().toString();

        LOG.infof("Response %s", entity);
    }
}
