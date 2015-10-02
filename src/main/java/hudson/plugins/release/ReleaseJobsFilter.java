/*
 * The MIT License
 *
 * Copyright (c) 2014, Francois Ritaly / Calypso Technology
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

import hudson.Extension;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor;
import hudson.model.View;
import hudson.views.ViewJobFilter;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Custom view job filter used for filtering (that is keeping) all jobs with the
 * release wrapper configured.
 *
 * @author francois_ritaly
 */
public class ReleaseJobsFilter extends ViewJobFilter {

	@DataBoundConstructor
	public ReleaseJobsFilter() {
	}

	@Override
	public List<TopLevelItem> filter(List<TopLevelItem> added, List<TopLevelItem> all, View filteringView) {
		final List<TopLevelItem> selection = new ArrayList<TopLevelItem>();

		// Filter the 'added' list to only keep the jobs using the release wrapper
		for (final TopLevelItem item : added) {
			if (ViewJobFilterUtils.isReleaseJob(item)) {
				selection.add(item);
			}
		}

		return selection;
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<ViewJobFilter> {
		@Override
		public String getDisplayName() {
			return Messages.ReleaseJobsFilter_DisplayName();
		}

        @Override
        public String getHelpFile() {
            return "/plugin/release/view-filter/help-releaseJobs.html";
        }
	}
}