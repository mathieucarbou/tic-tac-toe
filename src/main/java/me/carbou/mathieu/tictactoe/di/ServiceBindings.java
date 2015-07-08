package me.carbou.mathieu.tictactoe.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.guestful.client.facebook.FacebookClient;
import com.guestful.client.facebook.FacebookConfig;
import com.guestful.client.mandrill.MandrillClient;
import com.guestful.client.mandrill.MandrillConfig;
import com.guestful.client.pusher.Pusher;
import com.guestful.jaxrs.json.JsonProvider;
import com.guestful.json.JsonMapper;
import com.guestful.json.groovy.GroovyJsonMapper;
import com.guestful.jsr310.mongo.MongoJsr310;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import groovy.lang.GString;
import me.carbou.mathieu.tictactoe.Env;
import me.carbou.mathieu.tictactoe.db.DB;
import org.bson.BSON;
import org.glassfish.jersey.jsonp.JsonProcessingFeature;

import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Locale;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class ServiceBindings extends AbstractModule {

    @Override
    protected void configure() {
        bind(JsonMapper.class).to(GroovyJsonMapper.class).in(Singleton.class);
        bind(Clock.class).toInstance(Clock.systemUTC()); // enabled override the whole system clock
    }

    @Provides
    @Singleton
    public Client client(JsonMapper jsonMapper) {
        return ClientBuilder.newBuilder()
            .build()
            .register(JsonProcessingFeature.class) // javax.json support
            .register(new JsonProvider(jsonMapper)); // groovy support
    }

    @Provides
    @Singleton
    public Pusher pusherClient(Client restClient) {
        return new Pusher(restClient, Env.PUSHER_URL);
    }

    @Provides
    @Singleton
    MandrillClient guestfulMandrillClient(Client restClient) {
        return new MandrillClient(restClient, new MandrillConfig()
            .setAsync(true)
            .setPreserveRecipients(false)
            .setTrackOpens(true)
            .setTags(Env.isProduction() ? Collections.emptyList() : Collections.singletonList("testing"))
            .setApiKey(Env.MANDRILL_APIKEY));
    }

    @Provides
    @Singleton
    FacebookClient facebookClient(Client restClient) {
        return new FacebookClient(restClient, new FacebookConfig()
            .setAppID(Env.FACEBOOK_APP_ID)
            .setAppSecret(Env.FACEBOOK_APP_SECRET));
    }

    @Provides
    @Singleton
    public DB db(Clock clock) throws UnknownHostException {
        MongoClientURI mongoClientURI = new MongoClientURI(Env.MONGOLAB_URI);
        MongoClient mongoClient = new MongoClient(mongoClientURI);
        MongoJsr310.addJsr310EncodingHook();
        BSON.addEncodingHook(GString.class, o -> o instanceof GString ? o.toString() : o);
        BSON.addEncodingHook(ZoneId.class, o -> o instanceof ZoneId ? ((ZoneId) o).getId() : o);
        BSON.addEncodingHook(Enum.class, o -> o instanceof Enum ? ((Enum) o).name() : o);
        BSON.addEncodingHook(Locale.class, o -> o instanceof Locale ? o.toString() : o);
        BSON.addEncodingHook(BigDecimal.class, o -> o instanceof BigDecimal ? ((BigDecimal) o).doubleValue() : o);
        BSON.addEncodingHook(BigInteger.class, o -> o instanceof BigInteger ? ((BigInteger) o).longValue() : o);
        return new DB(mongoClient.getDB(mongoClientURI.getDatabase()), clock);
    }

}
