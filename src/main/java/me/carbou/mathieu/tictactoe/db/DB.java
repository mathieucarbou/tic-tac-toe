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
