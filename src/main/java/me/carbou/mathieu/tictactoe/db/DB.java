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
package me.carbou.mathieu.tictactoe.db;

import java.time.Clock;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class DB {

    public static final int NO_LIMIT = 0;

    final com.mongodb.DB mongoDB;
    final Clock clock;

    public final DBCollection users;
    public final DBCollection games;

    public DB(com.mongodb.DB mongoDB, Clock clock) {
        this.mongoDB = mongoDB;
        this.clock = clock;
        this.users = new DBCollection(this, "users");
        this.games = new DBCollection(this, "games");
    }

    public void close() {
        mongoDB.getMongo().close();
    }

}
