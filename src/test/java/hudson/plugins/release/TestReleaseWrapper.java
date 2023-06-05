/*
 * The MIT License
 *
 * Copyright (c) 2017, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.release;

import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.HttpMethod;
import org.htmlunit.Page;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlCheckBoxInput;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestReleaseWrapper {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @Issue("SECURITY-607")
    @Ignore("Html unit is not adding in release action on submit, configuration through UI works")
    public void ensurePostIsRequiredForSubmit() throws Exception {
        j.jenkins.setCrumbIssuer(null);
        JenkinsRule.WebClient wc = j.createWebClient();

        FreeStyleProject job = j.createFreeStyleProject("test");
        String submitUrl = job.getShortUrl() + "release/submit";
        URL absoluteSubmitUrl = new URL(j.getURL() + submitUrl);
        WebRequest postSubmit = new WebRequest(absoluteSubmitUrl, HttpMethod.POST);

        // the release plugin is not activated yet, so the page does not exist for the moment
        try {
            wc.getPage(postSubmit);
            fail();
        } catch (FailingHttpStatusCodeException e) {
            assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
        }

        // enable the release feature for that job
        HtmlForm configForm = j.createWebClient().getPage(job, "configure").getFormByName("config");
        HtmlCheckBoxInput releaseCheckButton = configForm.getInputByName("hudson-plugins-release-ReleaseWrapper");
        releaseCheckButton.setChecked(true);
        releaseCheckButton.setAttribute("checked", "checked");
        HtmlPage configPageSubmit = j.submit(configForm);
        assertEquals(HttpStatus.SC_OK, configPageSubmit.getWebResponse().getStatusCode());

        // the GET method is not allowed
        try {
            wc.goTo(submitUrl);
            fail();
        } catch (FailingHttpStatusCodeException e) {
            Assert.assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, e.getStatusCode());
        }

        // the POST method must be used to submit a form
        Page successSubmit = wc.getPage(postSubmit);
        Assert.assertEquals(HttpStatus.SC_OK, successSubmit.getWebResponse().getStatusCode());
    }
}
