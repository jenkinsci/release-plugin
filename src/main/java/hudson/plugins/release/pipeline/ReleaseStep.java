package hudson.plugins.release.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.workflow.util.StaplerReferer;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.google.inject.Inject;

import hudson.AbortException;
import hudson.Extension;
import hudson.console.ModelHyperlinkNote;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.BuildableItem;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.release.ReleaseWrapper;
import hudson.plugins.release.SafeParametersAction;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Pipeline step implementation for Release plugin.
 *
 * @author Alexey Merezhin
 * @since 2.7
 */
public class ReleaseStep extends AbstractStepImpl {
    private static final Logger LOGGER = Logger.getLogger(ReleaseStep.class.getName());

    private String job;
    @Nonnull
    private List<ParameterValue> parameters;

    @DataBoundConstructor
    public ReleaseStep(String job) {
        this.job = job;
        parameters = new ArrayList<>();
    }

    private Object readResolve() {
        if (parameters == null) {
            parameters = new ArrayList<>();
        }
        return this;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
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

    public static class Execution extends AbstractStepExecutionImpl {
        @StepContextParameter private transient Run<?,?> invokingRun;
        @StepContextParameter private transient TaskListener listener;

        @Inject(optional=true) transient ReleaseStep step;

        private List<ParameterValue> updateParametersWithDefaults(ParameterizedJobMixIn.ParameterizedJob project,
                List<ParameterValue> parameters) throws AbortException {

            if (project instanceof BuildableItemWithBuildWrappers) {
                ReleaseWrapper wrapper = ((BuildableItemWithBuildWrappers) project).getBuildWrappersList()
                                                                                   .get(ReleaseWrapper.class);
                if (wrapper != null) {
                    for (ParameterDefinition pd : wrapper.getParameterDefinitions()) {
                        boolean parameterExists = false;
                        for (ParameterValue pv : parameters) {
                            if (pv.getName()
                                  .equals(pd.getName())) {
                                parameterExists = true;
                                break;
                            }
                        }
                        if (!parameterExists) {
                            parameters.add(pd.getDefaultParameterValue());
                        }
                    }
                } else {
                    throw new AbortException("Job doesn't have release plugin configuration");
                }
            }
            return parameters;
        }

        @Override
        public boolean start() throws Exception {
            if (step.getJob() == null) {
                throw new AbortException("Job name is not defined.");
            }

            final ParameterizedJobMixIn.ParameterizedJob project =
                    Jenkins.getActiveInstance().
                            getItem(step.getJob(), invokingRun.getParent(),
                                    ParameterizedJobMixIn.ParameterizedJob.class);
            if (project == null) {
                throw new AbortException("No parametrized job named " + step.getJob() + " found");
            }
            listener.getLogger().println("Releasing project: " + ModelHyperlinkNote.encodeTo(project));

            StepContext context = getContext();
            LOGGER.log(Level.FINER, "scheduling a release of {0} from {1}", new Object[] { project, context });
            Action[] actions = new Action[]{
                    new ReleaseTriggerAction(context),
                    new SafeParametersAction(updateParametersWithDefaults(project, step.getParameters())),
                    new ReleaseWrapper.ReleaseBuildBadgeAction(),
                    new CauseAction(new Cause.UpstreamCause(invokingRun))
            };

            Queue.Item item = ParameterizedJobMixIn.scheduleBuild2((Job<?, ?>) project, 0, actions);

            if (item == null || item.getFuture() == null) {
                throw new AbortException("Failed to trigger build of " + project.getFullName());
            }

            return false;
        }

        @Override
        public void stop(@Nonnull Throwable cause) throws Exception {
            getContext().onFailure(cause);
        }

        private static final long serialVersionUID = 1L;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "release";
        }

        @Override
        public String getDisplayName() {
            return "Trigger release for the job";
        }

        @Override public Step newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            ReleaseStep step = (ReleaseStep) super.newInstance(req, formData);
            // Cf. ParametersDefinitionProperty._doBuild:
            Object parameter = formData.get("parameter");
            JSONArray params = parameter != null ? JSONArray.fromObject(parameter) : null;
            if (params != null) {
                Jenkins jenkins = Jenkins.getInstance();
                Job<?,?> context = StaplerReferer.findItemFromRequest(Job.class);
                Job<?,?> job = jenkins != null ? jenkins.getItem(step.getJob(), context, Job.class) : null;

                BuildableItem project = Jenkins.getActiveInstance()
                                               .getItem(step.getJob(), context, BuildableItem.class);

                if (project instanceof BuildableItemWithBuildWrappers) {
                    ReleaseWrapper wrapper = ((BuildableItemWithBuildWrappers) project).getBuildWrappersList()
                                                                       .get(ReleaseWrapper.class);
                    if (wrapper == null) {
                        throw new IllegalArgumentException("Job doesn't have release plugin configuration");
                    }
                    List<ParameterDefinition> parameterDefinitions = wrapper.getParameterDefinitions();
                    if (parameterDefinitions != null) {
                        List<ParameterValue> values = new ArrayList<>();
                        for (Object o : params) {
                            JSONObject jo = (JSONObject) o;
                            String name = jo.getString("name");
                            for (ParameterDefinition pd : parameterDefinitions) {
                                if (name.equals(pd.getName())) {
                                    ParameterValue parameterValue = pd.createValue(req, jo);
                                    values.add(parameterValue);
                                }
                            }
                        }
                        step.setParameters(values);
                    }
                } else {
                    String message = project == null ?
                            "Can't find project " + step.getJob() : "Wrong job type: " + project.getClass().getName();
                    throw new IllegalArgumentException(message);
                }
            }
            return step;
        }

        public AutoCompletionCandidates doAutoCompleteJob(@AncestorInPath ItemGroup<?> context, @QueryParameter String value) {
            return AutoCompletionCandidates.ofJobNames(ParameterizedJobMixIn.ParameterizedJob.class, value, context);
        }

        @Restricted(DoNotUse.class) // for use from config.jelly
        public String getContext() {
            Job<?,?> job = StaplerReferer.findItemFromRequest(Job.class);
            return job != null ? job.getFullName() : null;
        }
    }
}
