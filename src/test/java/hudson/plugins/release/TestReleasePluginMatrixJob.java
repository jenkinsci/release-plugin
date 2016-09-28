package hudson.plugins.release;

import hudson.matrix.MatrixProject;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildStep;
import hudson.tasks.Fingerprinter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Test integration with MatrixProject
 */
public class TestReleasePluginMatrixJob {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    protected MatrixProject job;

    @Before
    public void createJob() throws Exception {
        job = j.createProject(MatrixProject.class, "foo");
    }

    @After
    public void deleteJob() throws Exception {
        job.delete();
    }

    /**
     * Make sure postBuildSteps and postMatrixBuildSteps behave as expected on save
     *
     * @throws Exception
     */
    @Test
    @Bug(31159)
    public void testMatrixStepsSubmission() throws Exception {
        // prepare wrapper with some randomly chosen build steps
        final ReleaseWrapper originalWrapper = new ReleaseWrapper();
        final BuildStep postBuilder = new ArtifactArchiver("foo/*", "", true);
        final BuildStep postMatrixBuilder = new Fingerprinter("bar/*", true);
        originalWrapper.getPostBuildSteps().add(postBuilder);
        originalWrapper.getPostMatrixBuildSteps().add(postMatrixBuilder);

        job.getBuildWrappersList().add(originalWrapper);

        // submit unmodified form
        j.submit(j.createWebClient().getPage(job, "configure").getFormByName("config"));

        // retrieve resulting wrapper
        final ReleaseWrapper resultingWrapper = job.getBuildWrappersList().get(ReleaseWrapper.class);

        // assert
        Assert.assertTrue("postBuildSteps size is not 1", resultingWrapper.getPostBuildSteps().size() == 1);
        Assert.assertTrue("Expected ArtifactArchiver not found in postBuildSteps", resultingWrapper.getPostBuildSteps().iterator().next() instanceof ArtifactArchiver);
        Assert.assertTrue("postMatrixBuildSteps size is not 1", resultingWrapper.getPostMatrixBuildSteps().size() == 1);
        Assert.assertTrue("Expected Fingerprinter not found in postMatrixBuildSteps", resultingWrapper.getPostMatrixBuildSteps().iterator().next() instanceof Fingerprinter);
    }
}
