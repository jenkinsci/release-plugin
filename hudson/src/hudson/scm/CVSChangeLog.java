package hudson.scm;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory representation of CVS Changelog.
 *
 * @author Kohsuke Kawaguchi
 */
public class CVSChangeLog {
    private String date;
    private String time;
    private String author;
    private String msg;
    private int index;
    private final List files = new ArrayList();

    /**
     * Checks if two {@link CVSChangeLog} entries can be merged.
     * This is to work around the duplicate entry problems.
     */
    public boolean canBeMergedWith(CVSChangeLog that) {
        if(!this.date.equals(that.date))
            return false;
        if(!this.time.equals(that.time))    // TODO: perhaps check this loosely?
            return false;
        if(!this.author.equals(that.author))
            return false;
        if(!this.msg.equals(that.msg))
            return false;
        return true;
    }

    public void merge(CVSChangeLog that) {
        this.files.addAll(that.files);
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getMsg() {
        return msg;
    }

    /**
     * Message escaped for HTML
     */
    public String getMsgEscaped() {
        StringBuffer buf = new StringBuffer();
        for( int i=0; i<msg.length(); i++ ) {
            char ch = msg.charAt(i);
            if(ch=='\n')
                buf.append("<br>");
            else
            if(ch=='<')
                buf.append("&lt;");
            else
            if(ch=='&')
                buf.append("&amp;");
            else
            if(ch==' ')
                buf.append("&nbsp;");
            else
                buf.append(ch);
        }
        return buf.toString();
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void addFile( File f ) {
        files.add(f);
    }

    public List getFiles() {
        return files;
    }

    public int getIndex() {
        return index;
    }

    public static class File {
        private String name;
        private String revision;
        private String prevrevision;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRevision() {
            return revision;
        }

        public void setRevision(String revision) {
            this.revision = revision;
        }

        public String getPrevrevision() {
            return prevrevision;
        }

        public void setPrevrevision(String prevrevision) {
            this.prevrevision = prevrevision;
        }

        public EditType getEditType() {
            if( prevrevision==null )
                return EditType.ADD;
            if( revision==null )
                return EditType.DELETE;

            return EditType.EDIT;
        }
    }

    public static final class EditType {
        private String name;
        private String description;

        public EditType(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public static final EditType ADD = new EditType("add","The file was added");
        public static final EditType EDIT = new EditType("edit","The file was modified");
        public static final EditType DELETE = new EditType("delete","The file was removed");
    }


    public static CVSChangeLog[] parse( java.io.File f ) throws IOException, SAXException {
        if(!f.exists())
            return new CVSChangeLog[0];

        Digester digester = new Digester();
        ArrayList r = new ArrayList();
        digester.push(r);

        digester.addObjectCreate("*/entry",CVSChangeLog.class);
        digester.addBeanPropertySetter("*/entry/date");
        digester.addBeanPropertySetter("*/entry/time");
        digester.addBeanPropertySetter("*/entry/author");
        digester.addBeanPropertySetter("*/entry/msg");
        digester.addSetNext("*/entry","add");

        digester.addObjectCreate("*/entry/file",File.class);
        digester.addBeanPropertySetter("*/entry/file/name");
        digester.addBeanPropertySetter("*/entry/file/revision");
        digester.addBeanPropertySetter("*/entry/file/prevrevision");
        digester.addSetNext("*/entry/file","addFile");

        digester.parse(f);

        // merge duplicate entries. Ant task somehow seems to report duplicate entries.
        for(int i=r.size()-1; i>=0; i--) {
            CVSChangeLog log = (CVSChangeLog)r.get(i);
            boolean merged = false;
            for(int j=0;j<i;j++) {
                CVSChangeLog c = (CVSChangeLog) r.get(j);
                if(c.canBeMergedWith(log)) {
                    c.merge(log);
                    merged = true;
                    break;
                }
            }
            if(merged)
                r.remove(log);
        }

        CVSChangeLog[] ar = (CVSChangeLog[]) r.toArray(new CVSChangeLog[r.size()]);
        for( int i=0; i<ar.length; i++ )
            ar[i].index = i;
        return ar;
    }
}
