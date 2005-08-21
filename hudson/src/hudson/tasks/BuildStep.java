package hudson.tasks;

import hudson.model.Build;
import hudson.model.BuildListener;

/**
 * One step of the whole build process.
 *
 * @author Kohsuke Kawaguchi
 */
public interface BuildStep {

    /**
     * Runs before the build begins.
     *
     * @return
     *      true if the build can continue, false if there was an error
     *      and the build needs to be aborted.
     */
    boolean prebuild( Build build, BuildListener listener );

    /**
     * Runs the step over the given build and reports the progress to the listener.
     *
     * @return
     *      true if the build can continue, false if there was an error
     *      and the build needs to be aborted.
     */
    boolean perform( Build build, BuildListener listener );

    /**
     * Gets the descriptor for this instance.
     */
    BuildStepDescriptor getDescriptor();

    /**
     * List of all installed {@link BuildStepDescriptor}s.
     */
    public static final BuildStepDescriptor[] BUILDERS = new BuildStepDescriptor[]{
        Shell.DESCRIPTOR,
        Ant.DESCRIPTOR
    };

    public static final BuildStepDescriptor[] PUBLISHERS = new BuildStepDescriptor[]{
        ArtifactArchiver.DESCRIPTOR,
        Mailer.DESCRIPTOR,
        BuildTrigger.DESCRIPTOR
    };
}
