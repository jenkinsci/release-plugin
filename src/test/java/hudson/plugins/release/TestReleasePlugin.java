package hudson.plugins.release;

import hudson.PluginWrapper;
import hudson.model.Descriptor;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the general integration of the plugin with Jenkins/Hudson and ensure the plugin is registered properly and is active
 */
public class TestReleasePlugin
{
	@Rule
	public JenkinsRule j = new JenkinsRule();

	@Test
	public void testPluginActive() throws Exception
	{
		// Check if the plugin is loaded and active
		PluginWrapper plugin = j.getPluginManager().getPlugin("release");
		assertTrue("Plugin not active", plugin.isActive());
	}

	@Test
	public void testDescriptorRegistered() throws Exception
	{
		Descriptor<?> descriptor = Descriptor.find(ReleaseWrapper.DescriptorImpl.class.getName());
		assertNotNull("Must have ReleaseWrapper descriptor", descriptor);
	}

}
