package hudson.plugins.release.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import org.jenkinsci.plugins.workflow.steps.StepContext;

import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.InvisibleAction;
import hudson.model.Queue;
import hudson.model.queue.FoldableAction;

/**
 * copied from org.jenkinsci.plugins.workflow.support.steps.build.BuildTriggerAction
 * @since 2.7
 */
@SuppressWarnings("SynchronizeOnNonFinalField")
class ReleaseTriggerAction extends InvisibleAction implements FoldableAction {
    private static final Logger LOGGER = Logger.getLogger(ReleaseTriggerAction.class.getName());

    /** Record of one upstream build step. */
    static class Trigger {

        final StepContext context;

        /** Record of cancellation cause passed to {@link ReleaseStepExecution#stop}, if any. */
        @CheckForNull Throwable interruption;

        Trigger(StepContext context) {
            this.context = context;
        }

    }

    private /* final */ List<Trigger> triggers;

    ReleaseTriggerAction(StepContext context) {
        triggers = new ArrayList<>();
        triggers.add(new Trigger(context));
    }

    static Iterable<Trigger> triggersFor(Actionable actionable) {
        List<Trigger> triggers = new ArrayList<>();
        for (ReleaseTriggerAction action : actionable.getActions(ReleaseTriggerAction.class)) {
            synchronized (action.triggers) {
                triggers.addAll(action.triggers);
            }
        }
        return triggers;
    }

    @Override public void foldIntoExisting(Queue.Item item, Queue.Task owner, List<Action> otherActions) {
        // there may be >1 upstream builds (or other unrelated causes) for a single downstream build
        ReleaseTriggerAction existing = item.getAction(ReleaseTriggerAction.class);
        if (existing == null) {
            item.addAction(this);
        } else {
            if (!triggers.isEmpty()) {
                synchronized (existing.triggers) {
                    existing.triggers.addAll(triggers);
                }
            }
        }
        LOGGER.log(Level.FINE, "coalescing actions for {0}", item);
    }

}
