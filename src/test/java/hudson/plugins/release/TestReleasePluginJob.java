package hudson.plugins.release;

import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import org.junit.Ignore;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.*;
import hudson.plugins.release.ReleaseWrapper;
import hudson.plugins.release.ReleaseWrapper.ReleaseAction;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;

/**
 * Test the general integration of the plugin with Jenkins/Hudson and ensure it gets added properly and correctly add it's links/buttons etc to the
 * required pages.
 */
public class TestReleasePluginJob
{
	@Rule
	public JenkinsRule j = new JenkinsRule();

	FreeStyleProject job;

	@Before
	public void createJob() throws Exception
	{
		job = j.createFreeStyleProject("test");
	}

	@After
	public void deleteJob() throws Exception
	{
		job.delete();
	}

	@Test
	public void testPermission() throws Exception
	{
		// Check if the user has permission to release
		assertTrue("No permission", ReleaseWrapper.hasReleasePermission(job));
	}

	@Test
	@Ignore("Html unit is not adding in release action on submit, configuration through UI works")
	public void testInBuilders() throws Exception
	{
		// Check the job has the ReleaseWrapper listed in it's builders, AND that it it listed in the project actions
		enableReleaseBuilder();
		DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrappersList = job.getBuildWrappersList();
		Descriptor<BuildWrapper> descriptor = Descriptor.find(ReleaseWrapper.DescriptorImpl.class.getName());
		BuildWrapper buildWrapper = buildWrappersList.get(descriptor);
		assertNotNull(buildWrapper);
		Collection<? extends Action> actions = buildWrapper.getProjectActions(job);
		assertNotNull("Actions can't be null", actions);
		assertThat("Must have actions", actions.size(), not(0));
	}

	@Test
	@Ignore("Html unit is not adding in release action on submit, configuration through UI works")
	public void testInActions() throws Exception
	{
		// Check that the job has an action listed when release is enabled
		enableReleaseBuilder();
		assertNotNull("Must have action", job.getAction(ReleaseAction.class));
	}

	@Test
	@Ignore("Html unit is not adding in release action on submit, configuration through UI works")
	public void testEnableReleaseUsingWebPage() throws Exception
	{
		enableReleaseBuilder();

		/** Check if the Release links exist */
		HtmlPage page = j.createWebClient().goTo("job/test/");
		// Internationalisation not needed because the URL doesn't change between languages
		// see getUrlName() in ReleaseAction
		List<?> nodes = page.getByXPath("//a[contains(@href, '/test/release')]");
		assertThat("Require 2 href links", nodes.size(), is(2));
	}

	/** Enable the release builder by adding it to the job */
	private void enableReleaseBuilder() throws Exception
	{
		/** Enable the release plugin for the job */
		HtmlForm configForm = j.createWebClient().getPage(job, "configure").getFormByName("config");
		assertThat(configForm, notNullValue());

		// not sure if internationalisation required for this?
		HtmlCheckBoxInput releaseCheckButton = configForm.getInputByName("hudson-plugins-release-ReleaseWrapper");
		assertThat("No Release Check Button found", releaseCheckButton, notNullValue());
		releaseCheckButton.setChecked(true);
		j.submit(configForm);
	}

}
