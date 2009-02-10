package hudson.plugins.release;

import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildBadgeAction;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Wraps a build with pre and post build steps.  These steps can take
 * any action as part of the special release build.
 *
 * @author Peter Hayes
 * @since 0.1
 */
public class ReleaseWrapper extends BuildWrapper {
    private List<Builder> preBuildSteps = new ArrayList<Builder>();
    private List<Builder> postBuildSteps = new ArrayList<Builder>();
    
    private transient boolean releaseBuild = false;
    private transient String releaseVersion;
    private transient String developmentVersion;
    
    /**
     * @stapler-constructor
     */
    public ReleaseWrapper() {
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
    
    private void enableReleaseBuild() {
        releaseBuild = true;
    }
    
    @Override
    public Environment setUp(AbstractBuild build, final Launcher launcher, BuildListener listener) throws IOException,
            InterruptedException {
        synchronized (this) {
            if (releaseBuild) {
                releaseBuild = false;
            } else {
                return new Environment() { };
            }
        }
                
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("RELEASE_VERSION", releaseVersion);
        properties.put("DEVELOPMENT_VERSION", developmentVersion);
        
        // add ReleaseBuildBadge to this build
        build.addAction(new ReleaseBuildBadgeAction(releaseVersion));
        
        final WrappedBuild wrappedBuild = new WrappedBuild(build, properties);
        
        if (!executeBuildSteps(preBuildSteps, wrappedBuild, launcher, listener)) {
            throw new IOException("Could not execute pre-build steps");
        }
        
        // return environment
        return new Environment() {
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException,
                    InterruptedException {
                
                // save build
                build.keepLog();
                
                // set build description to indicate release
                build.setDescription("Release # " + releaseVersion);
                
                return executeBuildSteps(postBuildSteps, wrappedBuild, launcher, listener);
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

    public Descriptor<BuildWrapper> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    
    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        
        /**
         * 
         */
        public DescriptorImpl() {
            super(ReleaseWrapper.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return "Configure release build";
        }
        
        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            ReleaseWrapper instance = new ReleaseWrapper();
            instance.preBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "preBuildSteps", BuildStep.BUILDERS);
            instance.postBuildSteps = Descriptor.newInstancesFromHeteroList(req, formData, "postBuildSteps", BuildStep.BUILDERS);
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
            return "package.gif";
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
                
                if (badge != null) {
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
        
        public void doSubmit(StaplerRequest req, StaplerResponse resp) throws IOException {
            // bind development / release version
            req.bindParameters(this);
            
            // schedule release build
            synchronized (ReleaseWrapper.this) {
                if (Hudson.getInstance().getQueue().add(project, 0)) {
                    enableReleaseBuild();
                    ReleaseWrapper.this.releaseVersion = releaseVersion;
                    ReleaseWrapper.this.developmentVersion = developmentVersion;
                    
                    releaseVersion = null;
                    developmentVersion = null;
                }
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
