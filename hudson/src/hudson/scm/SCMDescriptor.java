package hudson.scm;

import hudson.model.Descriptor;

import javax.servlet.http.HttpServletRequest;

/**
 * Describes the {@link SCM} implementation.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class SCMDescriptor extends Descriptor {

    protected SCMDescriptor(Class clazz) {
        super(clazz);
    }

    public final boolean isInstance( SCM scm ) {
        return clazz.isInstance(scm);
    }

    /**
     * Human readable name of the SCM.
     */
    public abstract String getDisplayName();

    /**
     * Creates a new instance of the SCM.
     * @param req
     */
    public abstract SCM newInstance(HttpServletRequest req);
}
