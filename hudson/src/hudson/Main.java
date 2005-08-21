package hudson;

import hudson.model.ExternalJob;
import hudson.model.ExternalRun;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point to Hudson from command line.
 *
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public static void main(String[] args) {
        try {
            System.exit(run(args));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static int run(String[] args) throws Exception {
        String home = Hudson.masterEnvVars.get("HUDSON_HOME");
        if(home==null) {
            System.err.println("HUDSON_HOME is not set.");
            return -1;
        }

        Hudson app = new Hudson(new File(home));

        Job job = app.getJob(args[0]);
        if(!(job instanceof ExternalJob)) {
            System.err.println(args[0]+" is not a valid external job name in Hudson");
            return -1;
        }
        ExternalJob ejob = (ExternalJob) job;

        ExternalRun run = ejob.newBuild();

        // run the command
        List<String> cmd = new ArrayList<String>();
        for( int i=1; i<args.length; i++ )
            cmd.add(args[i]);
        run.run(cmd.toArray(new String[cmd.size()]));

        return run.getResult()==Result.SUCCESS?0:1;
    }
}
