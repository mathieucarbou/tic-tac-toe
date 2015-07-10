/**
 * Copyright (C) 2015 Mathieu Carbou (mathieu@carbou.me)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.carbou.mathieu.tictactoe.rest;

import com.guestful.jaxrs.filter.cache.Cache;
import me.carbou.mathieu.tictactoe.Env;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
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
    @Cache(maxAge = 60 * 60) // 1-hour cache on browser-side
    public Response getPage(@PathParam("resource") String resource) throws IOException {
        if ("config.js".equals(resource)) {
            return Response.ok("window.config={fbappid:'" + Env.FACEBOOK_APP_ID + "',pusherkey:'" + URI.create(Env.PUSHER_URL).getUserInfo().split(":")[0] + "'};", "application/javascript").build();
        }
        URL url = null;
        if (Env.isLocal()) {
            File file = new File("src/main/resources/root", resource);
            if (file.exists()) {
                url = file.toURI().toURL();
            }
        } else {
            url = getClass().getResource("/root/" + resource);
        }
        if (url == null) throw new NotFoundException(resource);
        String type = URLConnection.getFileNameMap().getContentTypeFor(resource);
        if(type == null && resource.endsWith(".css")) type = "text/css";
        if (type == null) type = MediaType.APPLICATION_OCTET_STREAM;
        return Response.ok(ResourceGroovyMethods.getBytes(url), type).build();
    }

}
