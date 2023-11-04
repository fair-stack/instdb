//package cn.cnic.instdb.utils;
//
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import javax.mail.*;
//import javax.mail.internet.*;
//import java.io.UnsupportedEncodingException;
//import java.util.Properties;
//
///**
// *  Email sending
// * @author chl
// * @date 2021/3/19
// */
//@Component
//public class EmailUtil {
//
//    @Value("${mail.protocol}")
//    private String protocol;
//    @Value("${mail.port}")
//    private Integer mailPort;
//    //Email account for sending emails
//    @Value("${mail.username}")
//    private String username;
//    //Authorization code of the email account that sent the email
//    @Value("${mail.password}")
//    private String password;
//    //Send email server
//    @Value("${mail.host}")
//    private String mailSmtpHost;
//
//    /**
//     * Sending an email
//     * Text content supporthtmlText content support
//     *
//     * @param sendEmail sender name default dplatform
//     * @param subject
//     * @param content
//     * @throws MessagingException
//     */
//    public boolean sendMail(String sendEmail,Address []sendEmails, String subject, String content)  {
//
//        // Attribute Object
//        Properties properties = properties();
//        // environmental information 
//        Session session = Session.getInstance(properties, new Authenticator() {
//            @Override
//            protected PasswordAuthentication getPasswordAuthentication() {
//                // staysessionstay，TransportstayW
//                return new PasswordAuthentication(username, password);
//            }
//        });
//        try {
//            MimeMultipart multipart = getMultipart(content);
//            MimeMessage mimeMessage = getMimeMessage(session, subject, sendEmail,sendEmails);
//            //Set up emailMINESet up email
//            mimeMessage.setContent(multipart);
//            //Sending an email
//            Transport.send(mimeMessage, mimeMessage.getAllRecipients());
//        }catch (UnsupportedEncodingException e1){
//            e1.printStackTrace();
//            return false;
//        }catch (MessagingException e2){
//            e2.getNextException();
//            return false;
//        }catch (Exception e3){
//            e3.printStackTrace();
//            return false;
//        }
//        return true;
//    }
//
//    protected Properties properties() {
//        // Attribute Object
//        Properties properties = new Properties();
//        // opendebugopen ，open
//        properties.setProperty("mail.debug", "false");
//        // The sending server requires authentication
//        properties.setProperty("mail.smtp.auth", "true");
//        // Send Server Port，Send Server Port，Send Server Port25
//        properties.setProperty("mail.smtp.port", mailPort.toString());
//
//        properties.setProperty("mail.smtp.ssl.enable","true");
//        // Send email protocol name
//        properties.setProperty("mail.transport.protocol", protocol);
//        // Set mail server host name
//        properties.setProperty("mail.host", mailSmtpHost);
//
//        return properties;
//    }
//
//
//    /**
//     *  Set basic information for email
//     * @param session
//     * @param subject
//     * @param sendEmail
//     */
//    private MimeMessage getMimeMessage(Session session, String subject, String sendEmail, Address []sendEmails) throws UnsupportedEncodingException, MessagingException {
//        //mail
//        MimeMessage msg = new MimeMessage(session);
//        //set up themes
//        msg.setSubject(subject);
//        //From，From
//        msg.setFrom(new InternetAddress("\"" + MimeUtility.encodeText("Instdb") + "\"<" + username + ">"));
//        //Set email responders
////        msg.setReplyTo(new Address[]{new InternetAddress("harry.hu@derbysoft.com")});
//
//        if(StringUtils.isNotBlank(sendEmail)){
//            msg.setRecipients(Message.RecipientType.TO, sendEmail);
//        }else if(null != sendEmails && sendEmails.length>0){
//            msg.setRecipients(Message.RecipientType.TO, sendEmails);
//        }
//
//
////        if (!StringUtils.isEmpty(cc)) {
////            msg.setRecipients(Message.RecipientType.CC, cc);
////        }
//        return msg;
//    }
//
//    /**
//     *  Set up emailMINESet up email
//     * @param content
//     * @return
//     */
//    private MimeMultipart getMultipart(String content) throws MessagingException {
//        //The entire emailMINEThe entire email
//        MimeMultipart msgMultipart = new MimeMultipart("mixed");//Mixed combination relationship
//
//        // Loading attachments
////        if (bytes != null && names != null) {
////            for (int i = 0; i < bytes.length; i++) {
////                MimeBodyPart attch = new MimeBodyPart(); // attachment
////                msgMultipart.addBodyPart(attch);         // Add attachments toMIMEAdd attachments to
////                ByteArrayDataSource dataSource = new ByteArrayDataSource(bytes[i], "text/data"); //data source
////                attch.setDataHandler(new DataHandler(dataSource));
////                attch.setFileName(names[i]);
////            }
////        }
//
//        //htmlCode section
//        MimeBodyPart htmlPart = new MimeBodyPart();
//        msgMultipart.addBodyPart(htmlPart);
//        //htmlcode
//        htmlPart.setContent(content, "text/html;charset=utf-8");
//        return msgMultipart;
//    }
//
//
//
//}
