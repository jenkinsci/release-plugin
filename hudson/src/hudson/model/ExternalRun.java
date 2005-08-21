package hudson.model;

import hudson.Proc;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class ExternalRun extends Run<ExternalJob,ExternalRun> {
    /**
     * Loads a run from a log file.
     */
    ExternalRun(ExternalJob owner, File runDir, ExternalRun prevRun ) throws IOException {
        super(owner,runDir,prevRun);
    }

    /**
     * Creates a new run.
     */
    ExternalRun(ExternalJob project) throws IOException {
        super(project);
    }

    /**
     * Instead of performing a build, run the specified command,
     * record the log and its exit code, then call it a build.
     */
    public void run(final String[] cmd) {
        run(new Runner() {
            public Result run(BuildListener listener) throws Exception {
                Proc proc = new Proc(cmd,getEnvVars(),System.in,new DualOutputStream(System.out,listener.getLogger()));
                return proc.join()==0?Result.SUCCESS:Result.FAILURE;
            }

            public void post(BuildListener listener) {
                // do nothing
            }
        });
    }

    private static class DualOutputStream extends OutputStream {
        private final OutputStream lhs,rhs;

        public DualOutputStream(OutputStream lhs, OutputStream rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        public void write(int b) throws IOException {
            lhs.write(b);
            rhs.write(b);
        }

        public void write(byte[] b) throws IOException {
            lhs.write(b);
            rhs.write(b);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            lhs.write(b,off,len);
            rhs.write(b,off,len);
        }

        public void flush() throws IOException {
            lhs.flush();
            rhs.flush();
        }

        public void close() throws IOException {
            lhs.close();
            rhs.close();
        }
    }
}
