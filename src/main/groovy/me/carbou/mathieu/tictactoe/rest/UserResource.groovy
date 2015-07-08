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
package me.carbou.mathieu.tictactoe.rest

import com.guestful.client.facebook.FacebookAccessToken
import com.guestful.jaxrs.filter.jsend.Jsend
import com.guestful.jaxrs.security.AuthenticationException
import com.guestful.jaxrs.security.annotation.Authenticated
import com.guestful.jaxrs.security.subject.Subject
import com.guestful.jaxrs.security.subject.SubjectContext
import com.guestful.jaxrs.security.token.FacebookToken
import me.carbou.mathieu.tictactoe.db.DB

import javax.annotation.security.PermitAll
import javax.inject.Inject
import javax.json.JsonObject
import javax.security.auth.login.LoginException
import javax.ws.rs.*
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.Context

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@Path("/api/gamers")
@Jsend
public class UserResource {

    @Inject DB db

    @POST
    @Path("auth/facebook/{appid}/{uid}")
    @Consumes("application/json; charset=utf-8")
    @Produces("application/json; charset=utf-8")
    public Map auth(@Context ContainerRequestContext request,
                    @PathParam("appid") String facebookAppId,
                    @PathParam("uid") String facebookUserId,
                    JsonObject fbData) {

        try {
            SubjectContext.login(new FacebookToken(
                "tic-tac-toe",
                facebookUserId,
                new FacebookAccessToken(fbData.getString("accessToken")),
                facebookAppId,
                fbData.getString("signedRequest", null)));
        } catch (LoginException e) {
            throw new AuthenticationException("Invalid facebook access", e, request);
        }

        return me();
    }

    @GET
    @Path("me")
    @Authenticated("tic-tac-toe")
    @PermitAll
    @Produces("application/json; charset=utf-8")
    public Map me() {
        Subject subject = SubjectContext.getSubject("tic-tac-toe", false);
        String guestId = subject.getPrincipal().getName();
        Map gamer = db.users.findOne([_id: guestId])
        return gamer
    }

}
