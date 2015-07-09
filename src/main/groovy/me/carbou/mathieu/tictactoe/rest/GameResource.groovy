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

import com.guestful.client.pusher.Pusher
import com.guestful.jaxrs.filter.jsend.Jsend
import com.guestful.jaxrs.security.annotation.Authenticated
import com.guestful.jaxrs.security.subject.SubjectContext
import me.carbou.mathieu.tictactoe.db.DB

import javax.annotation.security.RolesAllowed
import javax.inject.Inject
import javax.json.Json
import javax.json.JsonObject
import javax.ws.rs.*
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@Path("/api/games")
@Jsend
public class GameResource {

    @Inject DB db
    @Inject Pusher pusher

    @GET
    @Path("walloffame")
    @Authenticated("tic-tac-toe")
    @RolesAllowed("gamer")
    @Produces("application/json; charset=utf-8")
    public Stream<Map> wallOfFame() {
        return db.users.find([
            wins: [$exists: true, $gte: 1]
        ], [
            id: 1,
            fb_id: 1,
            name: 1,
            wins: 1,
            losts: 1
        ], [
            wins: -1
        ], Function.identity(), 10, 0)
    }

    @POST
    @Path("challenge/{uid}")
    @Authenticated("tic-tac-toe")
    @RolesAllowed("gamer")
    @Produces("application/json; charset=utf-8")
    public void challenge(@PathParam("uid") String targetId) {
        String myId = SubjectContext.getSubject('tic-tac-toe').principal.name

        // find players
        List<Map> users = db.users.find([id: [$in: [myId, targetId]]]).collect(Collectors.toList())
        Map me = users.find { it.id == myId }
        Map opponent = users.find { it.id == targetId }
        if(!me || !opponent) {
            throw new BadRequestException()
        }

        // be sure there are available
        JsonObject target = pusher.getChannel("presence-gamer-room").getMembers().getValuesAs(JsonObject).find { it.getString("id") == targetId }
        if (!target) {
            throw new BadRequestException("Member " + targetId + " unavailable")
        }

        // create game
        String startWith = [myId, targetId].get(Math.abs(new Random().nextInt()) % 2)
        String game_id = db.games.insert([
            player: myId,
            opponent: targetId,
            start: startWith,
            winner: null,
            loser: null,
            draw: false,
            turn: startWith,
            moves: []
        ])

        // send them an event to start the game
        pusher.getChannel("private-gamer-" + myId).publish("challenge", Json.createObjectBuilder()
            .add("game_id", game_id)
            .add("start", startWith)
            .add("turn", startWith)
            .add("opponent",
            Json.createObjectBuilder()
                .add("id", opponent.id as String)
                .add("name", opponent.name as String)
                .add("fb_id", opponent.fb_id as String)
                .build())
            .build())
        pusher.getChannel("private-gamer-" + targetId).publish("challenge", Json.createObjectBuilder()
            .add("game_id", game_id)
            .add("start", startWith)
            .add("turn", startWith)
            .add("opponent",
            Json.createObjectBuilder()
                .add("id", me.id as String)
                .add("name", me.name as String)
                .add("fb_id", me.fb_id as String)
                .build())
            .build())
    }

}
