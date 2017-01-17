package hudson.plugins.release.pipeline;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;

/**
 * copied from org.jenkinsci.plugins.workflow.support.steps.build.BuildQueueListener
 * @since 2.7
 */
@Extension
@Restricted(NoExternalUse.class)
public class ReleaseQueueListener extends QueueListener {
    @Override
    public void onLeft(Queue.LeftItem li) {
        if(li.isCancelled()){
            for (ReleaseTriggerAction.Trigger trigger : ReleaseTriggerAction.triggersFor(li)) {
                trigger.context.onFailure(new AbortException("Pipeline Release plugin: build of " + li.task.getFullDisplayName() + " was cancelled"));
            }
        }
    }


}
