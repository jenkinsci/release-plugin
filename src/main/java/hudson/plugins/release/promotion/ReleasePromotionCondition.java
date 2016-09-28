/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.release.promotion;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.promoted_builds.PromotionBadge;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.PromotionConditionDescriptor;
import hudson.plugins.release.ReleaseWrapper.ReleaseBuildBadgeAction;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Add support for the promotion plugin by adding a condition that the build
 * must be a released build.
 *
 * @author Peter Hayes
 */
public class ReleasePromotionCondition extends PromotionCondition {

    @Override
    public PromotionBadge isMet(AbstractBuild<?, ?> build) {
        if (build.getAction(ReleaseBuildBadgeAction.class) != null)
            return new Badge();
        
        return null;
    }

    /**
     * Register a new instance of {@link PromotionConditionDescriptor} to this {@link PromotionCondition}
     *
     * @deprecated Caused duplication of descriptor (see JENKINS-11176). Replaced by the Extension annotation in
     * the {@link PromotionConditionDescriptor}.
     */
    @Deprecated
    public static void registerExtension() {}

    public static final class Badge extends PromotionBadge {
        
    }

    @Extension(optional = true)
    public static final class DescriptorImpl extends PromotionConditionDescriptor {

        public boolean isApplicable(AbstractProject<?,?> item) {
            return true;
        }

        public String getDisplayName() {
            return Messages.ReleasePromotionCondition_DisplayName();
        }

        public String getHelpFile() {
            return "/plugin/release/promotion/release.html";
        }

        public PromotionCondition newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new ReleasePromotionCondition();
        }
    }
}
