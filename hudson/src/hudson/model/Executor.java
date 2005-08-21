package hudson.model;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;


/**
 * Thread that executes builds.
 *
 * @author Kohsuke Kawaguchi
 */
public class Executor extends Thread {
    private final Hudson owner;
    private final Queue queue;

    private Build build;

    private long startTime;

    public Executor(Hudson owner) {
        this.owner = owner;
        this.queue = owner.getQueue();
        start();
    }

    public void run() {
        while(true) {
            synchronized(owner) {
                if(owner.isShuttingDown())
                    return;
                if(owner.getNumExecutors()<owner.getExecutors().size()) {
                    // we've got too many executors.
                    owner.getExecutors().remove(this);
                    return;
                }
            }
            
            try {
                Project p = queue.pop();
                build = p.newBuild();
            } catch (InterruptedException e) {
                continue;
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            startTime = System.currentTimeMillis();
            build.run();
            build = null;
        }
    }

    /**
     * Returns the current {@link Build} this executor is running.
     *
     * @return
     *      null if the executor is idle.
     */
    public Build getCurrentBuild() {
        return build;
    }

    /**
     * Returns the progress of the current build in the number between 0-100.
     *
     * @return -1
     *      if it's impossible to estimate the progress.
     */
    public int getProgress() {
        Build b = build.getProject().getLastSuccessfulBuild();
        if(b==null)     return -1;

        long duration = b.getDuration();
        if(duration==0) return -1;

        int num = (int)((System.currentTimeMillis()-startTime)*100/duration);
        if(num>=100)    num=99;
        return num;
    }

    /**
     * Returns the image that shows the current buildCommand status.
     */
    public void doStop( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        interrupt();
        rsp.sendRedirect(req.getContextPath());
    }
}
