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
import com.guestful.client.mandrill.MandrillClient
import com.guestful.client.pusher.Pusher
import com.guestful.client.pusher.PusherPresence
import com.guestful.jaxrs.filter.jsend.Jsend
import com.guestful.jaxrs.security.AuthenticationException
import com.guestful.jaxrs.security.annotation.Authenticated
import com.guestful.jaxrs.security.subject.Subject
import com.guestful.jaxrs.security.subject.SubjectContext
import com.guestful.jaxrs.security.token.FacebookToken
import me.carbou.mathieu.tictactoe.db.DB

import javax.annotation.security.PermitAll
import javax.annotation.security.RolesAllowed
import javax.inject.Inject
import javax.json.Json
import javax.json.JsonObject
import javax.security.auth.login.LoginException
import javax.ws.rs.*
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@Path("/api/users")
class UserResource {

    @Inject DB db
    @Inject Pusher pusher
    @Inject MandrillClient mandrillClient

    @POST
    @Path("auth/facebook/{appid}/{uid}")
    @Consumes("application/json; charset=utf-8")
    @Produces("application/json; charset=utf-8")
    @Jsend
    Map auth(@Context ContainerRequestContext request,
                    @PathParam("appid") String facebookAppId,
                    @PathParam("uid") String facebookUserId,
                    Map authResponse) {

        boolean existing = db.users.exist([fb_id: facebookUserId])

        try {
            SubjectContext.login(new FacebookToken(
                "tic-tac-toe",
                facebookUserId,
                new FacebookAccessToken(authResponse.accessToken as String, authResponse.expiresIn as int),
                facebookAppId,
                authResponse.signedRequest as String));
        } catch (LoginException e) {
            throw new AuthenticationException("Invalid facebook access", e, request);
        }

        Map me = me()

        if (!existing && me.email) {
            // should be done by a worker (i.e. Iron Worker) and triggered by a message queue
            mandrillClient.getTemplate("welcome").createMandrillMessage()
                .set('FIRSTNAME', me.firstName as String)
                .to(me.name as String, me.email as String)
                .send()
        }

        return me
    }

    @GET
    @Path("me")
    @Authenticated("tic-tac-toe")
    @PermitAll
    @Produces("application/json; charset=utf-8")
    @Jsend
    Map me() {
        Subject subject = SubjectContext.getSubject("tic-tac-toe", false);
        String guestId = subject.getPrincipal().getName();
        Map gamer = db.users.findOne([id: guestId])
        return [
            id: gamer.id,
            name: gamer.name,
            firstName: gamer.firstName,
            lastName: gamer.lastName,
            email: gamer.email,
            fb_id: gamer.fb_id,
            wins: gamer.wins ?: 0,
            losts: gamer.losts ?: 0,
            draws: gamer.draws ?: 0
        ]
    }

    @POST
    @Path("auth/pusher")
    @Authenticated("tic-tac-toe")
    @RolesAllowed("gamer")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json; charset=utf-8")
    JsonObject auth(@FormParam('socket_id') String socket_id,
                    @FormParam('channel_name') String channel_name,
                    @HeaderParam(HttpHeaders.USER_AGENT) String userAgent) {

        String uid = SubjectContext.getSubject('tic-tac-toe').principal.name
        userAgent = userAgent ?: ""
        String device = ['Android', 'webOS', 'iPhone', 'iPad', 'iPod', 'BlackBerry', 'IEMobile', 'Opera Mini', 'Windows', 'IOS', 'Mac'].find { userAgent.indexOf(it) != -1 } ?: 'Unknown'

        switch (channel_name) {
            case 'presence-gamer-room':
                Map gamer = db.users.findOne([id: uid])
                PusherPresence presence = new PusherPresence(
                    gamer.id as String,
                    Json.createObjectBuilder()
                        .add("id", gamer.id as String)
                        .add("fb_id", gamer.fb_id as String)
                        .add("name", gamer.name as String)
                        .add("losts", gamer.losts ?: 0)
                        .add("wins", gamer.wins ?: 0)
                        .add("draws", gamer.draws ?: 0)
                        .add("device", device)
                        .build())
                return pusher.authenticate(socket_id, channel_name, presence).toJson()
            case ('private-gamer-' + uid):
                return pusher.authenticate(socket_id, channel_name).toJson()
            default:
                throw new NotAuthorizedException(channel_name);

        }
    }

}
