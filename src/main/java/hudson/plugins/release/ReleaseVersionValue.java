package hudson.plugins.release;

/**
 * Interface used by the release plugin to recognize a parameter value that 
 * can be used to extract the version of the release build.
 * 
 * @author Peter Hayes
 */
public interface ReleaseVersionValue {

	/**
	 * Get the release version
	 * 
	 * @return The release version specified
	 */
	public String getReleaseVersion();
	
}
