package com.playtheatria.jliii.magicpond.util;

import com.playtheatria.jliii.magicpond.managers.ConfigManager;

import java.util.logging.Logger;

/**
 * Thin wrapper over the plugin {@link Logger} whose {@link #debug(String)} output is gated
 * behind the config's {@code debug} flag (read live from the {@link ConfigManager}).
 * <p>
 * Inject this wherever debug logging is needed instead of reaching back into the plugin.
 */
public class CustomLogger {

    private final Logger logger;
    private final ConfigManager configManager;

    public CustomLogger(Logger logger, ConfigManager configManager) {
        this.logger = logger;
        this.configManager = configManager;
    }

    /** Logs at INFO with a {@code [debug]} prefix, only when {@code debug} is enabled in config.yml. */
    public void debug(String message) {
        if (configManager.debug()) {
            logger.info("[debug] " + message);
        }
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warning(String message) {
        logger.warning(message);
    }
}
