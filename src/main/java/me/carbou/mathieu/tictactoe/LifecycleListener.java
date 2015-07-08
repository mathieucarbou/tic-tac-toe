package me.carbou.mathieu.tictactoe;

import com.mycila.guice.ext.closeable.CloseableInjector;
import me.carbou.mathieu.tictactoe.db.DB;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class LifecycleListener implements ContainerLifecycleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleListener.class);

    @Inject CloseableInjector closeableInjector;
    @Inject DB db;

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
        db.close();
    }

}
