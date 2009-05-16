package hudson.plugins.release;

import hudson.Extension;
import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildBadgeAction;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterValue;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Wraps a build with pre and post build steps.  These steps can take
 * any action as part of the special release build.
 *
 * @author Peter Hayes
 * @since 1.0
 */
public class ReleaseWrapper extends BuildWrapper {
	private List<ParameterDefinition> parameterDefinitions = new ArrayList<ParameterDefinition>();
    private List<Builder> preBuildSteps = new ArrayList<Builder>();
    private List<Builder> postBuildSteps = new ArrayList<Builder>();
    
    /**
     * @stapler-constructor
     */
    public ReleaseWrapper() {
    }
    
    public List<ParameterDefinition> getParameterDefinitions() {
		return parameterDefinitions;
	}
    
    public void setParameterDefinitions(List<ParameterDefinition> parameterDefinitions) {
		this.parameterDefinitions = parameterDefinitions;
	}
    
    /**
     * @return Returns the preBuildSteps.
     */
    public List<Builder> getPreBuildSteps() {
        return preBuildSteps;
    }
    
    /**
     * @param preBuildSteps The preBuildSteps to set.
     */
    public void setPreBuildSteps(List<Builder> preBuildSteps) {
        this.preBuildSteps = preBuildSteps;
    }
    
    /**
     * @return Returns the postBuildSteps.
     */
    public List<Builder> getPostBuildSteps() {
        return postBuildSteps;
    }
    
    /**
     * @param postBuildSteps The postBuildSteps to set.
     */
    public void setPostBuildSteps(List<Builder> postSuccessBuildSteps) {
        this.postBuildSteps = postSuccessBuildSteps;
    }
    
    @Override
    public Action getProjectAction(AbstractProject job) {
        return new ReleaseAction(job);
    }
    
    @Override
    public Environment setUp(AbstractBuild build, final Launcher launcher, BuildListener listener) throws IOException,
            InterruptedException {

        final ReleaseBuildBadgeAction releaseBuildBadge = build.getAction(ReleaseBuildBadgeAction.class);
        
        if (releaseBuildBadge == null) {
        	return new Environment() { };
        }
        
        if (!executeBuildSteps(preBuildSteps, build, launcher, listener)) {
            throw new IOException("Could not execute pre-build steps");
        }
        
        // return environment
        return new Environment() {
        	
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException,
                    InterruptedException {
                
                // save build
                build.keepLog();
                
                // only set description if we can derive version
                if (releaseBuildBadge.getReleaseVersion() != null) {
                
	                // set build description to indicate release
	                build.setDescription("Release # " + releaseBuildBadge.getReleaseVersion());
                }
	                
                return executeBuildSteps(postBuildSteps, build, launcher, listener);
            }
        };
    }
    
    private boolean executeBuildSteps(List<Builder> buildSteps, AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        boolean shouldContinue = true;
        
        // execute prebuild steps, stop processing if indicated
        for (BuildStep buildStep : buildSteps) {
            
            if (!shouldContinue) {
                break;
            }
            
            shouldContinue = buildStep.prebuild(build, listener);
        }
        
        // execute build step, stop processing if indicated
        for (BuildStep buildStep : buildSteps) {
            
            if (!shouldContinue) {
                break;
            }
            
            shouldContinue = buildStep.perform(build, launcher, listener);
        }
        
        return shouldContinue;
    }
    
	public static boolean hasReleasePermission(AbstractProject job) {
		return job.hasPermission(Item.BUILD);
	}

	public static void checkReleasePermission(AbstractProject job) {
		job.checkPermission(Item.BUILD);
	}
    
	@Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        
        @Override
        public String getDisplayName() {
            return "Configure release build";
        }
        
        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            ReleaseWrapper instance = new ReleaseWrapper();
            instance.parameterDefinitions = Descriptor.newInstancesFromHeteroList(req, formData, "parameters", ParameterDefinition.all());
            instance.preBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "preBuildSteps", Builder.all());
            instance.postBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "postBuildSteps", Builder.all());
            return instance;
        }
        
        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return FreeStyleProject.class.isInstance(item) || MavenModuleSet.class.isInstance(item);
        }
    }
    
    public class ReleaseAction implements Action {
        private AbstractProject project;
        private String releaseVersion;
        private String developmentVersion;    
        
        public ReleaseAction(AbstractProject project) {
            this.project = project;
        }
        
        public List<ParameterDefinition> getParameterDefinitions() {
        	return parameterDefinitions;
        }

        /**
         * {@inheritDoc}
         */
        public String getDisplayName() {
            return "Release";
        }

        /**
         * {@inheritDoc}
         */
        public String getIconFileName() {
        	if (ReleaseWrapper.hasReleasePermission(project)) {
        		return "package.gif";
        	}
        	
        	return null;
        }

        /**
         * {@inheritDoc}
         */
        public String getUrlName() {
            return "release";
        }
        
        /**
         * @return Returns the project.
         */
        public AbstractProject getProject() {
            return project;
        }
        
        public List<String> getPreviousReleaseVersions() {
            LinkedList<String> previousReleaseVersions = new LinkedList<String>();
            
            for (Iterator iter = project.getBuilds().iterator(); iter.hasNext(); ) {
                AbstractBuild build = (AbstractBuild) iter.next();
                
                ReleaseBuildBadgeAction badge = build.getAction(ReleaseBuildBadgeAction.class);
                
                if (badge != null && badge.getReleaseVersion() != null) {
                    previousReleaseVersions.add(badge.getReleaseVersion());
                }
            }
            
            return previousReleaseVersions;
        }
        
        public String getReleaseVersion() {
            return releaseVersion;
        }
        
        public void setReleaseVersion(String releaseVersion) {
            this.releaseVersion = releaseVersion;
        }
        
        public String getDevelopmentVersion() {
            return developmentVersion;
        }
        
        public void setDevelopmentVersion(String developmentVersion) {
            this.developmentVersion = developmentVersion;
        }

        /**
         * Gets the {@link ParameterDefinition} of the given name, if any.
         */
        public ParameterDefinition getParameterDefinition(String name) {
        	if (parameterDefinitions == null) {
        		return null;
        	}
        	
            for (ParameterDefinition pd : parameterDefinitions)
                if (pd.getName().equals(name))
                    return pd;
            return null;
        }

        /*
         * TODO Would be nice if this method was accessible from AbstractProject
         */
        private List<ParameterValue> getDefaultParametersValues() {
            ParametersDefinitionProperty paramDefProp = (ParametersDefinitionProperty) project.getProperty(ParametersDefinitionProperty.class);
            ArrayList<ParameterValue> defValues = new ArrayList<ParameterValue>();
            
            /*
             * This check is made ONLY if someone will call this method even if isParametrized() is false.
             */
            if(paramDefProp == null)
                return defValues;
            
            /* Scan for all parameter with an associated default values */
            for(ParameterDefinition paramDefinition : paramDefProp.getParameterDefinitions())
            {
               ParameterValue defaultValue = paramDefinition.getDefaultParameterValue();
                
                if(defaultValue != null)
                    defValues.add(defaultValue);           
            }
            
            return defValues;
        }
        
        public void doSubmit(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        	// verify permission
        	ReleaseWrapper.checkReleasePermission(project);
        	
            // bind development / release version
            req.bindParameters(this);

            // create parameter list
            List<ParameterValue> paramValues = getDefaultParametersValues();
            
            if (getParameterDefinitions() != null && !getParameterDefinitions().isEmpty()) {
	            JSONObject formData = req.getSubmittedForm();
	            
	            JSONArray a = JSONArray.fromObject(formData.get("parameter"));
	
	            for (Object o : a) {
	                JSONObject jo = (JSONObject) o;
	                String name = jo.getString("name");
	
	                ParameterDefinition d = getParameterDefinition(name);
	                if(d==null)
	                    throw new IllegalArgumentException("No such parameter definition: " + name);
	                paramValues.add(d.createValue(req, jo));
	            }
            } else {
	            // add version if specified
	            if (releaseVersion != null && !"".equals(releaseVersion)) {
	            	paramValues.add(new StringParameterValue("RELEASE_VERSION", releaseVersion));
	            }
	            
	            if (developmentVersion != null && !"".equals(developmentVersion)) {
	            	paramValues.add(new StringParameterValue("DEVELOPMENT_VERSION", developmentVersion));
	            }
            }
            
            // schedule release build
            if (!project.scheduleBuild(0, new Cause.UserCause(), 
            		new ReleaseBuildBadgeAction(releaseVersion), 
            		new ParametersAction(paramValues))) {
            	// TODO redirect to error page?
            }
            
            // redirect to status page
            resp.sendRedirect(project.getAbsoluteUrl());
        }

    }
    
    public static class ReleaseBuildBadgeAction implements BuildBadgeAction {
        private String releaseVersion;
        
        public ReleaseBuildBadgeAction(String releaseVersion) {
            this.releaseVersion = releaseVersion;
        }
        
        public String getReleaseVersion() {
            return releaseVersion;
        }
        
        public String getIconFileName() { return null; }
        public String getDisplayName() { return null; }
        public String getUrlName() { return null; }
    }
}
