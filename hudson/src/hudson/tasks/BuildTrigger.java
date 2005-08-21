package hudson.tasks;

import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Project;

import javax.servlet.http.HttpServletRequest;
import java.util.StringTokenizer;

/**
 * Triggers builds of other projects.
 *
 * @author Kohsuke Kawaguchi
 */
public class BuildTrigger implements BuildStep {

    /**
     * Comma-separated list of other projects to be scheduled.
     */
    private final String childProjects;

    public BuildTrigger(String artifacts) {
        this.childProjects = artifacts;
    }

    public String getChildProjects() {
        return childProjects;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    public boolean perform(Build build, BuildListener listener) {

        Hudson hudson = Hudson.getInstance();

        StringTokenizer tokens = new StringTokenizer(childProjects,",");
        while(tokens.hasMoreTokens()) {
            String projectName = tokens.nextToken().trim();
            listener.getLogger().println("Triggering a new build of "+projectName);

            Job job = hudson.getJob(projectName);
            if(!(job instanceof Project)) {
                listener.getLogger().println(projectName+" is not a project");
                return false;
            }
            Project p = (Project) job;
            hudson.getQueue().add(p);
        }

        return true;
    }

    public BuildStepDescriptor getDescriptor() {
        return DESCRIPTOR;
    }


    public static final BuildStepDescriptor DESCRIPTOR = new BuildStepDescriptor(BuildTrigger.class) {
        public String getDisplayName() {
            return "Build other projects";
        }

        public BuildStep newInstance(HttpServletRequest req) {
            return new BuildTrigger(req.getParameter("childProjects"));
        }
    };
}
