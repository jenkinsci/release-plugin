package hudson.scm;

import hudson.Proc;
import hudson.Util;
import hudson.model.Build;
import hudson.model.BuildListener;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Subversion.
 *
 * @author Kohsuke Kawaguchi
 */
public class SubversionSCM extends AbstractCVSFamilySCM {
    private final String modules;
    private boolean useUpdate;

    SubversionSCM( String modules, boolean useUpdate ) {
        this.modules = modules;
        this.useUpdate = useUpdate;
    }

    public String getModules() {
        return modules;
    }

    public boolean isUseUpdate() {
        return useUpdate;
    }

    private Collection<String> getModuleDirNames() {
        List<String> dirs = new ArrayList<String>();
        StringTokenizer tokens = new StringTokenizer(modules);
        while(tokens.hasMoreTokens()) {
            dirs.add(getLastPathComponent(tokens.nextToken()));
        }
        return dirs;
    }

    public boolean calcChangeLog(Build build, File changelogFile, BuildListener listener) throws IOException {
        if(build.getPreviousBuild()==null) {
            // nothing to compare against
            return createEmptyChangeLog(changelogFile, listener);
        }

        PrintStream logger = listener.getLogger();

        Map<String,String> previousRevisions = new HashMap<String,String>(); // module -> revision
        {// read the revision file of the last build
            File file = getRevisionFile(build.getPreviousBuild());
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while((line=br.readLine())!=null) {
                int index = line.indexOf('/');
                if(index<0) {
                    logger.println("Unable to parse the line: "+line);
                    continue;   // invalid line?
                }
                previousRevisions.put(line.substring(0,index), line.substring(index+1));
            }
        }

        Map env = createEnvVarMap();

        for( String module : getModuleDirNames() ) {
            String prevRev = previousRevisions.get(module);
            if(prevRev==null) {
                logger.println("no revision recorded for "+module+" in the previous build");
                continue;
            }

            String cmd = DESCRIPTOR.getSvnExe()+" log --xml --non-interactive -r "+prevRev+":BASE "+module;
            logger.println("$ "+cmd);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int r = new Proc(cmd,env,baos,build.getProject().getWorkspace()).join();
            if(r!=0) {
                listener.fatalError("revision check failed");
                return false;
            }

            // TODO: changelog format conversion
        }

        return true;
    }

    public boolean checkout(Build build, File dir, BuildListener listener) throws IOException {
        boolean result;

        if(useUpdate && isUpdatable(dir))
            result = update(dir,listener);
        else {
            Util.deleteContentsRecursive(dir);
            result = run(DESCRIPTOR.getSvnExe()+" co -q  "+modules,listener,dir);
        }

        if(!result)
            return false;

        PrintStream logger = listener.getLogger();

        {// record the current revision
            PrintWriter w = new PrintWriter(new FileOutputStream(getRevisionFile(build)));

            Map env = createEnvVarMap();

            // invoke the "svn info"
            for( String module : getModuleDirNames() ) {
                String cmd = DESCRIPTOR.getSvnExe()+" info "+module;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                logger.println("$ "+cmd);
                int r = new Proc(cmd,env,baos,dir).join();
                if(r!=0) {
                    listener.fatalError("revision check failed");
                    return false;
                }

                // look for the revision line
                BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
                String line;
                while((line=br.readLine())!=null) {
                    if(line.startsWith("Revision:"))
                        break;
                }
                if(line==null) {
                    listener.fatalError("no revision in the svn info output");
                    return false;
                }

                w.println( module +'/'+ line.substring("Revision: ".length()) );
                logger.println(line);
            }

            w.close();
        }
        return true;
    }

    /**
     * Gets the file that stores the revision.
     */
    private File getRevisionFile(Build build) {
        return new File(build.getRootDir(),"revision.txt");
    }

    public boolean update(File dir, BuildListener listener) throws IOException {
        String cmd = DESCRIPTOR.getSvnExe()+" update -q --non-interactive";
        StringTokenizer tokens = new StringTokenizer(modules);
        while(tokens.hasMoreTokens()) {
            if(!run(cmd,listener,new File(dir,getLastPathComponent(tokens.nextToken()))))
                return false;
        }
        return true;
    }

    /**
     * Returns true if we can use "cvs update" instead of "cvs checkout"
     */
    private boolean isUpdatable(File dir) {
        StringTokenizer tokens = new StringTokenizer(modules);
        while(tokens.hasMoreTokens()) {
            String url = tokens.nextToken();
            File module = new File(dir,getLastPathComponent(url));
            File svn = new File(module,".svn/entries");
            if(!svn.exists())
                return false;

            // check wc-entries/entry/@url
            synchronized(spf) {
                try {
                    SAXParser parser = spf.newSAXParser();
                    Checker checker = new Checker(url);
                    parser.parse(svn,checker);
                    if(!checker.found())
                        return false;
                } catch (ParserConfigurationException e) {
                    // impossible
                    throw new Error(e);
                } catch (SAXException e) {
                    // corrupt file? don't use update to be safe
                    return false;
                } catch (IOException e) {
                    // corrupt file? don't use update to be safe
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Looks for /wc-entries/entry/@url and see if it matches the expected URL.
     */
    private static final class Checker extends DefaultHandler {
        private final String url;

        private boolean matched = false;

        public Checker(String url) {
            this.url = url;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if(!qName.equals("entry"))
                return;

            String n = attributes.getValue("name");
            if(n==null || n.length()>0)     return;

            String url = attributes.getValue("url");
            if(url!=null && url.equals(this.url))
                matched = true;
        }

        public boolean found() {
            return matched;
        }
    }




    public SCMDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public void buildEnvVars(Map env) {
        // no environment variable
    }

    public String getModule() {
        String s;

        // if multiple URLs are specified, pick the first one
        int idx = modules.indexOf(' ');
        if(idx>=0)  s = modules.substring(0,idx);
        else        s = modules;

        return getLastPathComponent(s);
    }

    private String getLastPathComponent(String s) {
        String[] tokens = s.split("/");
        return tokens[tokens.length-1]; // return the last token
    }

    /**
     * Shared instance. Note that it is not namespace aware.
     */
    static final SAXParserFactory spf = SAXParserFactory.newInstance();

    static final Descriptor DESCRIPTOR = new Descriptor();

    public static final class Descriptor extends SCMDescriptor {
        Descriptor() {
            super(SubversionSCM.class);
        }

        public String getDisplayName() {
            return "Subversion";
        }

        public SCM newInstance(HttpServletRequest req) {
            return new SubversionSCM(
                req.getParameter("svn_modules"),
                req.getParameter("svn_use_update")!=null
            );
        }

        public String getSvnExe() {
            String value = (String)getProperties().get("svn_exe");
            if(value==null)
                value = "svn";
            return value;
        }

        public void setSvnExe(String value) {
            getProperties().put("svn_exe",value);
            save();
        }

        public boolean configure( HttpServletRequest req ) {
            setSvnExe(req.getParameter("svn_exe"));
            return true;
        }
    }
}
