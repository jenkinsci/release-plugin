package hudson.tasks;

import hudson.model.Descriptor;

import javax.servlet.http.HttpServletRequest;

/**
 * Metadata of a {@link BuildStep} class.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class BuildStepDescriptor extends Descriptor {

    protected BuildStepDescriptor(Class clazz) {
        super(clazz);
    }

    public final boolean isInstance( BuildStep bs ) {
        return clazz.isInstance(bs);
    }

    /**
     * Human readable name of the SCM.
     */
    public abstract String getDisplayName();

    /**
     * Creates a new instance of the BuildStep.
     */
    public abstract BuildStep newInstance(HttpServletRequest req);
}
