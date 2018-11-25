package ctbrec.ui.sites;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.sites.ConfigUI;

public abstract class AbstractConfigUI implements ConfigUI {

    private static final transient Logger LOG = LoggerFactory.getLogger(AbstractConfigUI.class);

    protected void save() {
        try {
            Config.getInstance().save();
        } catch (IOException e) {
            LOG.error("Couldn't save config");
        }
    }
}
