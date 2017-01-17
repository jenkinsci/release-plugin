package hudson.plugins.release;

import java.io.IOException;
import java.util.List;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;

@Restricted(NoExternalUse.class)
public class SafeParametersAction extends ParametersAction {

    /**
     * At this point the list of parameter values is guaranteed to be safe, which is
     * parameter defined either at top level or release wrapper level.
     *
     * @param parameters parameters allowed by the job and parameters allowed by release-specific parameters definition
     * @since 2.7 - public constructor
     */
    public SafeParametersAction(List<ParameterValue> parameters) {
        super(parameters);
    }

    @Extension
    public static final class SafeParametersActionEnvironmentContributor extends EnvironmentContributor {

        @Override
        public void buildEnvironmentFor(Run r, EnvVars envs, TaskListener listener) throws IOException, InterruptedException {
            SafeParametersAction action = r.getAction(SafeParametersAction.class);
            if (action != null) {
                for (ParameterValue p : action.getParameters()) {
                    envs.put(p.getName(), String.valueOf(p.getValue()));
                }
            }
        }
    }

}
