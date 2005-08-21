package hudson.tasks;

import hudson.Proc;
import hudson.Util;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Project;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import static hudson.model.Hudson.isWindows;

/**
 * Executes a series of commands by using a shell.
 *
 * @author Kohsuke Kawaguchi
 */
public class Shell implements BuildStep {
    private final String command;

    public Shell(String command) {
        this.command = fixCrLf(command);
    }

    /**
     * Fix CR/LF in the string according to the platform we are running on.
     */
    private String fixCrLf(String s) {
        // eliminate CR
        int idx;
        while((idx=s.indexOf("\r\n"))!=-1)
            s = s.substring(0,idx)+s.substring(idx+1);

        // add CR back if this is for Windows
        if(isWindows()) {
            idx=0;
            while(true) {
                idx = s.indexOf('\n',idx);
                if(idx==-1) break;
                s = s.substring(0,idx)+'\r'+s.substring(idx);
                idx+=2;
            }
        }
        return s;
    }

    public String getCommand() {
        return command;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    public boolean perform(Build build, BuildListener listener) {
        Project proj = build.getProject();
        File script=null;
        try {
            try {
                script = File.createTempFile("hudson","sh");
                Writer w = new FileWriter(script);
                w.write(command);
                w.close();
            } catch (IOException e) {
                Util.displayIOException(e,listener);
                e.printStackTrace( listener.fatalError("Unable to produce a script file") );
                return false;
            }

            String cmd = DESCRIPTOR.getShell()+" -x -e "+script.getPath();

            listener.getLogger().println("$ "+cmd);

            int r = 0;
            try {
                r = new Proc(cmd,build.getEnvVars(),listener.getLogger(),proj.getWorkspace()).join();
            } catch (IOException e) {
                Util.displayIOException(e,listener);
                e.printStackTrace( listener.fatalError("command execution failed") );
            }
            return r==0;
        } finally {
            if(script!=null)
                script.delete();
        }
    }

    public BuildStepDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static final Descriptor DESCRIPTOR = new Descriptor();

    public static final class Descriptor extends BuildStepDescriptor {
        private Descriptor() {
            super(Shell.class);
        }

        public String getShell() {
            String shell = (String)getProperties().get("shell");
            if(shell==null)
                shell = isWindows()?"sh":"/bin/sh";
            return shell;
        }

        public void setShell(String shell) {
            getProperties().put("shell",shell);
            save();
        }

        public String getDisplayName() {
            return "Execute shell";
        }

        public BuildStep newInstance(HttpServletRequest req) {
            return new Shell(req.getParameter("shell"));
        }

        public boolean configure( HttpServletRequest req ) {
            setShell(req.getParameter("shell"));
            return true;
        }
    };
}
