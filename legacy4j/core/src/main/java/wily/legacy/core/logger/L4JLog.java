package wily.legacy.core.logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wily.legacy.core.ModConstants;

public class L4JLog {
    public static final Logger LOGGER = LogManager.getLogger(ModConstants.MOD_ID);
}
