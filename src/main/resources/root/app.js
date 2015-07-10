/*
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
window.fbAsyncInit = function () {
    FB.init({
        appId: config.fbappid,
        xfbml: true,
        version: 'v2.3',
        cookie: true
    });
    checkLoginState();
};

window.app = {
    user: null
};

function checkLoginState() {
    FB.getLoginStatus(function (response) {
        console.log('checkLoginState()', response);
        if (response.status === 'connected') {
            // Logged into your app and Facebook.
            loadGame(response.authResponse);
        } else if (response.status === 'not_authorized') {
            // The person is logged into Facebook, but not your app.
            $('#login-status').text('Please log into this app.');
            $('#login').removeClass('hidden');
        } else {
            // The person is not logged into Facebook, so we're not sure if
            // they are logged into this app or not.
            $('#login-status').text('Please log into Facebook.');
            $('#login').removeClass('hidden');
        }
    });
}

function loadGame(authResponse) {
    auth(authResponse).done(function () {
        updateWallOfFame();
        startAsync();
    });
}

function auth(authResponse) {
    console.log('connect()', authResponse);
    return $.ajax('/api/users/auth/facebook/' + config.fbappid + '/' + authResponse.userID, {
        type: "POST",
        dataType: 'json',
        contentType: 'application/json',
        data: JSON.stringify(authResponse)
    }).done(function (body) {
        app.user = body.data;
        var u = app.user;
        $('#login').addClass('hidden');
        $('#user').append('<h2 class="fb_avatar"><img class="circle" src="//graph.facebook.com/' + u.fb_id + '/picture?width=32&height=32" alt="' + u.name + '">&nbsp;&nbsp;<span id="user-name">' + u.name + ' : WINS: ' + u.wins + ', LOSTS: ' + u.losts + ', DRAWS: ' + u.draws + '</span></h2>').removeClass('hidden');
        $('#app-content').removeClass('hidden');
    });
}

function updateWallOfFame() {
    console.log('updateWallOfFame()');
    return $.ajax('/api/games/walloffame?n=5', {
        type: "GET",
        dataType: 'json'
    }).done(function (body) {
        if (body.data.length) {
            var table = $('#walloffame table tbody');
            table.empty();
            _.each(body.data, function (u, i) {
                table.append('<tr>' +
                    '<td>' + (i + 1) + '</td>' +
                    '<td class="fb_avatar"><img class="circle" src="//graph.facebook.com/' + u.fb_id + '/picture?width=32&height=32" alt="' + u.name + '">&nbsp;&nbsp;<span id="user-name">' + u.name + '</span></td>' +
                    '<td>' + u.wins + '</td>' +
                    '<td>' + u.losts + '</td>' +
                    '<td>' + u.draws + '</td>' +
                    '</tr>');
            });
        }
        $('#walloffame').removeClass('hidden');
    });
}

function template_user(u) {
    return '<li user-id="' + u.id + '" class="fb_avatar"><img class="circle" src="//graph.facebook.com/' + u.fb_id + '/picture?width=32&height=32" alt="' + u.name + '">&nbsp;&nbsp;<span id="user-name">' + u.name + ' on ' + u.device + ' (wins: ' + u.wins + ', losts: ' + u.losts + ', draws: ' + u.draws + ')</span>&nbsp;&nbsp;<a class="challenge" href="#">CHALLENGE !</a></li>';
}

function startAsync() {
    console.log('startAsync');

    app.pusher = new Pusher(config.pusherkey, {
        authEndpoint: '/api/users/auth/pusher'
    });

    // connected handler
    app.pusher.connection.bind('connected', function () {
        console.log('connected');
    });

    // error handling - connection limit is enforced on sandbox plans
    app.pusher.connection.bind('error', function (err) {
        if (err.data.code === 4004) {
            console.error('Pusher connection limit reached', err);
        }
    });

    setAvailable();

    var gamerChannel = app.pusher.subscribe("private-gamer-" + app.user.id);

    // subscribe to game start event
    gamerChannel.bind('game-challenge', function (message) {
        console.log('game-challenge', message);
        setUnavailable();
        app.game = {
            id: message.id,
            turn: message.turn,
            opponent: message.opponent
        };
        $('#soundFx')[0].play();
        var msg = $('#ttt .message').empty();
        if (message.from == app.user.id) {
            msg.append('<p class="fb_avatar">You challenged ' + message.opponent.name + '&nbsp;&nbsp;<img class="circle" src="//graph.facebook.com/' + message.opponent.fb_id + '/picture?width=32&height=32" alt="' + message.opponent.name + '"></p>');
        } else {
            msg.append('<p class="fb_avatar">You have been challenged by ' + message.opponent.name + '&nbsp;&nbsp;<img class="circle" src="//graph.facebook.com/' + message.opponent.fb_id + '/picture?width=32&height=32" alt="' + message.opponent.name + '"></span>');
        }
        if (message.start == app.user.id) {
            msg.append('<p>You start! You have the cross, and your opponent have the circle!</p>');
            $('#ttt .turn').empty().append('<p>This is your turn!</p>');
            $('#ttt table').css({cursor: 'move'});
        } else {
            msg.append('<p>' + message.opponent.name + ' starts! Your opponent have the cross and you have the circle!</p>');
            $('#ttt .turn').empty().append('<p>' + message.opponent.name + ' is playing...</p>');
            $('#ttt table').css({cursor: 'wait'});
        }
        $('#ttt td').empty();
        $('#ttt').removeClass('hidden');
        location.href = "#ttt";
    });

    // subscribe to game turn event
    gamerChannel.bind('game-turn', function (message) {
        console.log('game-turn', message);
        if (message.id == app.game.id) {
            app.game.turn = message.next;
            setBoard(message.board);
            if (app.game.turn == app.user.id) {
                $('#ttt .turn').empty().append('<p>This is your turn!</p>');
            } else {
                $('#ttt .turn').empty().append('<p>' + app.game.opponent.name + ' is playing...</p>');
            }
            $('#soundFx')[0].play();
        }
    });

    // subscribe to game end event
    gamerChannel.bind('game-finished', function (message) {
        console.log('game-finished', message);
        if (message.id == app.game.id) {
            $('#soundFx')[0].play();
            setBoard(message.board);
            if (message.winner == 'draw') {
                alert('Draw!');
            } else if (message.winner == app.user.id) {
                alert('You win!');
            } else {
                alert('You lost!');
            }
            $('#ttt').addClass('hidden');
            setAvailable();
            updateWallOfFame();
        }
    });
}

function setBoard(board) {
    var td = $('#ttt td').toArray();
    for (var i = 0; i < 9; i++) {
        $(td[i]).text(board[i]);
    }
}

function setAvailable() {
    // subscribe to presence channel
    console.log('subscribe()', 'presence-gamer-room');
    app.presenceChannel = app.pusher.subscribe('presence-gamer-room');

    app.presenceChannel.bind('pusher:subscription_succeeded', function () {
        var users = _.sortBy(_.filter(_.values(app.presenceChannel.members.members), function (u) {
            return app.user.id != u.id;
        }), 'name');
        console.log('presence-gamer-room', users);
        var ol = $('#gamer-room ol');
        ol.empty();
        if (users.length) {
            _.each(users, function (u) {
                ol.append(template_user(u));
            });
        } else {
            ol.append('<li empty>No player available! Please Wait for some...</li>');
        }
        $('#gamer-room').removeClass('hidden');
    });

    app.presenceChannel.bind('pusher:member_added', function (member) {
        if (member.info.id != app.user.id) {
            console.log('user joined', member.info);
            $('#gamer-room ol')
                .find('li[empty]').remove()
                .end()
                .append(template_user(member.info));
            $('#soundFx')[0].play();
        }
    });

    app.presenceChannel.bind('pusher:member_removed', function (member) {
        var u = member.info, ol = $('#gamer-room ol');
        console.log('user left', u);
        ol.find('li[user-id=' + u.id + ']').remove();
        if (!ol.children().length) {
            ol.append('<li empty>No player available! Please Wait for some...</li>');
        }
    });
}

function setUnavailable() {
    app.pusher.unsubscribe('presence-gamer-room');
    $('#gamer-room').addClass('hidden');
}

// on dom ready

$(function () {
    $('#gamer-room').on('click', 'a.challenge', function (e) {
        e.preventDefault();
        var targetId = $(this).closest('li').attr('user-id');
        console.log('challenging', targetId);
        $.ajax('/api/games/challenge/' + targetId, {type: "POST"});
    });
    $('#ttt table').on('click', 'td:empty', function (e) {
        if (app.game.turn == app.user.id) {
            var i = parseInt($(this).attr('id').substring(5), 10);
            $.ajax('/api/games/' + app.game.id + '/play/' + i, {type: "POST"});
        }
    });
});
