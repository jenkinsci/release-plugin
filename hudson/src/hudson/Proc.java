package hudson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public final class Proc {
    private final Process proc;

    public Proc(String cmd,Map env, OutputStream out, File workDir) throws IOException {
        this(cmd,mapToEnv(env),out,workDir);
    }

    public Proc(String[] cmd,Map env,InputStream in, OutputStream out) throws IOException {
        this(cmd,mapToEnv(env),in,out);
    }

    private static String[] mapToEnv(Map<?,?> m) {
        String[] r = new String[m.size()];
        int idx=0;

        for (final Map.Entry e : m.entrySet()) {
            r[idx++] = e.getKey().toString() + '=' + e.getValue().toString();
        }
        return r;
    }

    public Proc(String cmd,String[] env,OutputStream out, File workDir) throws IOException {
        this( Runtime.getRuntime().exec(cmd,env,workDir), null, out );
    }

    public Proc(String[] cmd,String[] env,OutputStream out, File workDir) throws IOException {
        this( Runtime.getRuntime().exec(cmd,env,workDir), null, out );
    }

    public Proc(String[] cmd,String[] env,InputStream in,OutputStream out) throws IOException {
        this( Runtime.getRuntime().exec(cmd,env), in, out );
    }

    private Proc( Process proc, InputStream in, OutputStream out ) throws IOException {
        this.proc = proc;
        new Copier(proc.getInputStream(),out).start();
        new Copier(proc.getErrorStream(),out).start();
        if(in!=null)
            new ByteCopier(in,proc.getOutputStream()).start();
        else
            proc.getOutputStream().close();
    }

    public int join() {
        try {
            return proc.waitFor();
        } catch (InterruptedException e) {
            return -1;
        }
    }

    private static class Copier extends Thread {
        private final InputStream in;
        private final OutputStream out;

        public Copier(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        public void run() {
            try {
                Util.copyStream(in,out);
                in.close();
            } catch (IOException e) {
                ; // TODO: what to do?
            }
        }
    }

    private static class ByteCopier extends Thread {
        private final InputStream in;
        private final OutputStream out;

        public ByteCopier(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        public void run() {
            try {
                while(true) {
                    int ch = in.read();
                    if(ch==-1)  break;
                    out.write(ch);
                }
                in.close();
                out.close();
            } catch (IOException e) {
                ; // TODO: what to do?
            }
        }
    }
}
