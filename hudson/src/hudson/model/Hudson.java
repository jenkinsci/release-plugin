package hudson.model;

import com.thoughtworks.xstream.io.xml.XppReader;
import hudson.XStreamEx;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMManager;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import org.apache.tools.ant.taskdefs.Execute;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Root object of the system.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Hudson implements ModelObject {
    private transient final Queue queue = new Queue();

    /**
     * {@link Executor}s in this system. Read-only.
     */
    private transient final List<Executor> executors;

    private int numExecutors = 2;

    /**
     * False to enable anyone to do anything.
     */
    private boolean useSecurity = false;

    /**
     * Root directory of the system.
     */
    public transient final File root;

    /**
     * All {@link Job}s keyed by their names.
     */
    /*package*/ transient final Map<String,Job> jobs = new TreeMap<String,Job>();

    /**
     * The sole instance.
     */
    private static Hudson theInstance;

    /**
     * Set to true if this instance is going to shut down.
     */
    private boolean shuttingDown;

    public static Hudson getInstance() {
        return theInstance;
    }


    public Hudson(File root) throws IOException {
        this.root = root;
        if(theInstance!=null)
            throw new IllegalStateException("second instance");
        theInstance = this;

        load();

        this.executors = new ArrayList<Executor>(numExecutors);
        for( int i=0; i<numExecutors; i++ )
            executors.add(new Executor(this));

    }

    /**
     * Gets the snapshot of all the jobs.
     */
    public synchronized Collection<Job> getJobs() {
        return new ArrayList<Job>(jobs.values());
    }

    /**
     * Gets the list of all {@link Executor}s.
     */
    public List<Executor> getExecutors() {
        return executors;
    }

    public Queue getQueue() {
        return queue;
    }

    public String getDisplayName() {
        return "Hudson";
    }

    /**
     * Dummy method that returns "".
     * used from JSPs.
     */
    public String getUrl() {
        return "";
    }

    public File getRootDir() {
        return root;
    }

    public boolean isUseSecurity() {
        return useSecurity;
    }

    public void setUseSecurity(boolean useSecurity) {
        this.useSecurity = useSecurity;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * Gets the job of the given name.
     *
     * @return null
     *      if such a project doesn't exist.
     */
    public synchronized Job getJob(String name) {
        return jobs.get(name);
    }

    /**
     * Creates a new job.
     *
     * @throws IllegalArgumentException
     *      if the project of the given name already exists.
     */
    public synchronized Job createProject( Class type, String name ) throws IOException {
        if(jobs.containsKey(name))
            throw new IllegalArgumentException();
        if(!Job.class.isAssignableFrom(type))
            throw new IllegalArgumentException();

        Job job;
        try {
            job = (Job)type.getConstructor(Hudson.class,String.class).newInstance(this,name);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        job.save();
        jobs.put(name,job);
        return job;
    }

    /**
     * The file we save our configuration.
     */
    private File getConfigFile() {
        return new File(root,"config.xml");
    }

    /**
     * Returns the number of {@link Executor}s.
     *
     * This may be different from <code>getExecutors().size()</code>
     * because it takes time to adjust the number of executors.
     */
    public int getNumExecutors() {
        return numExecutors;
    }

    private void load() throws IOException {
        if(getConfigFile().exists()) {
            Reader r = new InputStreamReader(new FileInputStream(getConfigFile()),"UTF-8");
            createConfiguredStream().unmarshal(new XppReader(r),this);
            r.close();
        }

        File projectsDir = new File(root,"jobs");
        projectsDir.mkdirs();
        File[] subdirs = projectsDir.listFiles(new FileFilter() {
            public boolean accept(File child) {
                return child.isDirectory();
            }
        });
        jobs.clear();
        for (final File subdir : subdirs) {
            try {
                Job p = Job.load(this,subdir);
                jobs.put(p.getName(), p);
            } catch (IOException e) {
                e.printStackTrace(); // TODO: logging
            }
        }
    }

    /**
     * Save the settings to a file.
     */
    public synchronized void save() throws IOException {
        createConfiguredStream().toXML(this,getConfigFile());
    }

    private static XStreamEx createConfiguredStream() {
        XStreamEx xs = new XStreamEx();
        xs.alias("hudson",Hudson.class);
        return xs;
    }


    /**
     * Environmental variables that we've inherited.
     */
    public static final Map<String,String> masterEnvVars;

    static {
        Vector<String> envs = Execute.getProcEnvironment();
        Map<String,String> m = new HashMap<String,String>();
        for (String e : envs) {
            int idx = e.indexOf('=');
            m.put(e.substring(0, idx), e.substring(idx + 1));
        }
        masterEnvVars = Collections.unmodifiableMap(m);
    }

    /**
     * Called to shut down the system.
     */
    public void cleanUp() {
        shuttingDown = true;
        for( Executor e : executors )
            e.interrupt();
    }



//
//
// actions
//
//
    /**
     * Accepts submission from the configuration page.
     */
    public synchronized void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        useSecurity = req.getParameter("use_security")!=null;

        numExecutors = Integer.parseInt(req.getParameter("numExecutors"));

        synchronized(this) {
            for( Executor e : executors )
                if(e.getCurrentBuild()==null)
                    e.interrupt();

            while(executors.size()<numExecutors)
                executors.add(new Executor(this));
        }

        boolean result = true;

        for( BuildStepDescriptor d : BuildStep.BUILDERS )
            result &= d.configure(req);

        for( BuildStepDescriptor d : BuildStep.PUBLISHERS )
            result &= d.configure(req);

        for( SCMDescriptor scmd : SCMManager.getSupportedSCMs() )
            result &= scmd.configure(req);

        save();
        if(result)
            rsp.sendRedirect(".");  // go to the top page
        else
            rsp.sendRedirect("configure"); // back to config
    }

    public synchronized void doCreateJob( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        String name = req.getParameter("name");
        String className = req.getParameter("type");

        if(name==null || getJob(name)!=null || className==null) {
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            Class type = Class.forName(className);

            // redirect to the project config screen
            rsp.sendRedirect(req.getContextPath()+'/'+createProject(type,name).getUrl()+"configure");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Called once the user logs in. Just forward to the top page.
     */
    public synchronized void doLoginEntry( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        rsp.sendRedirect(req.getContextPath());
    }

    /**
     * Called once the user logs in. Just forward to the top page.
     */
    public synchronized void doLogout( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        HttpSession session = req.getSession(false);
        if(session!=null)
            session.invalidate();
        rsp.sendRedirect(req.getContextPath());
    }

    private static final Comparator<Run> runComparator = new Comparator<Run>() {
        public int compare(Run lhs, Run rhs) {
            long r = lhs.getTimestamp().getTimeInMillis() - rhs.getTimestamp().getTimeInMillis();
            if(r<0)     return +1;
            if(r>0)     return -1;
            return lhs.getParent().getName().compareTo(rhs.getParent().getName());
        }
    };

    /**
     * RSS feed for all runs.
     */
    public synchronized void doRssAll( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        SortedSet<Run> runs = new TreeSet<Run>(runComparator);
        for( Job j : getJobs() )
            runs.addAll( j.getBuilds() );

        forwardToRss(this,"Hudson all builds",req,rsp,runs);
    }

    /**
     * RSS feed for failed runs.
     */
    public synchronized void doRssFailed( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        SortedSet<Run> runs = new TreeSet<Run>(runComparator);
        for( Job j : getJobs() )
            runs.addAll( j.getBuilds() );

        for (Iterator<Run> i = runs.iterator(); i.hasNext();) {
            if(i.next().getResult()!=Result.FAILURE)
                i.remove();
        }

        forwardToRss(this,"Hudson all failures", req,rsp,runs);
    }

    /**
     * Sends the RSS feed to the client.
     */
    static void forwardToRss( Object it, String title, StaplerRequest req, HttpServletResponse rsp, Collection<Run> runs) throws IOException, ServletException {
        GregorianCalendar threshold = new GregorianCalendar();
        threshold.add(Calendar.DAY_OF_YEAR,-7);

        int count=0;

        for (Iterator<Run> i = runs.iterator(); i.hasNext();) {
            // at least put 10 items
            if(count<10) {
                i.next();
                count++;
                continue;
            }
            // anything older than 7 days will be ignored
            if(i.next().getTimestamp().before(threshold))
                i.remove();
        }

        req.setAttribute("it",it);
        req.setAttribute("title",title);
        req.setAttribute("runs",runs);
        req.getServletContext().getRequestDispatcher("/WEB-INF/rss.jsp").forward(req,rsp);
    }

    /**
     * Reloads the configuration.
     */
    public synchronized void doReload( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        load();
        rsp.sendRedirect(req.getContextPath());
    }



    public static boolean isWindows() {
        return File.pathSeparatorChar==';';
    }
}
