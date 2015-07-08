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
package me.carbou.mathieu

import com.guestful.client.facebook.FacebookAccessToken
import com.guestful.client.facebook.FacebookClient
import com.guestful.jaxrs.security.realm.Account
import com.guestful.jaxrs.security.realm.AccountRepository
import com.guestful.jaxrs.security.subject.SubjectContext
import com.guestful.jaxrs.security.token.AuthenticationToken
import com.guestful.jaxrs.security.token.FacebookToken
import me.carbou.mathieu.tictactoe.Utils
import me.carbou.mathieu.tictactoe.db.DB

import javax.inject.Inject
import javax.json.JsonObject
import java.security.Principal

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@javax.inject.Singleton
class MongoAccountRepository implements AccountRepository {

    @Inject DB db
    @Inject FacebookClient facebookClient

    @Override
    Account findAccount(AuthenticationToken token) {
        if (token.token) {
            switch (token.system) {

                case 'tic-tac-toe':
                    switch (token) {

                        case FacebookToken:
                            FacebookToken facebookToken = (FacebookToken) token
                            String fb_firstName = facebookToken.me.getString("first_name", "")
                            String fb_lastName = facebookToken.me.getString("last_name", "")
                            String fb_id = facebookToken.me.getString("id")
                            db.users.update([
                                fb_id: fb_id
                            ], [
                                $setOnInsert: [
                                    roles: ['gamer']
                                ],
                                $set: [
                                    firstName: fb_firstName,
                                    lastName: fb_lastName,
                                    name: (fb_firstName + " " + fb_lastName).trim(),
                                    email: facebookToken.me.getString("email", null),
                                    gender: facebookToken.me.getString("gender", null),
                                    locale: facebookToken.me.getString("locale", Utils.getBestLocale(SubjectContext.getSubject(token.system).getRequest()).toString()),
                                    fb_friends: facebookClient.getFriends((FacebookAccessToken) facebookToken.readCredentials()).getValuesAs(JsonObject.class).collect { f ->
                                        [
                                            facebookId: f.getString('id'),
                                            name: f.getString('name', "")
                                        ]
                                    }
                                ]
                            ], true) // upsert
                            Map user = db.users.findOne([fb_id: fb_id], [_id: 1, roles: 1])
                            return toAccount(user)
                            break

                        default:
                            throw new UnsupportedOperationException(token.toString())
                    }
                    break

                default:
                    throw new UnsupportedOperationException(token.toString())
            }
        } else {
            return null
        }
    }

    @Override
    Account findAccount(String system, Principal principal) {
        switch (system) {

            case 'tic-tac-toe':
                Map user = db.users.findOne([_id: principal.name], [_id: 1, roles: 1])
                return toAccount(user)
                break

            default:
                throw new UnsupportedOperationException(system + ":" + principal)
        }
    }

    private static Account toAccount(Map guest) {
        if (guest == null) return null
        Account account = new Account(guest.id as String)
        account.locked = false
        account.addRoles(guest.roles ?: [])
        return account
    }

}
