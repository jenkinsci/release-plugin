package hudson.plugins.release;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.jvnet.hudson.test.JenkinsRule;
import hudson.model.*;
import java.util.List;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.Rule;

public class TestJobCreation
{
    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    @Test 
    public void createJobAndCheckButton() throws Exception 
    {
	FreeStyleProject p = j.createFreeStyleProject("test");
	
	/** Enable the release plugin for the job */
	HtmlForm configForm = j.createWebClient().getPage(p, "configure").getFormByName("config");
	assertThat(configForm, notNullValue());
	HtmlCheckBoxInput releaseCheckButton = configForm.getInputByName("hudson-plugins-release-ReleaseWrapper");
	assertThat(releaseCheckButton, notNullValue());
	releaseCheckButton.setChecked(true);
	j.submit(configForm);
	
	/** Check if the Release links exist */
	HtmlPage page = j.createWebClient().goTo("job/test/");
	List<Object> nodes = page.selectNodes("//a[contains(@href, '/test/release')]");
	assertThat(nodes.size(), is(2));

	/** Check the system log for errors
	HtmlPage logPage = j.createWebClient().goTo("log/all");
	String pageText = logPage.asText();
	PrintWriter fw = new PrintWriter("test1.log");
	fw.println(pageText);
	fw.close();
	assertFalse(pageText.contains("Exception"));
	*/
    }
   
}
