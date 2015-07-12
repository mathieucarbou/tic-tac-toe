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
import javax.json.JsonArrayBuilder
import javax.json.JsonObject
import javax.ws.rs.*
import java.time.Clock
import java.time.ZonedDateTime
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@Path("/api/games")
@Jsend
class GameResource {

    @Inject DB db
    @Inject Pusher pusher
    @Inject Clock clock

    @GET
    @Path("walloffame")
    @Authenticated("tic-tac-toe")
    @RolesAllowed("gamer")
    @Produces("application/json; charset=utf-8")
    Stream<Map> wallOfFame(@QueryParam("n") int n) {
        return db.users.find([
            wins: [$exists: true, $gte: 1]
        ], [
            id: 1,
            fb_id: 1,
            name: 1,
            wins: 1,
            losts: 1,
            draws: 1
        ], [
            wins: -1
        ], new Function<Map, Map>() {
            @Override
            Map apply(Map u) {
                u.wins = u.wins ?: 0
                u.losts = u.losts ?: 0
                u.draws = u.draws ?: 0
                return u
            }
        }, n > 100 ? 100 : n < 0 ? 0 : n, 0)
    }

    @POST
    @Path("challenge/{uid}")
    @Authenticated("tic-tac-toe")
    @RolesAllowed("gamer")
    @Produces("application/json; charset=utf-8")
    void challenge(@PathParam("uid") String opponentId) {
        String myId = SubjectContext.getSubject('tic-tac-toe').principal.name

        // find players
        List<Map> users = db.users.find([id: [$in: [myId, opponentId]]]).collect(Collectors.toList())
        Map me = users.find { it.id == myId }
        Map opponent = users.find { it.id == opponentId }
        if (!me || !opponent) {
            throw new BadRequestException()
        }

        // be sure there are available
        JsonObject target = pusher.getChannel("presence-gamer-room").getMembers().getValuesAs(JsonObject).find { it.getString("id") == opponentId }
        if (!target) {
            throw new BadRequestException("Member " + opponentId + " unavailable")
        }

        // cleanup all non-finished games for the current user
        // this task would be better put in an external worker but for this PoC it is set there.
        db.games.remove([player: myId, finished: false, updatedDate: [$lt: ZonedDateTime.now(clock).minusHours(2)]])

        // create game
        String startWith = [myId, opponentId].get(Math.abs(new Random().nextInt()) % 2)
        String game_id = db.games.insert([
            player: myId,
            opponent: opponentId,
            start: startWith,
            finished: false,
            winner: null,
            loser: null,
            draw: false,
            turn: startWith,
            board: ['', '', '', '', '', '', '', '', ''],
            symbol: [
                X: startWith,
                O: startWith == myId ? opponentId : myId
            ]
        ])

        // send them an event to start the game
        pusher.getChannel("private-gamer-" + myId).publish("game-challenge", Json.createObjectBuilder()
            .add("id", game_id)
            .add("from", myId)
            .add("to", opponentId)
            .add("start", startWith)
            .add("turn", startWith)
            .add("opponent",
            Json.createObjectBuilder()
                .add("id", opponent.id as String)
                .add("name", opponent.name as String)
                .add("fb_id", opponent.fb_id as String)
                .build())
            .build())
        pusher.getChannel("private-gamer-" + opponentId).publish("game-challenge", Json.createObjectBuilder()
            .add("id", game_id)
            .add("from", myId)
            .add("to", opponentId)
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

    @POST
    @Path("{id}/play/{i}")
    @Authenticated("tic-tac-toe")
    @RolesAllowed("gamer")
    @Produces("application/json; charset=utf-8")
    void play(@PathParam("id") String gameId, @PathParam("i") int i) {
        String myId = SubjectContext.getSubject('tic-tac-toe').principal.name
        Map game = db.games.findOne([id: gameId])

        if (!game) throw new NotFoundException("Game not found: " + gameId)
        if (game.finished) throw new BadRequestException("Game is finished: " + gameId)
        if (game.turn != myId) throw new BadRequestException("Not your turn for game: " + gameId)
        if (i < 0 || i > 8) throw new BadRequestException("Bad shot in game " + gameId + ": " + i)
        if (game.board[i] != '') throw new BadRequestException("Bad shot in game " + gameId + ": " + i)

        game.board[i] = game.start == myId ? 'X' : 'O'
        String symbol = findWinner(game.board as List<String>)
        String otherPlayer = myId == game.player ? game.opponent : game.player

        if (symbol != null) {
            // determine and of game
            String winner = game.symbol[symbol] ?: null
            String loser = game.symbol[symbol == 'X' ? 'O' : symbol == 'O' ? 'X' : ''] ?: null
            int n = db.games.update([
                id: gameId,
                finished: false,
                turn: myId
            ], [
                $set: [
                    turn: null,
                    finished: true,
                    board: game.board,
                    winner: winner,
                    loser: loser,
                    draw: symbol == ''
                ]
            ])
            if (n) {
                if (symbol == '') {
                    db.users.update([id: [$in: [game.player, game.opponent]]], [$inc: [draws: 1]], false, true)
                    fireWinner(game, "draw")
                } else {
                    db.users.update([id: winner], [$inc: [wins: 1]])
                    db.users.update([id: loser], [$inc: [losts: 1]])
                    fireWinner(game, winner)
                }
            }

        } else {
            // prepare next turn
            int n = db.games.update([
                id: gameId,
                finished: false,
                turn: myId
            ], [
                $set: [
                    turn: otherPlayer,
                    board: game.board
                ]
            ])
            if (n) {
                JsonArrayBuilder b = Json.createArrayBuilder()
                game.board.each { b.add(it as String) }
                JsonObject turn = Json.createObjectBuilder()
                    .add("id", gameId)
                    .add("prev", myId)
                    .add("next", otherPlayer)
                    .add("board", b)
                    .build()
                pusher.getChannel("private-gamer-$myId").publish("game-turn", turn)
                pusher.getChannel("private-gamer-$otherPlayer").publish("game-turn", turn)
            }
        }
    }

    private void fireWinner(Map game, String winner) {
        JsonArrayBuilder b = Json.createArrayBuilder()
        game.board.each { b.add(it as String) }
        JsonObject data = Json.createObjectBuilder()
            .add("id", game.id as String)
            .add("winner", winner)
            .add("board", b)
            .build()
        pusher.getChannel("private-gamer-$game.player").publish("game-finished", data)
        pusher.getChannel("private-gamer-$game.opponent").publish("game-finished", data)
    }

    // just tries to find which symbol has wom the game
    private static String findWinner(List<String> board) {
        def match = ['XXX', 'OOO']
        // horizontal
        if (board[0] + board[1] + board[2] in match) return board[0]
        if (board[3] + board[4] + board[5] in match) return board[3]
        if (board[6] + board[7] + board[8] in match) return board[6]
        // vertical
        if (board[0] + board[3] + board[6] in match) return board[0]
        if (board[1] + board[4] + board[7] in match) return board[1]
        if (board[2] + board[5] + board[8] in match) return board[2]
        // cross
        if (board[0] + board[4] + board[8] in match) return board[0]
        if (board[2] + board[4] + board[6] in match) return board[2]
        // check draw
        return board.count('') ? null : ''
    }

}
