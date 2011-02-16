package hudson.plugins.release;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
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
import hudson.model.Result;
import hudson.model.StringParameterValue;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Builder;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;

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
	private static final String DEFAULT_RELEASE_VERSION_TEMPLATE = "Release #$RELEASE_VERSION";
	
	private String releaseVersionTemplate;
        private boolean doNotKeepLog;
	private List<ParameterDefinition> parameterDefinitions = new ArrayList<ParameterDefinition>();
    private List<Builder> preBuildSteps = new ArrayList<Builder>();
    private List<Builder> postBuildSteps = new ArrayList<Builder>();
    private List<Builder> postSuccessfulBuildSteps = new ArrayList<Builder>();
    private List<Builder> postFailedBuildSteps = new ArrayList<Builder>();
    
    /**
     * @stapler-constructor
     */
    public ReleaseWrapper() {
    }
    
    public String getReleaseVersionTemplate() {
		return releaseVersionTemplate;
	}
    
    public void setReleaseVersionTemplate(String releaseVersionTemplate) {
		this.releaseVersionTemplate = releaseVersionTemplate;
	}

    public boolean isDoNotKeepLog() {
        return doNotKeepLog;
    }
    
    public void setDoNotKeepLog(boolean doNotKeepLog) {
        this.doNotKeepLog = doNotKeepLog;
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

    public List<Builder> getPostSuccessfulBuildSteps() {
        return postSuccessfulBuildSteps;
    }

    public void setPostSuccessfulBuildSteps(List<Builder> postSuccessfulBuildSteps) {
        this.postSuccessfulBuildSteps = postSuccessfulBuildSteps;
    }

    public List<Builder> getPostFailedBuildSteps() {
        return postFailedBuildSteps;
    }

    public void setPostFailedBuildSteps(List<Builder> postFailedBuildSteps) {
        this.postFailedBuildSteps = postFailedBuildSteps;
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject job) {
        return Collections.singletonList(new ReleaseAction(job));
    }
    
    @Override
    public Environment setUp(AbstractBuild build, final Launcher launcher, BuildListener listener) throws IOException,
            InterruptedException {

        final ReleaseBuildBadgeAction releaseBuildBadge = build.getAction(ReleaseBuildBadgeAction.class);
        
        if (releaseBuildBadge == null) {
        	return new Environment() { };
        }
        
        // Set the release version now by resolving build parameters against build and release version template
        ParametersAction parametersAction = build.getAction(ParametersAction.class);
        if (parametersAction != null) {
	        // set up variable resolver from parameters action
        	VariableResolver<String> resolver = createVariableResolver(parametersAction, build);
	        
        	// resolve template against resolver
        	String releaseVersion = Util.replaceMacro(releaseVersionTemplate != null && !"".equals(releaseVersionTemplate) ? releaseVersionTemplate : DEFAULT_RELEASE_VERSION_TEMPLATE, resolver);

        	// replace environment variables with actual values
        	EnvVars env = build.getEnvironment(listener);
        	releaseVersion = env.expand(releaseVersion);

        	// if release version is same as original, then blank it out
        	if (DEFAULT_RELEASE_VERSION_TEMPLATE.equals(releaseVersion)) {
        		releaseVersion = null;
        	}
        	
        	releaseBuildBadge.releaseVersion = releaseVersion;
        }
        
        if (!executeBuildSteps(preBuildSteps, build, launcher, listener)) {
            throw new IOException("Could not execute pre-build steps");
        }
        
        // return environment
        return new Environment() {
        	
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException,
                    InterruptedException {
                boolean shouldContinue = false;

                try {
                    Result result = build.getResult();
                    if (result == null || result.isBetterOrEqualTo(Result.UNSTABLE)) {
                        // save build
                        if (!doNotKeepLog) {
                            build.keepLog();
                        }

                        // set description if we can derive version
                        if (releaseBuildBadge.getReleaseVersion() != null) {

                                // set build description to indicate release
                                build.setDescription(releaseBuildBadge.getReleaseVersion());
                        }

                        shouldContinue = executeBuildSteps(postSuccessfulBuildSteps, build, launcher, listener);
                    } else {
                        shouldContinue = executeBuildSteps(postFailedBuildSteps, build, launcher, listener);
                    }
                } finally {
                    if (shouldContinue) {
                        shouldContinue = executeBuildSteps(postBuildSteps, build, launcher, listener);
                    }
                }

                return shouldContinue;
            }
        };
    }

    /*
     * Copied method from ParametersAction to reverse order of resolvers
     * per HUDSON-5094
     */
    private VariableResolver<String> createVariableResolver(ParametersAction parametersAction, AbstractBuild<?,?> build) {
        VariableResolver[] resolvers = new VariableResolver[parametersAction.getParameters().size()+1];
        int i=0;
        for (ParameterValue p : parametersAction.getParameters())
            resolvers[i++] = p.createVariableResolver(build);

        resolvers[i] = build.getBuildVariableResolver();

        ArrayUtils.reverse(resolvers);

        return new VariableResolver.Union<String>(resolvers);
    }
    
    private boolean executeBuildSteps(List<Builder> buildSteps, AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        boolean shouldContinue = true;
        
        // execute prebuild steps, stop processing if indicated
        if (buildSteps != null) {
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
            instance.releaseVersionTemplate = formData.getString("releaseVersionTemplate");
            instance.doNotKeepLog = formData.getBoolean("doNotKeepLog");
            instance.parameterDefinitions = Descriptor.newInstancesFromHeteroList(req, formData, "parameters", ParameterDefinition.all());
            instance.preBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "preBuildSteps", Builder.all());
            instance.postBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "postBuildSteps", Builder.all());
            instance.postSuccessfulBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "postSuccessfulBuildSteps", Builder.all());
            instance.postFailedBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "postFailedBuildSteps", Builder.all());
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
        	return ReleaseWrapper.hasReleasePermission(project) ? "package.gif" : null;
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
        
        /**
         * @return A map of previous build numbers and release version identifiers
         */
        public List<AbstractBuild> getPreviousReleaseBuilds() {
        	List<AbstractBuild> previousReleaseBuilds = new ArrayList<AbstractBuild>();
            
            for (Iterator iter = project.getBuilds().iterator(); iter.hasNext(); ) {
                AbstractBuild build = (AbstractBuild) iter.next();
                
                ReleaseBuildBadgeAction badge = build.getAction(ReleaseBuildBadgeAction.class);
                
                if (badge != null) {
                    previousReleaseBuilds.add(build);
                }
            }

            return previousReleaseBuilds;
        }

        public String getReleaseVersionForBuild(AbstractBuild build) {
            ReleaseBuildBadgeAction badge = build.getAction(ReleaseBuildBadgeAction.class);

            return badge.getReleaseVersion();
        }

        public List<ParameterValue> getParametersForBuild(AbstractBuild build) {
            ParametersAction parameters = build.getAction(ParametersAction.class);

            if (parameters != null) {
                return parameters.getParameters();
            }

            return Collections.emptyList();
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
	                
	                ParameterValue value = d.createValue(req, jo);
	                
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
            		new ReleaseBuildBadgeAction(), 
            		new ParametersAction(paramValues))) {
            	// TODO redirect to error page?
            }
            
            // redirect to status page
            resp.sendRedirect(project.getAbsoluteUrl());
        }

    }
    
    public static class ReleaseBuildBadgeAction implements BuildBadgeAction {
        private String releaseVersion;
        
        public ReleaseBuildBadgeAction() {
        }
        
        public String getReleaseVersion() {
            return releaseVersion;
        }
        
        public String getIconFileName() { return null; }
        public String getDisplayName() { return null; }
        public String getUrlName() { return null; }
    }
}
