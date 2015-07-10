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
package me.carbou.mathieu.tictactoe;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.guestful.client.facebook.FacebookClient;
import com.guestful.client.facebook.FacebookConfig;
import com.guestful.client.mandrill.MandrillClient;
import com.guestful.client.mandrill.MandrillConfig;
import com.guestful.client.pusher.Pusher;
import com.guestful.jaxrs.json.JsonProvider;
import com.guestful.jaxrs.security.DefaultSecurityService;
import com.guestful.jaxrs.security.SecurityService;
import com.guestful.jaxrs.security.realm.*;
import com.guestful.jaxrs.security.session.JedisJsonSessionRepository;
import com.guestful.jaxrs.security.session.SessionConfiguration;
import com.guestful.jaxrs.security.session.SessionConfigurations;
import com.guestful.jaxrs.security.session.SessionRepository;
import com.guestful.json.JsonMapper;
import com.guestful.json.groovy.GroovyJsonMapper;
import com.guestful.jsr310.groovy.GroovyJsr310;
import com.guestful.jsr310.mongo.MongoJsr310;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import groovy.lang.GString;
import me.carbou.mathieu.MongoAccountRepository;
import me.carbou.mathieu.tictactoe.db.DB;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.bson.BSON;
import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;

import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Locale;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class ServiceBindings extends AbstractModule {

    @Override
    protected void configure() {
        bind(Clock.class).toInstance(Clock.systemUTC()); // enabled override the whole system clock

        // bind security service and the way of getting a connected UserContext
        bind(SecurityService.class).to(DefaultSecurityService.class);
        bind(AccountRepository.class).to(MongoAccountRepository.class);
        bind(CredentialsMatcher.class).toInstance(new HashedCredentialsMatcher(3));
        bind(SessionConfigurations.class).toInstance(new SessionConfigurations().add("tic-tac-toe", new SessionConfiguration()
            .setCheckOrigin(false)
            .setCheckUserAgent(false)
            .setMaxAge((int) Duration.ofDays(30).getSeconds())
            .setCookieName(Env.isProduction() ? "id" : "id-" + Env.NAME)
            .setCookiePath("/")
            .setCookieDomain(Env.isLocal() ? null : ".carbou.me")));
    }

    @Provides
    @javax.inject.Singleton
    JedisPool jedisPool() {
        URI connectionURI = URI.create(Env.REDISCLOUD_URL);
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMinIdle(0);
        config.setMaxIdle(5);
        config.setMaxTotal(30);
        String password = connectionURI.getUserInfo().split(":", 2)[1];
        return new JedisPool(config, connectionURI.getHost(), connectionURI.getPort(), Protocol.DEFAULT_TIMEOUT, password);
    }

    @Provides
    @javax.inject.Singleton
    SessionRepository sessionRepository(JedisPool jedisPool, JsonMapper jsonMapper) {
        return new JedisJsonSessionRepository(jedisPool, jsonMapper);
    }

    @Provides
    @javax.inject.Singleton
    Realm realm(HttpCookieRealm httpCookieRealm, LoginPasswordRealm loginPasswordRealm, PassthroughRealm passthroughRealm, FacebookRealm facebookRealm) {
        return new FirstMatchingRealm(passthroughRealm, loginPasswordRealm, httpCookieRealm, facebookRealm);
    }

    @Provides
    @Singleton
    JsonMapper jsonMapper() {
        GroovyJsonMapper groovyJsonMapper = new GroovyJsonMapper();
        GroovyJsr310.addJsr310EncodingHook(groovyJsonMapper.getSerializer());
        return groovyJsonMapper;
    }

    @Provides
    @Singleton
    Client client(JsonMapper jsonMapper) {
        return ClientBuilder.newBuilder()
            .build()
            .register(JsonProcessingFeature.class) // javax.json support
            .register(new JsonProvider(jsonMapper)); // groovy support
    }

    @Provides
    @Singleton
    Pusher pusherClient(Client restClient) {
        return new Pusher(restClient, Env.PUSHER_URL);
    }

    @Provides
    @Singleton
    MandrillClient guestfulMandrillClient(Client restClient) {
        return new MandrillClient(restClient, new MandrillConfig()
            .setAsync(true)
            .setPreserveRecipients(false)
            .setTrackOpens(true)
            .setTrackClicks(false)
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
    DB db(Clock clock) throws UnknownHostException {
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
