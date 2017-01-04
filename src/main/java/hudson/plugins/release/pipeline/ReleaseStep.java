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
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Cause;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.release.ReleaseWrapper;
import hudson.plugins.release.SafeParametersAction;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Created by e3cmea on 1/3/17.
 *
 * @author Alexey Merezhin
 */
public class ReleaseStep extends AbstractStepImpl {
    private static final Logger LOGGER = Logger.getLogger(ReleaseStep.class.getName());

    private String job;
    private List<ParameterValue> parameters;

    @DataBoundConstructor
    public ReleaseStep(String job) {
        this.job = job;
        parameters = new ArrayList<>();
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

    @DataBoundSetter public void setParameters(List<ParameterValue> parameters) {
        this.parameters = parameters;
    }

    public static class Execution extends AbstractStepExecutionImpl {
        @StepContextParameter private transient Run<?,?> invokingRun;
        @StepContextParameter private transient TaskListener listener;

        @Inject(optional=true) transient ReleaseStep step;

        private List<ParameterValue> updateParametersWithDefaults(AbstractProject project,
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

            final AbstractProject project = Jenkins.getActiveInstance()
                                                   .getItem(step.getJob(), invokingRun.getParent(), AbstractProject.class);
            if (project == null) {
                throw new AbortException("No parametrized job named " + step.getJob() + " found");
            }
            listener.getLogger().println("Releasing project: " + ModelHyperlinkNote.encodeTo(project));

            List<Action> actions = new ArrayList<>();
            StepContext context = getContext();
            actions.add(new ReleaseTriggerAction(context));
            LOGGER.log(Level.FINER, "scheduling a release of {0} from {1}", new Object[] { project, context });
            actions.add(new SafeParametersAction(updateParametersWithDefaults(project, step.getParameters())));
            actions.add(new ReleaseWrapper.ReleaseBuildBadgeAction());

            QueueTaskFuture<?> task = project.scheduleBuild2(0, new Cause.UpstreamCause(invokingRun), actions);
            if (task == null) {
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

                AbstractProject project = Jenkins.getActiveInstance()
                                                       .getItem(step.getJob(), context, AbstractProject.class);

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
                    throw new IllegalArgumentException("Wrong job type: " + project.getClass().getName());
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
