package hudson.plugins.release;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * String based parameter that is used in conjunction with the release plugin so that 
 * the release plugin know which parameter contains the release version information.
 * 
 * @author Peter Hayes
 * @since 1.6
 * @see {@link ParameterDefinition}
 */
public class ReleaseStringParameterDefinition extends ParameterDefinition {
	private static final long serialVersionUID = 1L;

    @DataBoundConstructor
    public ReleaseStringParameterDefinition(String name, String description) {
        super(name, description);
    }
    
    public ParameterValue getDefaultParameterValue() {
        ReleaseStringParameterValue v = new ReleaseStringParameterValue(getName(), "", getDescription());
        return v;
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return "Release String Parameter";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/release/help-releaseParameter.html";
        }
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        ReleaseStringParameterValue value = req.bindJSON(ReleaseStringParameterValue.class, jo);
        value.setDescription(getDescription());
        return value;
    }

	@Override
	public ParameterValue createValue(StaplerRequest req) {
        String[] value = req.getParameterValues(getName());
        if (value == null) {
        	return getDefaultParameterValue();
        } else if (value.length != 1) {
        	throw new IllegalArgumentException("Illegal number of parameter values for " + getName() + ": " + value.length);
        } else 
        	return new ReleaseStringParameterValue(getName(), value[0], getDescription());
	}
}
