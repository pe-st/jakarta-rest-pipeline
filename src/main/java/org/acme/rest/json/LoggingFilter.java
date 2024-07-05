package org.acme.rest.json;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

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
        final String clazz = resourceInfo.getResourceClass().getSimpleName();
        final String method = resourceInfo.getResourceMethod().getName();

        log.info("Request Filter: {} {} from IP {} [method '{}' from {}]", httpMethod, path, address, method, clazz);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

        final String entity = responseContext.getEntity().toString();

        log.info("Response Filter: {}", entity);
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
    }
}
