package hudson;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * {@link XStream} with helper methods.
 *
 * @author Kohsuke Kawaguchi
 */
public class XStreamEx extends XStream {
    public Object fromXML( File f ) throws IOException {
        return fromXML(new InputStreamReader(new FileInputStream(f),"UTF-8"));
    }

    public void toXML( Object o, File f ) throws IOException {
        Writer w = new OutputStreamWriter(new FileOutputStream(f),"UTF-8");
        w.write("<?xml version='1.0' encoding='UTF-8'?>\n");
        toXML(o,w);
        w.close();
    }
}
