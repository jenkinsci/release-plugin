package hudson.model;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.Serializable;
import java.io.IOException;

/**
 * Contributes an item to the task list.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Action extends Serializable, ModelObject {
    /**
     * Gets the file name of the icon (relative to /images/24x24)
     */
    String getIconFileName();

    /**
     * Gets the string to be displayed.
     */
    String getDisplayName();

    /**
     * Performs the action.
     */
    void doAction( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException;
}
