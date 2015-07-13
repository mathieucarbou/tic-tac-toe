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
package me.carbou.mathieu.tictactoe.security

import com.guestful.client.facebook.FacebookAccessToken
import com.guestful.client.facebook.FacebookClient
import com.guestful.jaxrs.security.realm.Account
import com.guestful.jaxrs.security.realm.AccountRepository
import com.guestful.jaxrs.security.subject.SubjectContext
import com.guestful.jaxrs.security.token.AuthenticationToken
import com.guestful.jaxrs.security.token.FacebookToken
import me.carbou.mathieu.tictactoe.db.DB

import javax.inject.Inject
import javax.json.JsonObject
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.HttpHeaders
import java.security.Principal
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@javax.inject.Singleton
class MongoAccountRepository implements AccountRepository {

    @Inject DB db
    @Inject FacebookClient facebookClient

    @Override
    Account findAccount(AuthenticationToken token) {
        switch (token.system) {

            case 'tic-tac-toe':
                switch (token) {

                    case FacebookToken:
                        FacebookToken facebookToken = (FacebookToken) token
                        String fb_id = facebookToken.me.getString("id")
                        db.users.update([
                            fb_id: fb_id
                        ], [
                            $setOnInsert: [
                                roles: ['gamer']
                            ],
                            $set: [
                                firstName: facebookToken.me.getString("first_name", ""),
                                lastName: facebookToken.me.getString("last_name", ""),
                                name: facebookToken.me.getString("name", ""),
                                email: facebookToken.me.getString("email", null),
                                gender: facebookToken.me.getString("gender", null),
                                timeZone: facebookToken.me.containsKey("timezone") ? ZoneId.ofOffset("UTC", ZoneOffset.ofHours(facebookToken.me.getInt("timezone"))) : null,
                                locale: facebookToken.me.getString("locale", getBestLocale(SubjectContext.getSubject(token.system).getRequest()).toString()),
                                fb_friends: facebookClient.getFriends((FacebookAccessToken) facebookToken.readCredentials()).getValuesAs(JsonObject.class).collect { f ->
                                    [
                                        facebookId: f.getString('id'),
                                        name: f.getString('name', "")
                                    ]
                                }
                            ]
                        ], true) // upsert
                        Map user = db.users.findOne([fb_id: fb_id], [id: 1, roles: 1])
                        return toAccount(user)
                        break

                    default:
                        throw new UnsupportedOperationException(token.toString())
                }
                break

            default:
                throw new UnsupportedOperationException(token.toString())
        }
    }

    @Override
    Account findAccount(String system, Principal principal) {
        switch (system) {

            case 'tic-tac-toe':
                Map user = db.users.findOne([id: principal.name], [id: 1, roles: 1])
                return toAccount(user)
                break

            default:
                throw new UnsupportedOperationException(system + ":" + principal)
        }
    }

    private static Account toAccount(Map user) {
        if (user == null) return null
        Account account = new Account(user.id as String)
        account.locked = false
        account.addRoles(user.roles ?: [])
        return account
    }

    private static Locale getBestLocale(ContainerRequestContext request) {
        String header = request.getHeaderString(HttpHeaders.ACCEPT_LANGUAGE);
        if (header == null) {
            header = Locale.US.toLanguageTag();
        }
        // IE10 can send: Accept-Language: en-ca,en-us;q=05
        header = header.replaceAll("(q=\\d)(\\d)", "\$1.\$2");
        List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(header);
        return Locale.forLanguageTag(ranges.get(0).getRange());
    }

}
