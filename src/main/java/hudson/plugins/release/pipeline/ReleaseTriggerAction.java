package hudson.plugins.release.pipeline;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.CheckForNull;

import org.jenkinsci.plugins.workflow.steps.StepContext;

import hudson.model.Actionable;
import hudson.model.InvisibleAction;

/**
 * copied from org.jenkinsci.plugins.workflow.support.steps.build.BuildTriggerAction
 * @since 2.7
 */
class ReleaseTriggerAction extends InvisibleAction {
    /** Record of one upstream build step. */
    static class Trigger {

        final StepContext context;

        /** Record of cancellation cause passed to {@link ReleaseStepExecution#stop}, if any. */
        @CheckForNull Throwable interruption;

        Trigger(StepContext context) {
            this.context = context;
        }

    }

    private final List<Trigger> triggers;

    ReleaseTriggerAction(StepContext context) {
        triggers = new ArrayList<>();
        triggers.add(new Trigger(context));
    }

    static Iterable<Trigger> triggersFor(Actionable actionable) {
        List<Trigger> triggers = new ArrayList<>();
        for (final ReleaseTriggerAction action : actionable.getActions(ReleaseTriggerAction.class)) {
            if (!action.triggers.isEmpty()) {
                synchronized (action.triggers) {
                    triggers.addAll(action.triggers);
                }
            }
        }
        return triggers;
    }

}
