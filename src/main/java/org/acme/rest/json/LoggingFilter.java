package org.acme.rest.json;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.MDC;

import io.vertx.core.http.HttpServerRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
public class LoggingFilter implements ContainerRequestFilter,
        ContainerResponseFilter, ReaderInterceptor, WriterInterceptor {

    static final String RESOURCE_CLASS = "resource-class";
    static final String RESOURCE_METHOD = "resource-method";
    static final String RESOURCE_CONTENT_TYPE = "content-type";

    @Context
    UriInfo info;

    @Context
    HttpServerRequest request;

    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext context) {

        final String httpMethod = context.getMethod();
        final String path = info.getPath();
        final String address = request.remoteAddress().toString();

        MDC.put(RESOURCE_CLASS, resourceInfo.getResourceClass().getSimpleName());
        MDC.put(RESOURCE_METHOD, resourceInfo.getResourceMethod().getName());
        ofNullable(request.getHeader("Content-Type")).ifPresent(contentType -> MDC.put(RESOURCE_CONTENT_TYPE, contentType));

        log.info("Request Filter: {} {} from IP {}", httpMethod, path, address);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

        final String entity = ofNullable(responseContext.getEntity()).map(Object::toString).orElse(null);

        log.info("Response Filter: {}", entity);

        if (requestContext.getLength() <= 0) {
            // in this case the response has no payload and aroundWriteTo() will not be called
            cleanupMdcs();
        }
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext readerContext) throws IOException, WebApplicationException {

        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        try {
            IOUtils.copy(readerContext.getInputStream(), buf);
        } catch (IOException ex) {
            log.info("Problem while copying the request buffer", ex);
        }
        String content = buf.toString(UTF_8);
        ByteArrayInputStream bi = new ByteArrayInputStream(buf.toByteArray());
        readerContext.setInputStream(bi);

        log.info("Reader Interceptor before: {}", content);
        Object entity = readerContext.proceed();
        log.info("Reader Interceptor after: {}", entity);
        return entity;
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext writerContext) throws IOException, WebApplicationException {
        OutputStream oStream = writerContext.getOutputStream();

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writerContext.setOutputStream(buf);

        log.info("Writer Interceptor before: {}", writerContext.getEntity());
        writerContext.proceed();
        String content = buf.toString(UTF_8);
        log.info("Writer Interceptor after: {}", content);

        oStream.write(buf.toByteArray());

        writerContext.setOutputStream(oStream);

        cleanupMdcs();
    }

    private void cleanupMdcs() {
        MDC.remove(RESOURCE_CLASS);
        MDC.remove(RESOURCE_METHOD);
        MDC.remove(RESOURCE_CONTENT_TYPE);
    }
}
