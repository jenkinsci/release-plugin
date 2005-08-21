package hudson.tasks;

import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Result;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Map;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * Sends the build result in e-mail.
 *
 * @author Kohsuke Kawaguchi
 */
public class Mailer implements BuildStep {

    /**
     * Whitespace-separated list of e-mail addresses.
     */
    private String recipients;


    // TODO: left so that XStream won't get angry. figure out how to set the error handling behavior
    // in XStream.
    private transient String from;
    private transient String subject;
    private transient boolean failureOnly;

    public Mailer(String recipients) {
        this.recipients = recipients;
    }

    public String getRecipients() {
        return recipients;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    public boolean perform(Build build, BuildListener listener) {
        try {
            MimeMessage mail = getMail(build);
            if(mail!=null) {
                listener.getLogger().println("Sending e-mails to "+recipients);
                Transport.send(mail);
            }
        } catch (MessagingException e) {
            e.printStackTrace( listener.error(e.getMessage()) );
        }

        return true;
    }

    private MimeMessage getMail(Build build) throws MessagingException {
        if(build.getResult()==Result.FAILURE) {
            return createFailureMail(build);
        }

        if(build.getResult()==Result.SUCCESS) {
            Build prev = build.getPreviousBuild();
            if(prev!=null && prev.getResult()==Result.FAILURE)
                return createBackToNormalMail(build);
        }

        return null;
    }

    private MimeMessage createBackToNormalMail(Build build) throws MessagingException {
        MimeMessage msg = createEmptyMail();

        msg.setSubject(getSubject(build));
        msg.setSubject("Hudson build is back to normal: "+build.getProject().getName());
        msg.setText("... as of #"+build.getNumber());

        return msg;
    }

    private MimeMessage createFailureMail(Build build) throws MessagingException {
        MimeMessage msg = createEmptyMail();

        msg.setSubject(getSubject(build));
        try {
            msg.setText(build.getLog());
        } catch (IOException e) {
            // somehow failed to read the contents of the log
            StringBuilder w = new StringBuilder();
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            w.append("Failed to access build log\n\n").append(sw);
            msg.setText(w.toString());
        }
        return msg;
    }

    private MimeMessage createEmptyMail() throws MessagingException {
        MimeMessage msg = new MimeMessage(DESCRIPTOR.createSession());
        // TODO: I'd like to put the URL to the page in here,
        // but how do I obtain that?
        msg.setContent("","text/plain");
        msg.setFrom(new InternetAddress(DESCRIPTOR.getAdminAddress()));

        List<InternetAddress> rcp = new ArrayList<InternetAddress>();
        StringTokenizer tokens = new StringTokenizer(recipients);
        while(tokens.hasMoreTokens())
            rcp.add(new InternetAddress(tokens.nextToken()));
        msg.setRecipients(Message.RecipientType.TO, rcp.toArray(new InternetAddress[rcp.size()]));
        return msg;
    }

    public BuildStepDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    private String getSubject(Build build) {
        return "Build failed in Hudson: "+build.getProject().getName()+" #"+build.getNumber();
    }


    public static final Descriptor DESCRIPTOR = new Descriptor();

    public static final class Descriptor extends BuildStepDescriptor {

        public Descriptor() {
            super(Mailer.class);
        }

        public String getDisplayName() {
            return "E-mail Notification";
        }

        /** JavaMail session. */
        public Session createSession() {
            Properties props = new Properties(System.getProperties());
            // can't use putAll
            for (Map.Entry o : ((Map<?,?>)getProperties()).entrySet()) {
                if(o.getValue()!=null)
                    props.put(o.getKey(),o.getValue());
            }
            return Session.getInstance(props);
        }

        public boolean configure(HttpServletRequest req) {
            String v = req.getParameter("mailer_smtp_server");
            if(v!=null && v.length()==0)    v=null;
            getProperties().put("mail.smtp.host",v);

            getProperties().put("mail.admin.address",req.getParameter("mailer_admin_address"));
            save();
            return super.configure(req);
        }

        public String getSmtpServer() {
            return (String)getProperties().get("mail.smtp.host");
        }

        public String getAdminAddress() {
            String v = (String)getProperties().get("mail.admin.address");
            if(v==null)     v = "address not configured yet <nobody>";
            return v;
        }

        public BuildStep newInstance(HttpServletRequest req) {
            return new Mailer(
                req.getParameter("mailer_recipients")
            );
        }
    };
}
