package hudson.tasks;

import hudson.Proc;
import hudson.Util;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Project;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class Ant implements BuildStep {

    private final String targets;

    /**
     * Identifies {@link AntInstallation} to be used.
     */
    private final String antName;

    public Ant(String targets,String antName) {
        this.targets = targets;
        this.antName = antName;
    }

    public String getTargets() {
        return targets;
    }

    /**
     * Gets the Ant to invoke,
     * or null to invoke the default one.
     */
    public AntInstallation getAnt() {
        for( AntInstallation i : DESCRIPTOR.getInstallations() ) {
            if(antName!=null && i.getName().equals(antName))
                return i;
        }
        return null;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    public boolean perform(Build build, BuildListener listener) {
        Project proj = build.getProject();

        String cmd;

        String execName;
        if(File.separatorChar=='\\')
            execName = "ant.bat";
        else
            execName = "ant";

        AntInstallation ai = getAnt();
        if(ai==null)
            cmd = execName+' '+targets;
        else {
            File exec = ai.getExecutable();
            if(!ai.getExists()) {
                listener.fatalError(exec+" doesn't exist");
                return false;
            }
            cmd = exec.getPath()+' '+targets;
        }

        Map env = build.getEnvVars();
        if(ai!=null)
            env.put("ANT_HOME",ai.getAntHome());

        listener.getLogger().println("$ "+cmd);

        try {
            int r = new Proc(cmd,env,listener.getLogger(),proj.getModuleRoot()).join();
            return r==0;
        } catch (IOException e) {
            Util.displayIOException(e,listener);
            e.printStackTrace( listener.fatalError("command execution failed") );
            return false;
        }
    }

    public BuildStepDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static final Descriptor DESCRIPTOR = new Descriptor();

    public static final class Descriptor extends BuildStepDescriptor {
        private Descriptor() {
            super(Ant.class);
        }

        public String getDisplayName() {
            return "Invoke top-level Ant targets";
        }

        public AntInstallation[] getInstallations() {
            AntInstallation[] r = (AntInstallation[]) getProperties().get("installations");

            if(r==null)
                return new AntInstallation[0];

            return r.clone();
        }

        public boolean configure(HttpServletRequest req) {
            boolean r = true;

            List lst = new ArrayList();
            int i;
            for( i=0; req.getParameter("ant_name"+i)!=null; i++ ) {
                String name = req.getParameter("ant_name"+i);
                String home = req.getParameter("ant_home"+i);
                if(name.length()==0)    continue;

                lst.add(new AntInstallation(name,home));
            }

            if(req.getParameter("ant_delete")!=null) {
                if(i==lst.size())
                    lst.remove(i-1);
                r = false;
            }

            if(req.getParameter("ant_add")!=null) {
                r = false;
                lst.add(new AntInstallation("",""));
            }

            getProperties().put("installations",lst.toArray(new AntInstallation[lst.size()]));

            save();

            return r;
        }

        public BuildStep newInstance(HttpServletRequest req) {
            return new Ant(req.getParameter("ant_targets"),req.getParameter("ant_version"));
        }
    };

    public static final class AntInstallation {
        private final String name;
        private final String antHome;

        public AntInstallation(String name, String antHome) {
            this.name = name;
            this.antHome = antHome;
        }

        /**
         * install directory.
         */
        public String getAntHome() {
            return antHome;
        }

        /**
         * Human readable display name.
         */
        public String getName() {
            return name;
        }

        public File getExecutable() {
            String execName;
            if(File.separatorChar=='\\')
                execName = "ant.bat";
            else
                execName = "ant";

            return new File(getAntHome(),"bin/"+execName);
        }

        /**
         * Returns true if the executable exists.
         */
        public boolean getExists() {
            return getExecutable().exists();
        }
    }
}
