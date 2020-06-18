import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class Emailer {

    private final String myEmail;
    private final String password;

    /**
     * Constructor to create an Emailer Object.
     * @param myEmail Gmail username.
     * @param password Gmail password.
     */
    public Emailer(String myEmail, String password){
        this.myEmail = myEmail;
        this.password = password;
    }

    /**
     * Method for sending an email.
     * @param toEmail recipient email address.
     * @param subject topic of the email.
     * @param text text needed to be send.
     * @param filePath file path from where to pick up attached files.
     * @throws MessagingException
     */
    public void sendEmail(String toEmail, String subject, String text, String filePath) throws MessagingException {
        Properties properties = new Properties();

        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(myEmail, password);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(myEmail));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        msg.setSubject(subject);

        Multipart multipart = new MimeMultipart();

        MimeBodyPart textBodyPart = new MimeBodyPart();
        textBodyPart.setText(text);

        MimeBodyPart attachmentBodyPart= new MimeBodyPart();
        DataSource source = new FileDataSource(filePath);
        attachmentBodyPart.setDataHandler(new DataHandler(source));
        attachmentBodyPart.setFileName("xmlfile");

        multipart.addBodyPart(textBodyPart);  // add the text part
        multipart.addBodyPart(attachmentBodyPart); // add the attachement part

        msg.setContent(multipart);

        Transport.send(msg);
    }
}
