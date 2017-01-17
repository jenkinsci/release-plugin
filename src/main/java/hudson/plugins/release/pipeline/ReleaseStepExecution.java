package hudson.plugins.release.pipeline;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

import com.google.inject.Inject;

import hudson.AbortException;
import hudson.console.ModelHyperlinkNote;
import hudson.model.Action;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Item;
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

/**
 * @author Alexey Merezhin
 * @since 2.7
 */
public class ReleaseStepExecution extends StepExecution {
    private static final Logger LOGGER = Logger.getLogger(ReleaseStepExecution.class.getName());

    transient ReleaseStep step;

    public ReleaseStepExecution(@Nonnull StepContext context, @Nonnull ReleaseStep step) {
        super(context);
        this.step = step;
    }

    private List<ParameterValue> updateParametersWithDefaults(ParameterizedJobMixIn.ParameterizedJob project,
            List<ParameterValue> parameters) throws AbortException {

        if (project instanceof BuildableItemWithBuildWrappers) {
            ReleaseWrapper wrapper = ((BuildableItemWithBuildWrappers) project).getBuildWrappersList()
                                                                               .get(ReleaseWrapper.class);
            if (wrapper != null) {
                for (ParameterDefinition pd : wrapper.getParameterDefinitions()) {
                    boolean parameterExists = false;
                    for (ParameterValue pv : parameters) {
                        if (pv.getName().equals(pd.getName())) {
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

        final ParameterizedJobMixIn.ParameterizedJob project = Jenkins.getActiveInstance().
                getItemByFullName(step.getJob(), ParameterizedJobMixIn.ParameterizedJob.class);;
        if (project == null) {
            throw new AbortException("No parametrized job named " + step.getJob() + " found");
        }
        getContext().get(TaskListener.class).getLogger()
                .println("Releasing project: " + ModelHyperlinkNote.encodeTo(project));

        LOGGER.log(Level.FINER, "scheduling a release of {0} from {1}", new Object[] { project, getContext() });
        Action[] actions = new Action[] { new ReleaseTriggerAction(getContext()),
                new ReleaseWrapper.ReleaseBuildBadgeAction(),
                new SafeParametersAction(updateParametersWithDefaults(project, step.getParameters())),
                new CauseAction(new Cause.UpstreamCause(getContext().get(Run.class))) };

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
