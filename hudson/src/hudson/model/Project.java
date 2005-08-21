package hudson.model;

import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.scm.SCMManager;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

/**
 * @author Kohsuke Kawaguchi
 */
public class Project extends Job<Project,Build> {

    /**
     * All the builds keyed by their ID.
     */
    private transient SortedMap<String,Build> builds =
        Collections.synchronizedSortedMap(new TreeMap<String,Build>(reverseComparator));

    private SCM scm = new NullSCM();

    // TODO: consolidate build commands into one build step.
    /**
     * List of {@link BuildStep}s.
     */
    private List<BuildStep> builders = new Vector<BuildStep>();

    private List<BuildStep> publishers = new Vector<BuildStep>();

    /**
     * Creates a new project.
     */
    public Project(Hudson parent,String name) {
        super(parent,name);
        getBuildDir().mkdirs();
    }

    protected void onLoad(Hudson root, String name) throws IOException {
        super.onLoad(root, name);
        builds = new TreeMap<String,Build>(reverseComparator);

        // load builds
        File buildDir = getBuildDir();
        String[] builds = buildDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return new File(dir,name).isDirectory();
            }
        });
        Arrays.sort(builds);

        for( String build : builds ) {
            File d = new File(buildDir,build);
            if(new File(d,"build.xml").exists()) {
                // if the build result file isn't in the directory, ignore it.
                try {
                    Build b = new Build(this,d,getLastBuild());
                    this.builds.put( b.getId(), b );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public SCM getScm() {
        return scm;
    }

    public void setScm(SCM scm) {
        this.scm = scm;
    }

    public synchronized Map<BuildStepDescriptor,BuildStep> getBuilders() {
        Map<BuildStepDescriptor,BuildStep> m = new HashMap<BuildStepDescriptor,BuildStep>();
        for( int i=builders.size()-1; i>=0; i-- ) {
            BuildStep b = builders.get(i);
            m.put(b.getDescriptor(),b);
        }
        return m;
    }

    public synchronized Map<BuildStepDescriptor,BuildStep> getPublishers() {
        Map<BuildStepDescriptor,BuildStep> m = new HashMap<BuildStepDescriptor,BuildStep>();
        for( int i=publishers.size()-1; i>=0; i-- ) {
            BuildStep b = publishers.get(i);
            m.put(b.getDescriptor(),b);
        }
        return m;
    }

    public SortedMap<String, ? extends Build> _getRuns() {
        return builds;
    }

    public synchronized void removeRun(Run run) {
        builds.remove(run.getId());
    }

    /**
     * Creates a new build of this project for immediate execution.
     */
    public synchronized Build newBuild() throws IOException {
        Build lastBuild = new Build(this);
        builds.put(lastBuild.getId(),lastBuild);
        return lastBuild;
    }

    public boolean checkout(Build build, BuildListener listener) throws IOException {
        if(scm==null)
            return true;    // no SCM

        File workspace = getWorkspace();
        workspace.mkdirs();
        return scm.checkout(build,workspace,listener);
    }

    /**
     * Gets the directory where the module is checked out.
     */
    public File getWorkspace() {
        return new File(root,"workspace");
    }

    /**
     * Returns the root directory of the checked-out module.
     */
    public File getModuleRoot() {
        String module = getScm().getModule();
        if(module.length()==0)
            return getWorkspace();
        else
            return new File(getWorkspace(),module);
    }



//
//
// actions
//
//
    /**
     * Schedules a new build command.
     */
    public void doBuild( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        getParent().getQueue().add(this);
        rsp.forwardToPreviousPage(req);
    }

    /**
     * Accepts submission from the configuration page.
     */
    public synchronized void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        int scmidx = Integer.parseInt(req.getParameter("scm"));
        scm = SCMManager.getSupportedSCMs()[scmidx].newInstance(req);

        builders.clear();
        for( int i=0; i<BuildStep.BUILDERS.length; i++ ) {
            if(req.getParameter("builder"+i)!=null) {
                BuildStep b = BuildStep.BUILDERS[i].newInstance(req);
                builders.add(b);
            }
        }

        publishers.clear();
        for( int i=0; i<BuildStep.PUBLISHERS.length; i++ ) {
            if(req.getParameter("publisher"+i)!=null) {
                BuildStep b = BuildStep.PUBLISHERS[i].newInstance(req);
                publishers.add(b);
            }
        }

        super.doConfigSubmit(req,rsp);
    }

    /**
     * Serves the workspace files.
     */
    public synchronized void doWs( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        String path = req.getRestOfPath();

        if(path.length()==0)
            path = "/";

        if(path.indexOf("..")!=-1 || path.length()<1) {
            // don't serve anything other than files in the artifacts dir
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        File f = new File(getWorkspace(),path.substring(1));
        if(!f.exists()) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if(f.isDirectory()) {
            if(!req.getRequestURL().toString().endsWith("/")) {
                rsp.sendRedirect(req.getRequestURL().append('/').toString());
                return;
            }

            req.setAttribute("it",this);
            req.setAttribute("files",f.listFiles());
            req.getView(this,"workspaceDir.jsp").forward(req,rsp);
            return;
        }

        FileInputStream in = new FileInputStream(f);
        // serve the file
        rsp.setContentType(req.getServletContext().getMimeType(req.getServletPath()));
        rsp.setContentLength((int)f.length());
        byte[] buf = new byte[1024];
        int len;
        while((len=in.read(buf))>0)
            rsp.getOutputStream().write(buf,0,len);
        in.close();
        return;
    }
}
