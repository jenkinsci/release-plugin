package hudson.model;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.XppReader;
import hudson.Util;
import hudson.tasks.LogRotator;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;

/**
 * A job is an runnable entity under the monitoring of Hudson.
 *
 * <p>
 * Every time it "runs", it will be recorded as a {@link Run} object.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Job<JobT extends Job, RunT extends Run<JobT,RunT>>
        extends Actionable implements ModelObject {
    /**
     * Project name.
     */
    protected transient String name;

    /**
     * Project description. Can be HTML.
     */
    protected String description;

    /**
     * Root directory for this job.
     */
    protected transient File root;

    protected int nextBuildNumber = 1;
    private transient Hudson parent;

    private LogRotator logRotator;

    protected Job(Hudson parent,String name) {
        this.parent = parent;
        this.name = name;
        this.root = new File(new File(parent.root,"jobs"),name);
        this.root.mkdirs();
    }

    /**
     * Called when a {@link Job} is loaded from memory.
     */
    protected void onLoad(Hudson root, String name) throws IOException {
        this.parent = root;
        this.name = name;
        this.root = new File(new File(parent.root,"jobs"),name);
    }

    public Hudson getParent() {
        return parent;
    }

    /**
     * Allocates a new buildCommand number.
     */
    public int assignBuildNumber() throws IOException {
        int r = nextBuildNumber++;
        save();
        return r;
    }

    public int getNextBuildNumber() {
        return nextBuildNumber;
    }

    /**
     * Gets the project description HTML.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the project description HTML.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the log rotator for this job, or null if none.
     */
    public LogRotator getLogRotator() {
        return logRotator;
    }

    public void setLogRotator(LogRotator logRotator) {
        this.logRotator = logRotator;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return getName();
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets all the builds.
     *
     * @return
     *      never null. The first entry is the latest buildCommand.
     */
    public synchronized Collection<? extends RunT> getBuilds() {
        return _getRuns().values();
    }

    public synchronized RunT getBuild(String id) {
        return _getRuns().get(id);
    }

    /**
     * The file we save our configuration.
     */
    protected final File getConfigFile() {
        return new File(root,"config.xml");
    }

    File getBuildDir() {
        return new File(root,"builds");
    }

    /**
     * Returns the URL of this project.
     */
    public String getUrl() {
        return "job/"+name+'/';
    }


    /**
     * Gets all the runs.
     */
    protected abstract SortedMap<String,? extends RunT> _getRuns();

    /**
     * Called from {@link Run} to remove it from this job.
     *
     * The files are deleted already. So all the callee needs to do
     * is to remove a reference from this {@link Job}.
     */
    protected abstract void removeRun(Run run);

    /**
     * Returns the last build.
     */
    public synchronized RunT getLastBuild() {
        SortedMap<String,? extends RunT> runs = _getRuns();

        if(runs.isEmpty())    return null;
        return runs.get(runs.firstKey());
    }

    /**
     * Returns the last successful build, if any. Otherwise null.
     */
    public synchronized RunT getLastSuccessfulBuild() {
        RunT r = getLastBuild();
        while(r!=null && r.getResult()!=Result.SUCCESS)
            r=r.getPreviousBuild();
        return r;
    }

    /**
     * Returns the last failed build, if any. Otherwise null.
     */
    public synchronized RunT getLastFailedBuild() {
        RunT r = getLastBuild();
        while(r!=null && r.getResult()!=Result.FAILURE)
            r=r.getPreviousBuild();
        return r;
    }

    public synchronized String getIconColor() {
        RunT lastBuild = getLastBuild();
        while(lastBuild!=null && lastBuild.hasntStartedYet())
            lastBuild = lastBuild.getPreviousBuild();

        if(lastBuild!=null)
            return lastBuild.getIconColor();
        else
            return "grey";
    }


    /**
     * Save the settings to a file.
     */
    public synchronized void save() throws IOException {
        Writer w = new OutputStreamWriter(new FileOutputStream(getConfigFile()),"UTF-8");
        w.write("<?xml version='1.0' encoding='UTF-8'?>\n");
        createConfiguredStream().toXML(this,w);
        w.close();
    }

    protected static XStream createConfiguredStream() {
        XStream xs = new XStream();
        xs.alias("project",Project.class);
        return xs;
    }

    /**
     * Loads a project from a config file.
     */
    static Job load(Hudson root, File dir) throws IOException {
        Reader r = new InputStreamReader(new FileInputStream(new File(dir,"config.xml")),"UTF-8");
        Job job = (Job)createConfiguredStream().unmarshal(new XppReader(r));
        r.close();

        job.onLoad(root,dir.getName());

        return job;
    }

    protected static final Comparator reverseComparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            return -((Comparable)o1).compareTo((Comparable)o2);
        }
    };



//
//
// actions
//
//
    /**
     * Accepts submission from the configuration page.
     */
    public synchronized void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        nextBuildNumber = Integer.parseInt(req.getParameter("buildNumber"));
        description = req.getParameter("description");

        if(req.getParameter("logrotate")!=null)
            logRotator = LogRotator.DESCRIPTOR.newInstance(req);
        else
            logRotator = null;

        save();
        rsp.sendRedirect(".");
    }

    /**
     * Returns the image that shows the current buildCommand status.
     */
    public void doBuildStatus( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        rsp.sendRedirect(req.getContextPath()+'/'+getBuildStatusUrl());
    }

    public String getBuildStatusUrl() {
        return "images/"+getIconColor()+".gif";
    }

    /**
     * Deletes this project.
     */
    public synchronized void doDoDelete( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        Util.deleteRecursive(root);
        getParent().jobs.remove(name);
        rsp.sendRedirect(req.getContextPath());
    }

    /**
     * RSS feed for all runs.
     */
    public synchronized void doRssAll( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        List<Run> runs = new ArrayList<Run>(getBuilds());
        Hudson.forwardToRss(this,getDisplayName()+" all builds",req,rsp,runs);
    }

    /**
     * RSS feed for failed runs.
     */
    public synchronized void doRssFailed( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        List<Run> runs = new ArrayList<Run>();
        for( Run r=getLastFailedBuild(); r!=null; r=r.getPreviousFailedBuild() )
            runs.add(r);
        Hudson.forwardToRss(this,getDisplayName()+" all failures",req,rsp,runs);
    }
}
