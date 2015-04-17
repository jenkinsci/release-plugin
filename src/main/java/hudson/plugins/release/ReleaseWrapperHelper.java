/*
 * 
 */
package hudson.plugins.release;

import hudson.Functions;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import java.util.LinkedList;
import java.util.List;

/**
 * Helper class
 */
public class ReleaseWrapperHelper
{
    /** Retrieve a list of all build steps including publishers and builders 
     * 
     * @param project The project
     * @return A list of descriptor's for both publishers and builders
     */
    static public List<Descriptor> getBuildSteps(AbstractProject<?,?> project)
    {
        List<Descriptor<Publisher>> publishers = Functions.getPublisherDescriptors(project);
        List<Descriptor<Builder>> builders = Functions.getBuilderDescriptors(project);
        
        List<Descriptor> descriptors = new LinkedList<Descriptor>();
        descriptors.addAll(publishers);
        descriptors.addAll(builders);
        
        return descriptors;
    }
}
