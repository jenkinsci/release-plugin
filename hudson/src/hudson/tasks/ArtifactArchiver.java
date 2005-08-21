package hudson.tasks;

import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

/**
 * Copies the artifacts into an archive directory.
 *
 * @author Kohsuke Kawaguchi
 */
public class ArtifactArchiver implements BuildStep {

    /**
     * Comma-separated list of files/directories to be archived.
     */
    private final String artifacts;

    public ArtifactArchiver(String artifacts) {
        this.artifacts = artifacts;
    }

    public String getArtifacts() {
        return artifacts;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        listener.getLogger().println("Removing artifacts from the previous build");

        File dir = build.getArtifactsDir();
        if(!dir.exists())   return true;

        Delete delTask = new Delete();
        delTask.setProject(new org.apache.tools.ant.Project());
        delTask.setDir(dir);
        delTask.setIncludes(artifacts);

        execTask(delTask,listener);

        return true;
    }

    public boolean perform(Build build, BuildListener listener) {
        Project p = build.getProject();

        Copy copyTask = new Copy();
        copyTask.setProject(new org.apache.tools.ant.Project());
        File dir = build.getArtifactsDir();
        dir.mkdirs();
        copyTask.setTodir(dir);
        FileSet src = new FileSet();
        src.setDir(p.getWorkspace());
        src.setIncludes(artifacts);
        copyTask.addFileset(src);

        execTask(copyTask, listener);

        return true;
    }

    private void execTask(Task copyTask, BuildListener listener) {
        try {
            copyTask.execute();
        } catch( BuildException e ) {
            // failing to archive isn't a fatal error
            e.printStackTrace(listener.error(e.getMessage()));
        }
    }

    public BuildStepDescriptor getDescriptor() {
        return DESCRIPTOR;
    }


    public static final BuildStepDescriptor DESCRIPTOR = new BuildStepDescriptor(ArtifactArchiver.class) {
        public String getDisplayName() {
            return "Archive the artifacts";
        }

        public BuildStep newInstance(HttpServletRequest req) {
            return new ArtifactArchiver(req.getParameter("artifacts"));
        }
    };
}
