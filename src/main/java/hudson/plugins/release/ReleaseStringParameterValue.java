package hudson.plugins.release;

import hudson.model.AbstractBuild;
import hudson.model.ParameterValue;
import hudson.util.VariableResolver;

import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

/**
 * {@link ParameterValue} created from {@link ReleaseStringParameterDefinition}.
 * 
 * @since 1.6
 */
public class ReleaseStringParameterValue extends ParameterValue implements ReleaseVersionValue {
	private static final long serialVersionUID = 1L;
	
	@Exported(visibility=4)
    public final String value;

    @DataBoundConstructor
    public ReleaseStringParameterValue(String name, String value) {
        this(name, value, null);
    }

    public ReleaseStringParameterValue(String name, String value, String description) {
        super(name, description);
        this.value = value;
    }
    
    public String getReleaseVersion() {
    	return value;
    }

    /**
     * Exposes the name/value as an environment variable.
     */
    @Override
    public void buildEnvVars(AbstractBuild<?,?> build, Map<String,String> env) {
        env.put(name.toUpperCase(),value);
    }

    @Override
    public VariableResolver<String> createVariableResolver(AbstractBuild<?, ?> build) {
        return new VariableResolver<String>() {
            public String resolve(String name) {
                return ReleaseStringParameterValue.this.name.equals(name) ? value : null;
            }
        };
    }
    

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReleaseStringParameterValue other = (ReleaseStringParameterValue) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
    public String toString() {
    	return "(ReleaseStringParameterValue) " + getName() + "='" + value + "'";
    }
}

