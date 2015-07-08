package me.carbou.mathieu.tictactoe;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class Env {

    public static final Object MODULE = "tic-tac-toe";

    public static final String ENV_LOCAL = "local";
    public static final String ENV_PROD = "production";
    public static final String NAME = env("ENV", ENV_LOCAL, Arrays.asList(ENV_LOCAL, ENV_PROD));

    public static final int PORT = env("PORT", 8080);
    public static final long PID = Long.parseLong(System.getProperty("pid", "0"));
    public static final int WORKERS = env("WORKERS", 50);
    public static final String DYNO = env("DYNO", "local");
    public static final String VERSION = getVersion();

    public static final String MONGOLAB_URI = env("MONGOLAB_URI", "mongodb://127.0.0.1:27017/tic-tac-toe");
    public static final String PUSHER_URL = env("PUSHER_URL", "");
    public static final String FACEBOOK_APP_ID = env("FACEBOOK_APP_ID", "");
    public static final String FACEBOOK_APP_SECRET = env("FACEBOOK_APP_SECRET", "");
    public static final String MANDRILL_USERNAME = env("MANDRILL_USERNAME", (String) null);
    public static final String MANDRILL_APIKEY = env("MANDRILL_APIKEY", (String) null);

    public static boolean isLocal() {
        return NAME.equals(ENV_LOCAL);
    }

    public static boolean isProduction() {
        return NAME.equals(ENV_PROD);
    }

    private static String env(String name, String def, Collection<String> allowed) {
        String e = System.getenv(name);
        if (e == null) e = def;
        if (!allowed.contains(e)) throw new IllegalArgumentException("Illegal env: " + name);
        return e;
    }

    private static String env(String name, String def) {
        String e = System.getenv(name);
        if (e == null) return def;
        return e;
    }

    private static int env(String name, int def) {
        String e = System.getenv(name);
        if (e == null) return def;
        return Integer.parseInt(e);
    }

    private static boolean env(String name, boolean def) {
        String e = System.getenv(name);
        if (e == null) return def;
        return Boolean.parseBoolean(e);
    }

    private static String getVersion() {
        try {
            Properties p = new Properties();
            p.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/maven/me.carbou.mathieu/tictactoe/pom.properties"));
            return p.getProperty("version");
        } catch (Exception e) {
            return "<dev>";
        }
    }

}
