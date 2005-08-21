package hudson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Represents a Hudson plug-in.
 *
 * <p>
 * A plug-in is packaged into a jar file whose extension is ".hudson-plugin".
 * The file is structured much like a war file.
 *
 * <p>
 * At run-time, the code portion of it is extracted into a directory named
 * after its file name (except the extension), and the rest of the resources
 * including the side files, tag files, and so on will be extracted into the
 * same directory as the Hudson itself.
 *
 * <p>
 * When a plug-in file is expanded, Hudson also remembers what files came from
 * where, in a file called "index". This allows a plug-in to be uninstalled.
 *
 *
 * <p>
 * I couldn't attain the complete isolation between plugins because J2EE doesn't
 * allow JSP loading mechanism to be customized.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Plugin {
    /**
     * {@link ClassLoader} for loading plug-in code.
     */
    private ClassLoader classLoader;

    private final File archive;

    private final File explodeDir;

    private final PluginManager owner;

    /**
     * @param arc
     *      A .hudson-plugin archive file.
     */
    public Plugin(PluginManager owner, File arc) {
        this.owner = owner;
        this.archive = arc;
        String p = archive.getPath();
        explodeDir = new File(p.substring(0,p.length()-14));
    }

    /**
     * Makes the plug-in ready for use.
     */
    public void deploy() throws IOException {
        if(explodeDir.exists() && explodeDir.lastModified()<=archive.lastModified())
            deleteExploded();
        if(!explodeDir.exists())
            explode();
    }

    /**
     * Extracts the contents of the plug-in archive into the file system.
     */
    private void explode() throws IOException {
        // extract a new one
        PrintWriter index = new PrintWriter(getIndexFile());

        final byte[] buf = new byte[8192];

        ZipFile zip = new ZipFile(archive);
        Enumeration<? extends ZipEntry> entires = zip.entries();
        while(entires.hasMoreElements()) {
            ZipEntry entry = entires.nextElement();
            File f;
            if(isPluginLocal(entry))
                f = new File(explodeDir,entry.getName());
            else
                f = new File(owner.context.getRealPath("/"),entry.getName());
            if(f.isDirectory()) {
                f.mkdirs();
                continue;
            }
            f.getParentFile().mkdirs();

            index.println(f.getPath());
            InputStream in = zip.getInputStream(entry);
            OutputStream out = new FileOutputStream(f);
            int size;
            while((size=in.read(buf))>0) {
                out.write(buf,0,size);
            }
            in.close();
            out.close();
            f.setLastModified(entry.getTime());
        }

        index.close();
    }

    private File getIndexFile() {
        return new File(explodeDir.getPath()+".index");
    }

    /**
     * Deletes the exploded copies.
     */
    private void deleteExploded() throws IOException {
        try {
            Util.deleteRecursive(explodeDir);
        } catch (IOException e) {
            System.err.println("Unable to delete "+explodeDir);
            e.printStackTrace();
        }
        BufferedReader r = new BufferedReader(new FileReader(getIndexFile()));
        String fileName;
        while((fileName=r.readLine())!=null)
            new File(fileName).delete();
        r.close();
    }

    private boolean isPluginLocal(ZipEntry entry) {
        String name = entry.getName();
        return name.startsWith("WEB-INF/lib/") || name.startsWith("WEB-INF/classes/");
    }
}
