/*
 * The MIT License
 *
 * Copyright (c) 2009-2011, Peter Hayes, Manufacture Francaise des Pneumatiques Michelin,
 * Romain Seguy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.release;

import hudson.DescriptorExtensionList;
import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.ivy.IvyModuleSet;
import hudson.matrix.MatrixRun;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildBadgeAction;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PermalinkProjectAction;
import hudson.model.PermalinkProjectAction.Permalink;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.plugins.release.promotion.ReleasePromotionCondition;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.PermissionScope;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Builder;
import hudson.util.VariableResolver;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixChildAction;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.ArrayUtils;
import org.jenkinsci.plugins.jobgenerator.JobGenerator;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Wraps a build with pre and post build steps.  These steps can take
 * any action as part of the special release build.
 *
 * @author Peter Hayes
 * @since 1.0
 */
public class ReleaseWrapper extends BuildWrapper implements MatrixAggregatable {

    public static final PermissionGroup PERMISSIONS = new PermissionGroup(ReleaseWrapper.class, Messages._ReleaseWrapper_PermissionsTitle());

    /**
     * Permission to trigger release builds.
     */
    public static final Permission RELEASE_PERMISSION = new Permission(PERMISSIONS,"Release",Messages._ReleaseWrapper_ReleasePermission_Description(),Jenkins.ADMINISTER, PermissionScope.ITEM);

    private static final String DEFAULT_RELEASE_VERSION_TEMPLATE = "Release #$RELEASE_VERSION";
	
    private String releaseVersionTemplate;
    private boolean doNotKeepLog;
    private boolean overrideBuildParameters;
    private List<ParameterDefinition> parameterDefinitions = new ArrayList<ParameterDefinition>();
    private List<BuildStep> preBuildSteps = new ArrayList<BuildStep>();
    private List<BuildStep> postBuildSteps = new ArrayList<BuildStep>();
    private List<BuildStep> postSuccessfulBuildSteps = new ArrayList<BuildStep>();
    private List<BuildStep> postFailedBuildSteps = new ArrayList<BuildStep>();
    private List<BuildStep> preMatrixBuildSteps = new ArrayList<BuildStep>();
    private List<BuildStep> postSuccessfulMatrixBuildSteps = new ArrayList<BuildStep>();
    private List<BuildStep> postFailedMatrixBuildSteps = new ArrayList<BuildStep>();
    private List<BuildStep> postMatrixBuildSteps = new ArrayList<BuildStep>();
    
    /**
     * List of {@link Permalink}s for release builds.
     */
    public static final List<Permalink> RELEASE = new CopyOnWriteArrayList<Permalink>();

    static {
        RELEASE.add(new Permalink() {
            public String getDisplayName() {
                return Messages.ReleaseWrapper_LastReleaseBuild();
            }

            public String getId() {
                return "lastReleaseBuild";
            }

            public Run<?,?> resolve(Job<?,?> job) {
            	for(Run<?,?> build : job.getBuilds()){
            		if(build.getAction(ReleaseBuildBadgeAction.class) != null){
            			return build;
            		}
            	}
                return null;
            }
        });
        RELEASE.add(new Permalink() {
            public String getDisplayName() {
                return Messages.ReleaseWrapper_LastSuccessfulReleaseBuild();
            }

            public String getId() {
                return "lastSuccessfulReleaseBuild";
            }

            public Run<?,?> resolve(Job<?,?> job) {
            	for(Run<?,?> build : job.getBuilds()){
            		if(build.getResult() == Result.SUCCESS && build.getAction(ReleaseBuildBadgeAction.class) != null){
            			return build;
            		}
            	}
                return null;
            }
        });
    }

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
    
    public boolean isOverrideBuildParameters() {
        return overrideBuildParameters;
    }
    
    public void setOverrideBuildParameters(boolean overrideBuildParameters) {
        this.overrideBuildParameters = overrideBuildParameters;
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
    public List<BuildStep> getPreBuildSteps() {
        return preBuildSteps;
    }
    
    /**
     * @param preBuildSteps The preBuildSteps to set.
     */
    public void setPreBuildSteps(List<BuildStep> preBuildSteps) {
        this.preBuildSteps = preBuildSteps;
    }
    
    /**
     * @return Returns the preMatrixBuildSteps.
     */
    public List<BuildStep> getPreMatrixBuildSteps() {
        return preMatrixBuildSteps;
    }

    public void setPreMatrixBuildSteps(List<BuildStep> preMatrixBuildSteps) {
        this.preMatrixBuildSteps = preMatrixBuildSteps;
    }
    
    /**
     * @return Returns the postBuildSteps.
     */
    public List<BuildStep> getPostBuildSteps() {
        return postBuildSteps;
    }

    public void setPostBuildSteps(List<BuildStep> postSuccessBuildSteps) {
        this.postBuildSteps = postSuccessBuildSteps;
    }
    
    /**
     * @return Returns the postMatrixBuildSteps.
     */
    public List<BuildStep> getPostMatrixBuildSteps() {
        return postMatrixBuildSteps;
    }
    
    /**
     * @param postMatrixBuildSteps The postMatrixBuildSteps to set.
     */
    public void setPostMatrixBuildSteps(List<BuildStep> postMatrixBuildSteps) {
        this.postMatrixBuildSteps = postMatrixBuildSteps;
    }

    public List<BuildStep> getPostSuccessfulBuildSteps() {
        return postSuccessfulBuildSteps;
    }

    public void setPostSuccessfulBuildSteps(List<BuildStep> postSuccessfulBuildSteps) {
        this.postSuccessfulBuildSteps = postSuccessfulBuildSteps;
    }

    public List<BuildStep> getPostFailedBuildSteps() {
        return postFailedBuildSteps;
    }

    public void setPostFailedBuildSteps(List<BuildStep> postFailedBuildSteps) {
        this.postFailedBuildSteps = postFailedBuildSteps;
    }

    public List<BuildStep> getPostSuccessfulMatrixBuildSteps() {
        return postSuccessfulMatrixBuildSteps;
    }

    public void setPostSuccessfulMatrixBuildSteps(List<BuildStep> postSuccessfulMatrixBuildSteps) {
        this.postSuccessfulMatrixBuildSteps = postSuccessfulMatrixBuildSteps;
    }

    public List<BuildStep> getPostFailedMatrixBuildSteps() {
        return postFailedMatrixBuildSteps;
    }

    public void setPostFailedMatrixBuildSteps(List<BuildStep> postFailedMatrixBuildSteps) {
        this.postFailedMatrixBuildSteps = postFailedMatrixBuildSteps;
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject job) {
        return Collections.singletonList(new ReleaseAction(job));
    }
    
    public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        ReleaseAggregator instance = new ReleaseAggregator(build, launcher, listener);
        instance.setPreMatrixBuildSteps(preMatrixBuildSteps);
        instance.setPostSuccessfulMatrixBuildSteps(postSuccessfulMatrixBuildSteps);
        instance.setPostFailedMatrixBuildSteps(postFailedMatrixBuildSteps);
        instance.setPostMatrixBuildSteps(postMatrixBuildSteps);
        return instance;
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
            throw new IOException(Messages.ReleaseWrapper_CouldNotExecutePreBuildSteps());
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

                                if (build instanceof MatrixRun) {
                                       // also set the description for the parent if the build is a matrix one
                                       ((MatrixRun) build).getParentBuild().setDescription(releaseBuildBadge.getReleaseVersion());
                                }
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
    
    private boolean executeBuildSteps(List<BuildStep> buildSteps, AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
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
		return job.hasPermission(RELEASE_PERMISSION) && !MatrixConfiguration.class.isInstance(job);
	}

	public static void checkReleasePermission(AbstractProject job) {
		job.checkPermission(RELEASE_PERMISSION);
	}
    
	@Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        
        @Override
        public String getDisplayName() {
            return Messages.ReleaseWrapper_ConfigureReleaseBuild();
        }
        
        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            ReleaseWrapper instance = new ReleaseWrapper();
            instance.releaseVersionTemplate = formData.getString("releaseVersionTemplate");
            instance.doNotKeepLog = formData.getBoolean("doNotKeepLog");
            instance.overrideBuildParameters = formData.getBoolean("overrideBuildParameters");
            instance.parameterDefinitions = Descriptor.newInstancesFromHeteroList(req, formData, "parameters", ParameterDefinition.all());
            instance.preBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "preBuildSteps", getSteps());
            instance.preMatrixBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "preMatrixBuildSteps", getSteps());
            instance.postBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "postBuildSteps", getSteps());
            instance.postSuccessfulBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "postSuccessfulBuildSteps", getSteps());
            instance.postFailedBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "postFailedBuildSteps", getSteps());
            instance.postMatrixBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "postMatrixBuildSteps", getSteps());
            instance.postSuccessfulMatrixBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "postSuccessfulMatrixBuildSteps", getSteps());
            instance.postFailedMatrixBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "postFailedMatrixBuildSteps", getSteps());
            return instance;
        }
        
        private ExtensionList getSteps()
        {
            DescriptorExtensionList<Publisher, Descriptor<Publisher>> publishers = DescriptorExtensionList.createDescriptorList(Jenkins.getInstance(), Publisher.class);
            DescriptorExtensionList<Builder, Descriptor<Builder>> builders = DescriptorExtensionList.createDescriptorList(Jenkins.getInstance(), Builder.class);

            ExtensionListImpl impl = new ExtensionListImpl();
            impl.addAll(publishers);
            impl.addAll(builders);
            return impl;
        }
        
        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return FreeStyleProject.class.isInstance(item) || MavenModuleSet.class.isInstance(item) || MatrixProject.class.isInstance(item) || isApplicableFor3rdParty(item);
        }
        
        private boolean isApplicableFor3rdParty(AbstractProject<?, ?> item) {
        	boolean isApplicable = false;
            Jenkins jenkinsInstance = Jenkins.getInstance();
            if (jenkinsInstance != null){
                if (jenkinsInstance.getPlugin("ivy") != null) {
                    isApplicable = IvyModuleSet.class.isInstance(item);
                }
                if(jenkinsInstance.getPlugin("jobgenerator") != null){
                    isApplicable |= JobGenerator.class.isInstance(item);
                }
            }
            return isApplicable;
        } 

        public boolean isMatrixProject(AbstractProject<?, ?> item) {
            return MatrixProject.class.isInstance(item);
        }

        private static class ExtensionListImpl extends ExtensionList<Descriptor> {

            public ExtensionListImpl() {
                super(Jenkins.getInstance(), Descriptor.class);
            }
        }
    }

    public class ReleaseAction implements Action, PermalinkProjectAction {
        private AbstractProject project;
        private String releaseVersion;
        private String developmentVersion;    
        
        public ReleaseAction(AbstractProject project) {
            this.project = project;
        }
        
        public List<ParameterDefinition> getParameterDefinitions() {
        	return parameterDefinitions;
        }

        public List<ParameterDefinition> getBuildParameterDefinitions() {
            ParametersDefinitionProperty paramsDefProp = (ParametersDefinitionProperty) project.getProperty(ParametersDefinitionProperty.class);
            if (paramsDefProp != null) {
                return paramsDefProp.getParameterDefinitions();
            }
            return null;
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
        	return this.project.isBuildable() && ReleaseWrapper.hasReleasePermission(project) ? "package.png" : null;
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
         * Gets the {@link ParameterDefinition} of the given name, including
         * the ones from the build parameters, if any.
         *
         * @param name The parameter name
         * @return the parameter definition with the given name
         */
        public ParameterDefinition getParameterDefinition(String name) {
            ParametersDefinitionProperty buildParamsDefProp = (ParametersDefinitionProperty) project.getProperty(ParametersDefinitionProperty.class);
            List<ParameterDefinition> buildParameterDefinitions = null;
            if (buildParamsDefProp != null) {
                buildParameterDefinitions = buildParamsDefProp.getParameterDefinitions();
            }

            if (!overrideBuildParameters && parameterDefinitions == null
                    || overrideBuildParameters && parameterDefinitions == null && buildParameterDefinitions == null) {
        		return null;
        	}

            for (ParameterDefinition pd : parameterDefinitions) {
                if (pd.getName().equals(name)) {
                    return pd;
                }
            }

            if (overrideBuildParameters) {
                for (ParameterDefinition pd : buildParameterDefinitions) {
                    if (pd.getName().equals(name)) {
                        return pd;
                    }
                }
            }

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
            for(ParameterDefinition paramDefinition : paramDefProp.getParameterDefinitions()) {
               ParameterValue defaultValue = paramDefinition.getDefaultParameterValue();
                
                if(defaultValue != null)
                    defValues.add(defaultValue);           
            }
            
            return defValues;
        }
        
        public boolean isOverrideBuildParameters() {
            return overrideBuildParameters;
        }

        @RequirePOST
        public void doSubmit(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        	// verify permission
        	ReleaseWrapper.checkReleasePermission(project);
        	
            // bind development / release version
            req.bindParameters(this);

            // create parameter list
            List<ParameterValue> paramValues;
            if (isOverrideBuildParameters()) {
                // if overrideBuildParameters is set, then build params are submitted
                // within the list of release params -- no need to gather default values
                paramValues = new ArrayList<ParameterValue>();
            }
            else {
                paramValues = getDefaultParametersValues();
            }
            
            if (getParameterDefinitions() != null && !getParameterDefinitions().isEmpty()
                    || overrideBuildParameters &&  getBuildParameterDefinitions() != null && !getBuildParameterDefinitions().isEmpty()) {
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
            if (!project.scheduleBuild(0, new Cause.UserIdCause(), 
            		new ReleaseBuildBadgeAction(), 
            		new SafeParametersAction(paramValues))) {
            	// TODO redirect to error page?
            }
            
            // redirect to status page
            resp.sendRedirect(project.getAbsoluteUrl());
        }

		public List<Permalink> getPermalinks() {
			return RELEASE;
		}

    }
    
    public static class ReleaseBuildBadgeAction implements BuildBadgeAction, MatrixChildAction {
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

    // Ideally needs rework to a REAL extension point implementation
    /**
     * Aggregates releases in matrix projects.
     * @since 2.5 - First implementation
     * @since 2.6 - Static class
     */
    public static class ReleaseAggregator extends MatrixAggregator {
        private List<BuildStep> preMatrixBuildSteps = new ArrayList<BuildStep>();
        private List<BuildStep> postSuccessfulMatrixBuildSteps = new ArrayList<BuildStep>();
        private List<BuildStep> postFailedMatrixBuildSteps = new ArrayList<BuildStep>();
        private List<BuildStep> postMatrixBuildSteps = new ArrayList<BuildStep>();
        private boolean isNotRelease = true;

        public ReleaseAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
            super(build, launcher, listener);

            this.isNotRelease = build.getAction(ReleaseBuildBadgeAction.class) == null;
        }

        public void setPreMatrixBuildSteps(List<BuildStep> preMatrixBuildSteps) {
            this.preMatrixBuildSteps = preMatrixBuildSteps;
        }

        /**
         * @param postMatrixBuildSteps The postMatrixBuildSteps to set.
         */
        public void setPostMatrixBuildSteps(List<BuildStep> postMatrixBuildSteps) {
            this.postMatrixBuildSteps = postMatrixBuildSteps;
        }

        public void setPostSuccessfulMatrixBuildSteps(List<BuildStep> postSuccessfulMatrixBuildSteps) {
            this.postSuccessfulMatrixBuildSteps = postSuccessfulMatrixBuildSteps;
        }

        public void setPostFailedMatrixBuildSteps(List<BuildStep> postFailedMatrixBuildSteps) {
            this.postFailedMatrixBuildSteps = postFailedMatrixBuildSteps;
        }

        @Override
        public boolean startBuild() throws InterruptedException, IOException {
            if (isNotRelease) {
                return true;
            }

            return executeBuildSteps(preMatrixBuildSteps, build, launcher, listener);
        }

        @Override
        public boolean endBuild() throws InterruptedException, IOException {
            if (isNotRelease) {
                return true;
            }

            boolean shouldContinue = true;

            try {
                Result result = build.getResult();

                if (result == null || result.isBetterOrEqualTo(Result.UNSTABLE)) {
                    shouldContinue = executeBuildSteps(postSuccessfulMatrixBuildSteps, build, launcher, listener);
                } else {
                    shouldContinue = executeBuildSteps(postFailedMatrixBuildSteps, build, launcher, listener);
                }
            } finally {
                if (shouldContinue) {
                    shouldContinue = executeBuildSteps(postMatrixBuildSteps, build, launcher, listener);
                }
            }
            return shouldContinue;
        }
    
        private boolean executeBuildSteps(List<BuildStep> buildSteps, AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
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
    }
}
