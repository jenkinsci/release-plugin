package hudson.plugins.release;

import hudson.Plugin;
import hudson.tasks.BuildWrappers;

/**
 * Entry point for plugins
 *
 * @author Peter Hayes
 * @since 0.1
 */
public class PluginImpl extends Plugin {

    @Override
    public void start() throws Exception {
        BuildWrappers.WRAPPERS.add(ReleaseWrapper.DESCRIPTOR);
    }
}
