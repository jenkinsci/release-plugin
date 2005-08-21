package hudson.model;

import hudson.XStreamEx;
import org.kohsuke.stapler.Stapler;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class Descriptor {
    private Map properties;

    /**
     * The class being described by this descriptor.
     */
    protected final Class clazz;

    protected Descriptor(Class clazz) {
        this.clazz = clazz;
    }

    /**
     * Returns the data store that can be used to store configuration info.
     */
    protected synchronized Map getProperties() {
        if(properties==null)
            properties = load();
        return properties;
    }

    /**
     * Invoked when the global configuration page is submitted.
     *
     * Can be overrided to store descriptor-specific information.
     *
     * @return false
     *      to keep the client in the same config page.
     */
    public boolean configure( HttpServletRequest req ) {
        return true;
    }

    public final String getConfigPage() {
        return Stapler.getViewURL(clazz,"config.jsp");
    }

    public final String getGlobalConfigPage() {
        return Stapler.getViewURL(clazz,"global.jsp");
    }


    /**
     * Saves the configuration info to the disk.
     */
    protected synchronized void save() {
        if(properties!=null)
            try {
                new XStreamEx().toXML(properties,getConfigFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private Map load() {
        // load
        File file = getConfigFile();
        if(!file.exists())
            return new HashMap();

        try {
            return (Map)new XStreamEx().fromXML(file);
        } catch (IOException e) {
            return new HashMap();
        }
    }

    private File getConfigFile() {
        return new File(Hudson.getInstance().getRootDir(),clazz.getName()+".xml");
    }
}
