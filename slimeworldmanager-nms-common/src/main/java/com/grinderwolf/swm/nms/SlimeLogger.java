package com.grinderwolf.swm.nms;

import java.util.logging.*;

public class SlimeLogger {

    private static final Logger LOGGER = Logger.getLogger("ASWM-INTERNAL");

    public static boolean DEBUG = false;

    public static void debug(String message) {
        if (DEBUG) {
          LOGGER.log(Level.WARNING, message);
        }
    }
}
