package hudson.scm;

import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.Proc;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.util.Map;
import java.util.HashMap;

/**
 * Common implementation between {@link CVSSCM} and {@link SubversionSCM}.
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractCVSFamilySCM implements SCM {
    /**
     * Invokes the command with the specified command line option and wait for its completion.
     */
    protected final boolean run(String cmd, BuildListener listener, File dir) throws IOException {
        listener.getLogger().println("$ "+cmd);

        Map env = createEnvVarMap();

        int r = new Proc(cmd,env,listener.getLogger(),dir).join();
        if(r!=0)
            listener.fatalError(getDescriptor().getDisplayName()+" failed");

        return r==0;
    }

    protected final Map createEnvVarMap() {
        Map env = new HashMap(Hudson.masterEnvVars);
        buildEnvVars(env);
        return env;
    }

    protected final boolean createEmptyChangeLog(File changelogFile, BuildListener listener) {
        try {
            FileWriter w = new FileWriter(changelogFile);
            w.write("<changelog/>");
            w.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace(listener.error(e.getMessage()));
            return false;
        }
    }
}
