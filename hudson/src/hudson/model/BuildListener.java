package hudson.model;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Receives events that happen during a build.
 *
 * @author Kohsuke Kawaguchi
 */
public interface BuildListener {

    /**
     * Called when a build is started.
     */
    void started();

    /**
     * This writer will receive the output of the build.
     *
     * @return
     *      must be non-null.
     */
    PrintStream getLogger();

    /**
     * An error in the build.
     *
     * @return
     *      If return non-null, it will receive details of the error.
     */
    PrintWriter error(String msg);

    /**
     * A fatal error in the build.
     *
     * @return
     *      If return non-null, it will receive details of the error.
     */
    PrintWriter fatalError(String msg);

    /**
     * Called when a build is finished.
     */
    void finished(Result result);
}
