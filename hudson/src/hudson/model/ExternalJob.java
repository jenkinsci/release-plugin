package hudson.model;

import hudson.Util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Job that runs outside Hudson.
 *
 * @author Kohsuke Kawaguchi
 */
public class ExternalJob extends Job<ExternalJob,ExternalRun> {

    /**
     * We occasionally update the list of {@link Run}s from a file system.
     * The next scheduled update time.
     */
    private transient long nextUpdate = 0;

    private transient SortedMap<String,ExternalRun> runs = null;

    public ExternalJob(Hudson parent,String name) {
        super(parent,name);
        getBuildDir().mkdirs();
    }

    protected synchronized SortedMap<String,ExternalRun> _getRuns() {
        if(nextUpdate<System.currentTimeMillis()) {
            nextUpdate = System.currentTimeMillis()+1000;
            reload();
        }
        return runs;
    }

    public void removeRun(Run run) {
        // reload the info next time
        nextUpdate = 0;
    }

    private void reload() {
        runs = new TreeMap<String,ExternalRun>(reverseComparator);

        File[] subdirs = getBuildDir().listFiles(new FileFilter() {
            public boolean accept(File subdir) {
                return subdir.isDirectory();
            }
        });

        Arrays.sort(subdirs,fileComparator);
        ExternalRun lastBuild = null;

        for( File dir : subdirs ) {
            try {
                ExternalRun b = new ExternalRun(this,dir,lastBuild);
                lastBuild = b;
                runs.put( b.getId(), b );
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    Util.deleteRecursive(dir);
                } catch (IOException e1) {
                    e1.printStackTrace();
                    // but ignore
                }
            }
        }

    }


    /**
     * Creates a new build of this project for immediate execution.
     */
    public synchronized ExternalRun newBuild() throws IOException {
        ExternalRun run = new ExternalRun(this);
        _getRuns().put(run.getId(),run);
        return run;
    }

    private static final Comparator<File> fileComparator = new Comparator<File>() {
        public int compare(File lhs, File rhs) {
            return lhs.getName().compareTo(rhs.getName());
        }
    };
}
