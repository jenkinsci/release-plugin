package hudson.model;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.basic.AbstractBasicConverter;

/**
 * The build outcome.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Result {
    /**
     * The build didn't have any fatal errors not errors.
     */
    public static final Result SUCCESS = new Result("SUCCESS");
    /**
     * The build didn't have any fatal errors but some errors.
     */
    public static final Result UNSTABLE = new Result("UNSTABLE");
    /**
     * The build had a fatal error.
     */
    public static final Result FAILURE = new Result("FAILURE");
    /**
     * The build was manually aborted.
     */
    public static final Result ABORTED = new Result("ABORTED");

    private final String name;

    private Result(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    private static final Result[] all = new Result[] {SUCCESS,UNSTABLE,FAILURE,ABORTED};

    public static final Converter conv = new AbstractBasicConverter () {
        public boolean canConvert(Class clazz) {
            return clazz==Result.class;
        }

        protected Object fromString(String s) {
            for(int i=0;i<all.length;i++)
                if(s.equals(all[i].name))
                    return all[i];
            return FAILURE;
        }
    };
}
