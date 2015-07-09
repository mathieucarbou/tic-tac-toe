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
        $('#user').append('<h2 class="fb_avatar"><img class="circle" src="//graph.facebook.com/' + u.fb_id + '/picture?width=32&height=32" alt="' + u.name + '">&nbsp;&nbsp;<span id="user-name">' + u.name + ' : WINS: ' + u.wins + ', LOSTS: ' + u.losts + '</span></h2>').removeClass('hidden');
    });
}

function updateWallOfFame() {
    console.log('updateWallOfFame()');
    return $.ajax('/api/games/walloffame', {
        type: "GET",
        dataType: 'json'
    }).done(function (body) {
        if (body.data.length) {
            var ol = $('#walloffame ol');
            ol.empty();
            _.each(body.data, function (u) {
                ol.append('<li class="fb_avatar"><img class="circle" src="//graph.facebook.com/' + u.fb_id + '/picture?width=32&height=32" alt="' + u.name + '">&nbsp;&nbsp;<span id="user-name">' + u.name + ' : WINS: ' + u.wins + ', LOSTS: ' + u.losts + '</span></li>');
            });
        }
        $('#walloffame').removeClass('hidden');
    });
}

function template_user(u) {
    return '<li user-id="' + u.id + '" class="fb_avatar"><img class="circle" src="//graph.facebook.com/' + u.fb_id + '/picture?width=32&height=32" alt="' + u.name + '">&nbsp;&nbsp;<span id="user-name">' + u.name + ' on ' + u.device + ' (wins: ' + u.wins + ', losts: ' + u.losts + ')</span>&nbsp;&nbsp;<a class="challenge" href="#">CHALLENGE !</a></li>';
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

    // subscribe to resto events
    var gamerChannel = app.pusher.subscribe("private-gamer-" + app.user.id);
    gamerChannel.bind('challenge', function (message) {
        console.log('challenge', message);
        setUnavailable();
        setTimeout(function () {
            console.log('challenge end');
            setAvailable();
        }, 10000);
    });
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
});
