package me.carbou.mathieu.tictactoe.rest;

import me.carbou.mathieu.tictactoe.Env;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 * Ideally, this class would be out of the api and replaced by a Node.js (example) serving Handlebars pages.
 *
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@Path("/")
public class StaticResource {

    @GET
    @Produces("text/html; charset=utf-8")
    public String getRootPage() {
        throw new RedirectionException(Response.Status.MOVED_PERMANENTLY, URI.create("/home.html"));
    }

    @GET
    @Path("{resource: ((?!api/?).)*}")
    //@Cache(maxAge = 60 * 60) // 1-hour cache on browser-side
    public Response getPage(@PathParam("resource") String resource) throws IOException {
        if ("config.js".equals(resource)) {
            return Response.ok("window.config = {fbappid: " + Env.FACEBOOK_APP_ID + "};", "application/javascript").build();
        }
        URL url = getClass().getResource("/root/" + resource);
        if (url == null) throw new NotFoundException(resource);
        String type = URLConnection.getFileNameMap().getContentTypeFor(resource);
        if (type == null) type = MediaType.APPLICATION_OCTET_STREAM;
        return Response.ok(ResourceGroovyMethods.getBytes(url), type).build();
    }

}
