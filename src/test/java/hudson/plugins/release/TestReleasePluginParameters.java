package hudson.plugins.release;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;

import com.gargoylesoftware.htmlunit.html.HtmlForm;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.tasks.BuildStep;

public class TestReleasePluginParameters {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @Issue("JENKINS-34996")
    public void testReleaseParameters() throws Exception {
        FreeStyleProject prj = j.createProject(FreeStyleProject.class, "foo");
        ReleaseWrapper releaseWrapper = new ReleaseWrapper();
        // Parameter in release specific builder
        releaseWrapper.setParameterDefinitions(Arrays.asList(new ParameterDefinition [] {
                new StringParameterDefinition("TEST", "test value") }));

        CheckerBuildStep checkerBuildStep = new CheckerBuildStep();
        releaseWrapper.setPostSuccessfulBuildSteps(Arrays.asList(new BuildStep [] { checkerBuildStep }));
        prj.getBuildWrappersList().add(releaseWrapper);

        scheduleReleaseBuild(prj);

        j.waitUntilNoActivity();
        assertTrue("Build finishes", prj.getLastBuild() != null);

        assertTrue("The parameter must be visible as environment variable", checkerBuildStep.value != null);
        assertTrue("The parameter value must match", checkerBuildStep.value.equals("test value"));
        j.assertBuildStatus(Result.SUCCESS, prj.getLastBuild());
    }

    @Test
    @Issue("JENKINS-34996") // This test worked before the fix, added just as a verification
    public void testJobParameters() throws Exception {
        FreeStyleProject prj = j.createProject(FreeStyleProject.class, "foo");
        // Parameter at top level (job)
        prj.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("TEST","test value")));

        ReleaseWrapper releaseWrapper = new ReleaseWrapper();
        CheckerBuildStep checkerBuildStep = new CheckerBuildStep();
        releaseWrapper.setPostSuccessfulBuildSteps(Arrays.asList(new BuildStep [] { checkerBuildStep }));
        prj.getBuildWrappersList().add(releaseWrapper);

        scheduleReleaseBuild(prj);

        j.waitUntilNoActivity();

        assertTrue("The parameter must be visible as environment variable", checkerBuildStep.value != null);
        assertTrue("The parameter value must match", checkerBuildStep.value.equals("test value"));
        j.assertBuildStatus(Result.SUCCESS, prj.getLastBuild());
    }

    public static class CheckerBuildStep extends MockBuilder {

        public String value;

        public CheckerBuildStep() {
            super(Result.SUCCESS);
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            value = build.getEnvironment(listener).get("TEST");
            return true;
        }
    }

    /**
     * The release build must be scheduled from UI as all the parameters logic is tied
     * to {@link ReleaseWrapper.ReleaseAction#doSubmit(org.kohsuke.stapler.StaplerRequest, org.kohsuke.stapler.StaplerResponse)}.
     */
    private void scheduleReleaseBuild(Job<?, ?> job) throws Exception {
        HtmlForm releaseForm = j.createWebClient().getPage(job, "release").getFormByName("release-action-form");
        j.submit(releaseForm);
    }

}
