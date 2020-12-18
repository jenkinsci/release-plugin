package hudson.plugins.release.pipeline;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

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
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * @author Alexey Merezhin
 * @since 2.7
 */
@Restricted(NoExternalUse.class)
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
                throw new AbortException("Job doesn't have the Release plugin configuration");
            }
        }
        return parameters;
    }

    @Override
    public boolean start() throws Exception {
        final String jobFullName = step.getJob();
        if (jobFullName == null) {
            throw new AbortException("Job name is not defined.");
        }

        final Item itemToRun = Jenkins.getActiveInstance().getItemByFullName(jobFullName);
        if (itemToRun == null) {
            throw new AbortException("No item found: " + jobFullName );
        } else if (!(itemToRun instanceof Job<?, ?> && itemToRun instanceof ParameterizedJobMixIn.ParameterizedJob)) {
            throw new AbortException("The specified item is not a parameterizable job: " + jobFullName );
        } 
        
        final ParameterizedJobMixIn.ParameterizedJob jobToRun = (ParameterizedJobMixIn.ParameterizedJob) itemToRun;
        println("Releasing job: " + ModelHyperlinkNote.encodeTo(jobToRun));

        LOGGER.log(Level.FINER, "Scheduling release of {0} from {1}", new Object[] { jobToRun, getContext() });
        Run run = getContext().get(Run.class);
        List<Action> actions = new ArrayList<>(3);
        actions.add(new ReleaseTriggerAction(getContext()));
        actions.add(new ReleaseWrapper.ReleaseBuildBadgeAction());
        actions.add(new SafeParametersAction(updateParametersWithDefaults(jobToRun, step.getParameters())));
        if (run != null) {
            actions.add(new CauseAction(new Cause.UpstreamCause(run)));
        }

        Queue.Item item = ParameterizedJobMixIn.scheduleBuild2((Job<?, ?>)jobToRun, 0, 
                actions.toArray(new Action[0]));

        if (item == null || item.getFuture() == null) {
            throw new AbortException("Failed to trigger build of " + jobToRun.getFullName());
        }

        return false;
    }

    private void println(String message) throws IOException, InterruptedException {
        TaskListener taskListener = getContext().get(TaskListener.class);
        if (taskListener == null) return;
        PrintStream taskLogger = taskListener.getLogger();
        taskLogger.println(message);
    }

    @Override
    public void stop(@Nonnull Throwable cause) throws Exception {
        getContext().onFailure(cause);
    }

    private static final long serialVersionUID = 1L;
}
