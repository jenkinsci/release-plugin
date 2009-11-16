package hudson.plugins.release.dashboard;

import hudson.Extension;
import hudson.FeedAdapter;
import hudson.model.Cause;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.RSS;
import hudson.model.Run;
import hudson.model.User;
import hudson.model.Cause.UserCause;
import hudson.plugins.release.ReleaseWrapper.ReleaseBuildBadgeAction;
import hudson.plugins.view.dashboard.DashboardPortlet;
import hudson.tasks.Mailer;
import hudson.util.RunList;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class RecentReleasesPortlet extends DashboardPortlet {

	@DataBoundConstructor
	public RecentReleasesPortlet(String name) {
		super(name);
	}
	
	public Collection<Run> getRecentReleases(int max) {
		LinkedList<Run> recentReleases = new LinkedList<Run>();
		
		for (Job job : getDashboard().getJobs()) {
			// Keep going through jobs while there are more builds
			// and (the recent releases is less than max or this run 
			// is newer than the last in the list)
			for (Run run = job.getLastCompletedBuild(); run != null && (recentReleases.size() < max || run.getTimestamp().compareTo(recentReleases.getLast().getTimestamp()) > 0); run = run.getPreviousBuild()) {
				ReleaseBuildBadgeAction rbb = run.getAction(ReleaseBuildBadgeAction.class);
				
				if (rbb != null) {
					// if you couldn't insert run, stop trying this job as next runs
					// are even older
					if (!insertRun(run, recentReleases, max)) {
						break;
					}
				}
			}
		}
		
		return recentReleases;
	}
	
	/**
	 * Get the release version from this run
	 * @param run Must be a release run - i.e. have a ReleaseBuildBadgeAction
	 * @return
	 */
	public String getReleaseVersion(Run run) {
		ReleaseBuildBadgeAction rbb = run.getAction(ReleaseBuildBadgeAction.class);
		
		return rbb.getReleaseVersion();
	}
	
	private boolean insertRun(Run run, LinkedList<Run> recentReleases, int max) {
		ListIterator<Run> iter = recentReleases.listIterator();
		Run recentRun = null;
		
		do {
			if (iter.hasNext()) {
				recentRun = iter.next();
			} else {
				recentRun = null;
			}
			
			// if we're at the end of the recent releases list and the list has room for another
			// or this run is more recent than current position, then insert
			if ((recentRun == null && recentReleases.size() < max) || (recentRun != null && run.getTimestamp().compareTo(recentRun.getTimestamp()) > 0)) {
				// back up one and add to list
				if (!recentReleases.isEmpty() && recentRun != null) {
					iter.previous();
				}
				
				iter.add(run);
				
				// remove last on list if size == max - might be removing what we just added
				if (recentReleases.size() > max) {
					recentReleases.removeLast();
				}
				
				return true;
			}
		} while (recentRun != null);
		
		return false;
	}
	
	public void doRssAll( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        rss(req, rsp, " all builds", RunList.fromRuns(getRecentReleases(20)));
    }

    public void doRssFailed( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        rss(req, rsp, " failed builds", RunList.fromRuns(getRecentReleases(20)).failureOnly());
    }

    private void rss(StaplerRequest req, StaplerResponse rsp, String suffix, RunList runs) throws IOException, ServletException {
        RSS.forwardToRss(getDisplayName()+ suffix, getDashboard().getUrl() + getUrl(),
            runs.newBuilds(), new RelativePathFeedAdapter(getDashboard().getUrl() + getUrl()), req, rsp );
    }
	
    public static class DescriptorImpl extends Descriptor<DashboardPortlet> {
		
		@Extension
		public static DescriptorImpl newInstance() {
			if (Hudson.getInstance().getPlugin("dashboard-view") != null) {
				return new DescriptorImpl();
			} else {
				return null;
			}
		}

		@Override
		public String getDisplayName() {
			return "Recent Releases";
		}
	}
	
	private class RelativePathFeedAdapter implements FeedAdapter<Run> {
		private String url;
		
		RelativePathFeedAdapter(String url) {
			this.url = url;
		}
		
        public String getEntryTitle(Run entry) {
            return entry+" ("+entry.getResult()+")";
        }

        public String getEntryUrl(Run entry) {
            return url + entry.getUrl();
        }

        public String getEntryID(Run entry) {
            return "tag:" + "hudson.dev.java.net,"
                + entry.getTimestamp().get(Calendar.YEAR) + ":"
                + entry.getParent().getName()+':'+entry.getId();
        }

        public String getEntryDescription(Run entry) {
        	return getReleaseVersion(entry);
        }

        public Calendar getEntryTimestamp(Run entry) {
            return entry.getTimestamp();
        }

        public String getEntryAuthor(Run entry) {
        	// release builds are manual so get the UserCause
        	// and report rss entry as user who kicked off build
        	List<Cause> causes = entry.getCauses();
        	for (Cause cause : causes) {
        		if (cause instanceof UserCause) {
        			return User.get(((UserCause) cause).getUserName()).getFullName();
        		}
        	}
        	
        	// in the unexpected case where there is no user cause, return admin
            return Mailer.descriptor().getAdminAddress();
        }
    }
}
