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

import com.mycila.guice.ext.closeable.CloseableInjector;
import me.carbou.mathieu.tictactoe.db.DB;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import javax.inject.Inject;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class LifecycleListener implements ContainerLifecycleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleListener.class);

    @Inject CloseableInjector closeableInjector;
    @Inject DB db;
    @Inject JedisPool jedisPool;

    @Override
    public void onStartup(Container container) {
        LOGGER.info("onStartup() Version=" + Env.VERSION + ", PORT=" + Env.PORT + ", PID=" + Env.PID + ", DYNO=" + Env.DYNO + ", ENV=" + Env.NAME);
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
        closeableInjector.close();
        jedisPool.close();
        db.close();
    }

}
