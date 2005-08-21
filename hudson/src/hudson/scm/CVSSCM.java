package hudson.scm;

import hudson.Util;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Action;
import hudson.model.StreamBuildListener;
import hudson.model.Result;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.cvslib.ChangeLogTask;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * CVS.
 *
 * @author Kohsuke Kawaguchi
 */
public class CVSSCM extends AbstractCVSFamilySCM {
    /**
     * CVSSCM connection string.
     */
    private String cvsroot;

    /**
     * Module names.
     *
     * This could be a whitespace-separate list of multiple modules.
     */
    private String module;

    private String branch;

    private String cvsRsh;

    private boolean canUseUpdate;


    public CVSSCM(String cvsroot, String module,String branch,String cvsRsh,boolean canUseUpdate) {
        if(branch==null || branch.trim().length()==0)
            branch = null;
        if(cvsRsh==null || cvsRsh.trim().length()==0)
            cvsRsh = null;
        this.cvsroot = cvsroot;
        this.module = module;
        this.branch = branch;
        this.cvsRsh = cvsRsh;
        this.canUseUpdate = canUseUpdate;
    }

    public String getCvsRoot() {
        return cvsroot;
    }

    /**
     * If there are multiple modules, return the module directory of the first one.
     */
    public String getModule() {
        int idx = module.indexOf(' ');
        if(idx>=0)  return module.substring(0,idx);
        else        return module;
    }

    public String getBranch() {
        return branch;
    }

    public String getCvsRsh() {
        return cvsRsh;
    }

    public boolean getCanUseUpdate() {
        return canUseUpdate;
    }

    public boolean checkout(Build build, File dir, BuildListener listener) throws IOException {
        boolean result;

        if(canUseUpdate && isUpdatable(dir))
            result = update(dir,listener);
        else {
            Util.deleteContentsRecursive(dir);

            String cmd = MessageFormat.format("cvs -Q -z9 -d {0} co {2} {1}",
                cvsroot,
                module,
                branch!=null?"-r "+branch:""
            );

            result = run(cmd,listener,dir);
        }

        if(!result)
           return false;


        // archive the workspace to support later tagging
        File archiveFile = getArchiveFile(build);
        ZipOutputStream zos = new ZipOutputStream(archiveFile);
        StringTokenizer tokens = new StringTokenizer(module);
        while(tokens.hasMoreTokens()) {
            String m = tokens.nextToken();
            archive(new File(build.getProject().getWorkspace(),m),m,zos);
        }
        zos.close();

        // contribute the tag action
        build.getActions().add(new TagAction(build));

        return true;
    }

    /**
     * Returns the file name used to archive the build.
     */
    private static File getArchiveFile(Build build) {
        return new File(build.getRootDir(),"workspace.zip");
    }

    private void archive(File dir,String relPath,ZipOutputStream zos) throws IOException {
        // TODO: look at CVS/Entires and archive CVS-controlled files only
        for( File f : dir.listFiles() ) {
            String name = relPath+'/'+f.getName();
            if(f.isDirectory()) {
                archive(f,name,zos);
            } else {
                zos.putNextEntry(new ZipEntry(name));
                FileInputStream fis = new FileInputStream(f);
                Util.copyStream(fis,zos);
                fis.close();
                zos.closeEntry();
            }
        }
    }

    public boolean update(File dir, BuildListener listener) throws IOException {
        String cmd = "cvs -q -z9 update -PdC";
        StringTokenizer tokens = new StringTokenizer(module);
        while(tokens.hasMoreTokens()) {
            if(!run(cmd,listener,new File(dir,tokens.nextToken())))
                return false;
        }
        return true;
    }

    /**
     * Returns true if we can use "cvs update" instead of "cvs checkout"
     */
    private boolean isUpdatable(File dir) {
        StringTokenizer tokens = new StringTokenizer(module);
        while(tokens.hasMoreTokens()) {
            File module = new File(dir,tokens.nextToken());
            File cvs = new File(module,"CVS");
            if(!cvs.exists())
                return false;

            // check cvsroot
            if(!checkContents(new File(cvs,"Root"),cvsroot))
                return false;
            if(branch!=null) {
                if(!checkContents(new File(cvs,"Tag"),'T'+branch))
                    return false;
            } else {
                if(new File(cvs,"Tag").exists())
                    return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the contents of the file is equal to the given string.
     *
     * @return false in all the other cases.
     */
    private boolean checkContents(File file, String contents) {
        try {
            String s = new BufferedReader(new FileReader(file)).readLine();
            if(s==null)     return false;
            return s.trim().equals(contents.trim());
        } catch (IOException e) {
            return false;
        }
    }

    public boolean calcChangeLog( Build build, File changelogFile, BuildListener listener ) {
        if(build.getPreviousBuild()==null) {
            // nothing to compare against
            return createEmptyChangeLog(changelogFile,listener);
        }

        listener.getLogger().println("$ computing changelog");

        ChangeLogTask task = new ChangeLogTask() {
            {
                setOutputStream(System.out);
                setErrorStream(System.err);
            }
        };
        task.setProject(new Project());
        task.setDir(build.getProject().getWorkspace());
        if(DESCRIPTOR.getCvspassFile().length()!=0)
            task.setPassfile(new File(DESCRIPTOR.getCvspassFile()));
        task.setCvsRoot(cvsroot);
        task.setCvsRsh(cvsRsh);
        task.setPackage(module);
        task.setFailOnError(true);
        task.setDestfile(changelogFile);
        task.setStart(build.getPreviousBuild().getTimestamp().getTime());
        task.setEnd(build.getTimestamp().getTime());

        try {
            task.execute();
            return true;
        } catch( BuildException e ) {
            e.printStackTrace(listener.error(e.getMessage()));
            return false;
        } catch( RuntimeException e ) {
            // an user reported a NPE inside the changeLog task.
            // we don't want a bug in Ant to prevent a build.
            e.printStackTrace(listener.error(e.getMessage()));
            return true;    // so record the message but continue
        }
    }

    public SCMDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public void buildEnvVars(Map env) {
        if(cvsRsh!=null)
            env.put("CVS_RSH",cvsRsh);
        String cvspass = DESCRIPTOR.getCvspassFile();
        if(cvspass.length()!=0)
            env.put("CVS_PASSFILE",cvspass);
    }

    static final Descriptor DESCRIPTOR = new Descriptor();

    public static final class Descriptor extends SCMDescriptor {
        Descriptor() {
            super(CVSSCM.class);
        }

        public String getDisplayName() {
            return "CVS";
        }

        public SCM newInstance(HttpServletRequest req) {
            return new CVSSCM(
                req.getParameter("cvs_root"),
                req.getParameter("cvs_module"),
                req.getParameter("cvs_branch"),
                req.getParameter("cvs_rsh"),
                req.getParameter("cvs_use_update")!=null
            );
        }

        public String getCvspassFile() {
            String value = (String)getProperties().get("cvspass");
            if(value==null)
                value = "";
            return value;
        }

        public void setCvspassFile(String value) {
            getProperties().put("cvspass",value);
            save();
        }

        public boolean configure( HttpServletRequest req ) {
            setCvspassFile(req.getParameter("cvs_cvspass"));
            return true;
        }
    }

    /**
     * Action for a build that performs the tagging.
     */
    public final class TagAction implements Action {
        private final Build build;

        /**
         * If non-null, that means the build is already tagged.
         */
        private String tagName;

        /**
         * If non-null, that means the tagging is in progress
         * (asynchronously.)
         */
        private transient TagWorkerThread workerThread;

        public TagAction(Build build) {
            this.build = build;
        }

        public String getIconFileName() {
            return "Save.gif";
        }

        public String getDisplayName() {
            return "Tag this build";
        }

        public String getTagName() {
            return tagName;
        }

        public TagWorkerThread getWorkerThread() {
            return workerThread;
        }

        public Build getBuild() {
            return build;
        }

        public void doAction(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            req.setAttribute("build",build);
            req.getView(this,chooseAction()).forward(req,rsp);
        }

        private synchronized String chooseAction() {
            if(tagName!=null)
                return "alreadyTagged.jsp";
            if(workerThread!=null)
                return "inProgress.jsp";
            return "tagForm.jsp";
        }

        /**
         * Invoked to actually tag the workspace.
         */
        public synchronized void doSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            String name = req.getParameter("name");
            if(name==null || name.length()==0) {
                // invalid tag name
                doAction(req,rsp);
                return;
            }

            if(workerThread==null) {
                workerThread = new TagWorkerThread(name);
                workerThread.start();
            }

            doAction(req,rsp);
        }

        /**
         * Clears the error status.
         */
        public synchronized void doClearError(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            if(workerThread!=null && !workerThread.isAlive())
                workerThread = null;
            doAction(req,rsp);
        }

        public final class TagWorkerThread extends Thread {
            private final String tagName;
            // StringWriter is synchronized
            private final StringWriter log = new StringWriter();

            public TagWorkerThread(String tagName) {
                this.tagName = tagName;
            }

            public String getLog() {
                // this method can be invoked from another thread.
                return log.toString();
            }

            public String getTagName() {
                return tagName;
            }

            public void run() {
                BuildListener listener = new StreamBuildListener(log);

                Result result = Result.FAILURE;
                File destdir = null;
                listener.started();
                try {
                    destdir = Util.createTempDir();

                    // unzip the archive
                    listener.getLogger().println("expanding the workspace archive into "+destdir);
                    Expand e = new Expand();
                    e.setProject(new Project());
                    e.setDest(destdir);
                    e.setSrc(getArchiveFile(build));
                    e.setTaskType("unzip");
                    e.execute();

                    // run cvs tag command
                    listener.getLogger().println("tagging the workspace");
                    StringTokenizer tokens = new StringTokenizer(CVSSCM.this.module);
                    while(tokens.hasMoreTokens()) {
                        String m = tokens.nextToken();
                        if(!CVSSCM.this.run("cvs tag -R \""+tagName+"\"",listener,new File(destdir,m))) {
                            listener.getLogger().println("tagging failed");
                            return;
                        }
                    }

                    // completed successfully
                    synchronized(TagAction.this) {
                        TagAction.this.tagName = this.tagName;
                        TagAction.this.workerThread = null;
                    }
                    build.save();
                    
                } catch (Throwable e) {
                    e.printStackTrace(listener.fatalError(e.getMessage()));
                } finally {
                    try {
                        if(destdir!=null) {
                            listener.getLogger().println("cleaning up "+destdir);
                            Util.deleteRecursive(destdir);
                        }
                    } catch (IOException e) {
                        e.printStackTrace(listener.fatalError(e.getMessage()));
                    }
                    listener.finished(result);
                }
            }
        }
    }
}
