package hudson.plugins.release.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.util.StaplerReferer;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.google.common.collect.ImmutableSet;

import hudson.Extension;
import hudson.model.AutoCompletionCandidates;
import hudson.model.BuildableItem;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.release.ReleaseWrapper;
import javax.annotation.CheckForNull;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Pipeline step implementation for the Release plugin.
 *
 * @author Alexey Merezhin
 * @since 2.7
 */
@Restricted(NoExternalUse.class)
public class ReleaseStep extends Step {
    private static final Logger LOGGER = Logger.getLogger(ReleaseStep.class.getName());

    /**
     * Full name of the job.
     * If {@code null}, the release step will fail
     */
    @CheckForNull
    private String job;
    @Nonnull
    private List<ParameterValue> parameters;

    @DataBoundConstructor
    public ReleaseStep(String job) {
        this.job = StringUtils.trimToNull(job);
        parameters = new ArrayList<>();
    }

    /**
     * Gets full name of the job to be released
     * @return Full name of the job.
     *         {@code null} if it is not specified in the Pipeline definition.
     *         {@code null} will cause the build failure.
     */
    @CheckForNull
    public String getJob() {
        return job;
    }

    @Restricted(NoExternalUse.class)
    public void setJob(String job) {
        this.job = StringUtils.trimToNull(job);
    }

    public List<ParameterValue> getParameters() {
        return parameters;
    }

    @DataBoundSetter
    public void setParameters(List<ParameterValue> parameters) {
        if (parameters == null) {
            this.parameters = new ArrayList<>();
        } else {
            this.parameters = parameters;
        }
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new ReleaseStepExecution(stepContext, this);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "release";
        }

        @Override
        public String getDisplayName() {
            return Messages.ReleaseStep_DisplayName();
        }

        @Override
        public Set<Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);
        }

        @Override
        public Step newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            ReleaseStep step = (ReleaseStep) super.newInstance(req, formData);
            
            final String jobFullName = step.getJob();
            if (jobFullName == null) {
                throw new FormException("Job name is not specified or blank", "job");
            }

            // Cf. ParametersDefinitionProperty._doBuild:
            Object parameter = formData.get("parameter");
            JSONArray params = parameter != null ? JSONArray.fromObject(parameter) : null;
            if (params != null) {
                // TODO: Add support of relative paths like in the Promoted Builds plugin
                BuildableItem project = Jenkins.getActiveInstance()
                                               .getItemByFullName(jobFullName, BuildableItem.class);

                if (project == null) {
                    throw new FormException("Can't find buildable item " + jobFullName, "job");
                } else if (project instanceof BuildableItemWithBuildWrappers) {
                    ReleaseWrapper wrapper = ((BuildableItemWithBuildWrappers) project)
                            .getBuildWrappersList().get(ReleaseWrapper.class);
                    if (wrapper == null) {
                        throw new FormException("Job doesn't have the Release plugin configuration", "job");
                    }
                    List<ParameterDefinition> parameterDefinitions = wrapper.getParameterDefinitions();
                    if (parameterDefinitions != null) {
                        List<ParameterValue> values = new ArrayList<>();
                        for (Object o : params) {
                            if (o instanceof JSONObject) {
                                JSONObject jo = (JSONObject) o;
                                String name = jo.getString("name");
                                for (ParameterDefinition pd : parameterDefinitions) {
                                    if (name.equals(pd.getName())) {
                                        ParameterValue parameterValue = pd.createValue(req, jo);
                                        values.add(parameterValue);
                                    }
                                }
                            }
                        }
                        step.setParameters(values);
                    }
                } else {
                    throw new FormException("The job is not an instance of the BuildableItemWithBuildWrappers class: " + project.getClass().getName(), "job");
                }
            }
            return step;
        }

        public AutoCompletionCandidates doAutoCompleteJob(@AncestorInPath ItemGroup<?> context, @QueryParameter String value) {
            return AutoCompletionCandidates.ofJobNames(ParameterizedJobMixIn.ParameterizedJob.class, value, context);
        }

        @Restricted(DoNotUse.class) // for use from config.jelly / parameters.groovy
        public String getContext() {
            Job<?,?> job = StaplerReferer.findItemFromRequest(Job.class);
            return job != null ? job.getFullName() : null;
        }
    }
}
