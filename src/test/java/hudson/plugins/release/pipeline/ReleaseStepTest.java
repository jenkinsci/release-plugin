package hudson.plugins.release.pipeline;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;

import hudson.model.Action;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.release.ReleaseWrapper;
import jenkins.model.ParameterizedJobMixIn;

/**
 * Created by e3cmea on 1/4/17.
 *
 * @author Alexey Merezhin
 */
public class ReleaseStepTest {
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule
    public JenkinsRule j = new JenkinsRule();
    @Rule public LoggerRule logging = new LoggerRule();

    @Test
    public void releaseFreeStyleProject() throws Exception {
        FreeStyleProject ds = j.createFreeStyleProject("ds");
        ds.getBuildWrappersList().add(new ReleaseWrapper());
        WorkflowJob us = j.jenkins.createProject(WorkflowJob.class, "us");
        us.setDefinition(new CpsFlowDefinition("release 'ds'\n echo \"release's done\"", true));
        j.assertLogContains("release's done", j.buildAndAssertSuccess(us));
        ds.getBuildByNumber(1).delete();
    }

    @Test
    public void failForJobWithoutRelease() throws Exception {
        FreeStyleProject ds = j.createFreeStyleProject("ds");
        final WorkflowJob us = j.jenkins.createProject(WorkflowJob.class, "us");
        us.setDefinition(new CpsFlowDefinition("release 'ds'\n echo \"release's done\"", true));
        QueueTaskFuture f = (new ParameterizedJobMixIn() {
            protected Job asJob() {
                return us;
            }
        }).scheduleBuild2(0, new Action[0]);
        j.assertBuildStatus(Result.FAILURE, f);
    }

}