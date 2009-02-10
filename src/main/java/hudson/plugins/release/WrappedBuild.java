package hudson.plugins.release;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Api;
import hudson.model.BallColor;
import hudson.model.BuildBadgeAction;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.User;
import hudson.model.Fingerprint.RangeSet;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.search.Search;
import hudson.security.ACL;
import hudson.security.Permission;
import hudson.tasks.test.AbstractTestResultAction;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Wraps another build instance in order to add variables accessible
 * to the build
 *
 * @author Peter Hayes
 * @since 0.1
 */
public class WrappedBuild<P extends AbstractProject<P,R>,R extends AbstractBuild<P,R>> extends AbstractBuild<P, R> {
    private AbstractBuild<P, R> build;
    private Map<String, String> variables;

    public WrappedBuild(AbstractBuild<P, R> build, Map<String, String> variables) throws IOException {
        super(build.getProject(), build.getTimestamp());
        
        this.build = build;
        this.variables = variables;
    }

    public void addAction(Action a) {
        build.addAction(a);
    }

    public void checkPermission(Permission p) {
        build.checkPermission(p);
    }

    public int compareTo(R that) {
        return build.compareTo(that);
    }

    public void delete() throws IOException {
        build.delete();
    }

    public void doArtifact(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException,
            InterruptedException {
        build.doArtifact(req, rsp);
    }

    public void doBuildNumber(StaplerRequest req, StaplerResponse rsp) throws IOException {
        build.doBuildNumber(req, rsp);
    }

    public void doBuildStatus(StaplerRequest req, StaplerResponse rsp) throws IOException {
        build.doBuildStatus(req, rsp);
    }

    public void doBuildTimestamp(StaplerRequest req, StaplerResponse rsp, String format) throws IOException {
        build.doBuildTimestamp(req, rsp, format);
    }

    public void doDoDelete(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        build.doDoDelete(req, rsp);
    }

    public void doProgressiveLog(StaplerRequest req, StaplerResponse rsp) throws IOException {
        build.doProgressiveLog(req, rsp);
    }

    public void doStop(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        build.doStop(req, rsp);
    }

    public void doSubmitDescription(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        build.doSubmitDescription(req, rsp);
    }

    public void doToggleLogKeep(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        build.doToggleLogKeep(req, rsp);
    }

    public Calendar due() {
        return build.due();
    }

    public boolean equals(Object obj) {
        return build.equals(obj);
    }

    public ACL getACL() {
        return build.getACL();
    }

    public <T extends Action> T getAction(Class<T> type) {
        return build.getAction(type);
    }

    public Action getAction(int index) {
        return build.getAction(index);
    }

    public List<Action> getActions() {
        return build.getActions();
    }

    public <T extends Action> List<T> getActions(Class<T> type) {
        return build.getActions(type);
    }

    public Api getApi(StaplerRequest req) {
        return build.getApi(req);
    }

    public List<Artifact> getArtifacts() {
        return build.getArtifacts();
    }

    public File getArtifactsDir() {
        return build.getArtifactsDir();
    }

    public List<BuildBadgeAction> getBadgeActions() {
        return build.getBadgeActions();
    }

    public Summary getBuildStatusSummary() {
        return build.getBuildStatusSummary();
    }

    public String getBuildStatusUrl() {
        return build.getBuildStatusUrl();
    }

    public Map<String, String> getBuildVariables() {
        return build.getBuildVariables();
    }

    public Node getBuiltOn() {
        return build.getBuiltOn();
    }

    public String getBuiltOnStr() {
        return build.getBuiltOnStr();
    }

    public ChangeLogSet<? extends Entry> getChangeSet() {
        return build.getChangeSet();
    }

    public Set<User> getCulprits() {
        return build.getCulprits();
    }

    public Map<AbstractProject, DependencyChange> getDependencyChanges(AbstractBuild from) {
        return build.getDependencyChanges(from);
    }

    public String getDescription() {
        return build.getDescription();
    }

    public String getDisplayName() {
        return build.getDisplayName();
    }

    public Map<AbstractProject, RangeSet> getDownstreamBuilds() {
        return build.getDownstreamBuilds();
    }

    public Iterable<AbstractBuild<?, ?>> getDownstreamBuilds(AbstractProject<?, ?> that) {
        return build.getDownstreamBuilds(that);
    }

    public RangeSet getDownstreamRelationship(AbstractProject that) {
        return build.getDownstreamRelationship(that);
    }

    public long getDuration() {
        return build.getDuration();
    }

    public String getDurationString() {
        return build.getDurationString();
    }

    public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        return build.getDynamic(token, req, rsp);
    }

    public Map<String, String> getEnvVars() {
        Map<String, String> envVars = build.getEnvVars();
        
        envVars.putAll(variables);
        
        return envVars;
    }

    public Executor getExecutor() {
        return build.getExecutor();
    }

    public boolean getHasArtifacts() {
        return build.getHasArtifacts();
    }

    public BallColor getIconColor() {
        return build.getIconColor();
    }

    public String getId() {
        return build.getId();
    }

    public String getLog() throws IOException {
        return build.getLog();
    }

    public List<String> getLog(int maxLines) throws IOException {
        return build.getLog(maxLines);
    }

    public File getLogFile() {
        return build.getLogFile();
    }

    public R getNextBuild() {
        return build.getNextBuild();
    }

    public int getNumber() {
        return build.getNumber();
    }

    public P getParent() {
        return build.getParent();
    }

    public R getPreviousBuild() {
        return build.getPreviousBuild();
    }

    public R getPreviousFailedBuild() {
        return build.getPreviousFailedBuild();
    }

    public R getPreviousNotFailedBuild() {
        return build.getPreviousNotFailedBuild();
    }

    public Result getResult() {
        return build.getResult();
    }

    public File getRootDir() {
        return build.getRootDir();
    }

    public Search getSearch() {
        return build.getSearch();
    }

    public String getSearchName() {
        return build.getSearchName();
    }

    public AbstractTestResultAction getTestResultAction() {
        return build.getTestResultAction();
    }

    public Calendar getTimestamp() {
        return build.getTimestamp();
    }

    public String getTimestampString() {
        return build.getTimestampString();
    }

    public String getTimestampString2() {
        return build.getTimestampString2();
    }

    public Map<AbstractProject, Integer> getTransitiveUpstreamBuilds() {
        return build.getTransitiveUpstreamBuilds();
    }

    public String getTruncatedDescription() {
        return build.getTruncatedDescription();
    }

    public Map<AbstractProject, Integer> getUpstreamBuilds() {
        return build.getUpstreamBuilds();
    }

    public int getUpstreamRelationship(AbstractProject that) {
        return build.getUpstreamRelationship(that);
    }

    public AbstractBuild<?, ?> getUpstreamRelationshipBuild(AbstractProject<?, ?> that) {
        return build.getUpstreamRelationshipBuild(that);
    }

    public String getUpUrl() {
        return build.getUpUrl();
    }

    public String getUrl() {
        return build.getUrl();
    }

    public String getWhyKeepLog() {
        return build.getWhyKeepLog();
    }

    public boolean hasChangeSetComputed() {
        return build.hasChangeSetComputed();
    }

    public int hashCode() {
        return build.hashCode();
    }

    public boolean hasntStartedYet() {
        return build.hasntStartedYet();
    }

    public boolean hasParticipant(User user) {
        return build.hasParticipant(user);
    }

    public boolean hasPermission(Permission p) {
        return build.hasPermission(p);
    }

    public boolean isBuilding() {
        return build.isBuilding();
    }

    public boolean isLogUpdated() {
        return build.isLogUpdated();
    }

    public void keepLog(boolean newValue) throws IOException {
        build.keepLog(newValue);
    }
    
    public void run() {
        build.run();
    }

    public void save() throws IOException {
        build.save();
    }

    public void setDescription(String description) throws IOException {
        build.setDescription(description);
    }

    public void setResult(Result r) {
        build.setResult(r);
    }

    public String toString() {
        return "WrappedBuild: " + build.toString();
    }
    
    
}
