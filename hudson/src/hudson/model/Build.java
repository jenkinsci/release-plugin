package hudson.model;

import hudson.Proc;
import hudson.scm.CVSChangeLog;
import hudson.tasks.BuildStep;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.Map;

import static hudson.model.Hudson.isWindows;

/**
 * @author Kohsuke Kawaguchi
 */
public final class Build extends Run<Project,Build> implements Runnable {

    /**
     * Creates a new build.
     */
    Build(Project project) throws IOException {
        super(project);
    }

    public Project getProject() {
        return getParent();
    }

    /**
     * Loads a build from a log file.
     */
    Build(Project project, File buildDir, Build prevBuild ) throws IOException {
        super(project,buildDir,prevBuild);
    }

    /**
     * Gets the changes incorporated into this build.
     */
    public CVSChangeLog[] getChangeSet() {
        try {
            return CVSChangeLog.parse(new File(getRootDir(),"changelog.xml"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return new CVSChangeLog[0];
    }

    public Calendar due() {
        return timestamp;
    }

    /**
     * Performs a build.
     */
    public void run() {
        run(new Runner() {
            public Result run(BuildListener listener) throws IOException {
                if(!project.checkout(Build.this,listener))
                    return Result.FAILURE;

                if(!project.getScm().calcChangeLog(Build.this,new File(getRootDir(),"changelog.xml"),listener))
                    return Result.FAILURE;

                if(!preBuild(listener,project.getBuilders()))
                    return Result.FAILURE;
                if(!preBuild(listener,project.getPublishers()))
                    return Result.FAILURE;

                if(!build(listener,project.getBuilders()))
                    return Result.FAILURE;

                if(!isWindows()) {
                    try {
                        // ignore a failure.
                        new Proc(new String[]{"rm","../latest"},new String[0],listener.getLogger(),getProject().getBuildDir()).join();
                        int r = new Proc(new String[]{"ln","-s",getRootDir().getPath(),"../lastSuccessful"},
                            new String[0],listener.getLogger(),getProject().getBuildDir()).join();
                        if(r!=0)
                            listener.getLogger().println("ln failed: "+r);
                    } catch (IOException e) {
                        PrintStream log = listener.getLogger();
                        log.println("ln failed");
                        e.printStackTrace( log );
                    }
                }

                return Result.SUCCESS;
            }

            public void post(BuildListener listener) {
                build(listener,project.getPublishers());
            }
        });
    }

    private boolean build(BuildListener listener,Map<?,BuildStep> steps) {
        for( BuildStep bs : steps.values() )
            if(!bs.perform(this,listener))
                return false;
        return true;
    }

    private boolean preBuild(BuildListener listener,Map<?,BuildStep> steps) {
        for( BuildStep bs : steps.values() )
            if(!bs.prebuild(this,listener))
                return false;
        return true;
    }

    @Override
    public Map getEnvVars() {
        Map env = super.getEnvVars();
        project.getScm().buildEnvVars(env);
        return env;
    }


//
//
// actions
//
//

}
