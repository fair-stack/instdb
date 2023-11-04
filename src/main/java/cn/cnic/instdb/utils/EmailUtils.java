package cn.cnic.instdb.utils;

import cn.cnic.instdb.cacheLoading.CacheLoading;
import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.commentNotice.UserSendEmail;
import cn.cnic.instdb.model.config.BasicConfigurationVo;
import cn.cnic.instdb.model.config.EmailConfig;
import cn.cnic.instdb.model.system.EmailErrorInfo;
import cn.cnic.instdb.model.system.EmailModel;
import cn.cnic.instdb.model.system.ToEmail;
import cn.cnic.instdb.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.util.*;

/**
 * @Auther: wdd
 * @Date: 2021/03/17/16:52
 * @Description:
 */
@Slf4j
@Component
public class EmailUtils {


    @Resource
    private TemplateEngine templateEngine;

    @Resource
    private SystemConfigService systemConfigService;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private InstdbUrl instdbUrl;

    private static JavaMailSenderImpl javaMailSender = null;

    private String from;

    /**
     *  loading
     * @return
     */
    public synchronized JavaMailSenderImpl getInstance(){
        initInstance();
        if(javaMailSender == null){
            CacheLoading cacheLoading = new CacheLoading(mongoTemplate);
            EmailConfig emailConfig = cacheLoading.getEmailConfig();
            if(null == emailConfig){
                return null;
            }
            from = emailConfig.getFrom();
            javaMailSender = new JavaMailSenderImpl();
            javaMailSender.setHost(emailConfig.getHost());
            javaMailSender.setPort(emailConfig.getPort());
            javaMailSender.setUsername(emailConfig.getUsername());
            if ("other".equals(emailConfig.getType())) {
                javaMailSender.setPassword(RSAEncrypt.decrypt(emailConfig.getPasswordOther()));
            } else {
                javaMailSender.setPassword(RSAEncrypt.decrypt(emailConfig.getPassword()));
            }
            javaMailSender.setProtocol(emailConfig.getProtocol());
            Properties properties = new Properties();
            properties.put("mail.smtp.auth",true);
            properties.put("mail.smtp.starttls.enable",true);
            properties.put("mail.smtp.starttls.required",true);
            properties.put("mail.smtp.ssl.enable",true);
            javaMailSender.setJavaMailProperties(properties);
        }
        return javaMailSender;
    }

    /**
     *  initialization
     */
    public synchronized static void initInstance(){
        if(javaMailSender != null){
            javaMailSender = null;
        }
    }


    /**
     * Send template email Send template emailthymeleafSend template email
     * @param toEmail
     * @param attachment
     * @param emailModel
     */
    public void sendTemplateMail(ToEmail toEmail, Map<String, Object> attachment, EmailModel emailModel) {
        try {
            //Email configuration recipient filtering processing
            Query query = new Query();
            String type = emailModel.getType();
            if ("EMAIL_RESOURCES_APPROVE".equals(type)) {
                query.addCriteria(Criteria.where("waitApproval").is(1));
            } else if ("EMAIL_APPLY_ADOPT".equals(type)) {
                query.addCriteria(Criteria.where("approved").is(1));
            } else if ("EMAIL_APPLY_REJECT".equals(type)) {
                query.addCriteria(Criteria.where("approvalRejected").is(1));
            } else if ("EMAIL_RESOURCES_REVOKE".equals(type)) {
                query.addCriteria(Criteria.where("approvalRevocation").is(1));
            } else if ("VERSION_UP".equals(type)) {
                query.addCriteria(Criteria.where("versionUp").is(1));
            }

            List<UserSendEmail> userSendEmails = null;
            if (query.getQueryObject().size() > 0) {
                userSendEmails = mongoTemplate.find(query, UserSendEmail.class);
            }

            List<String> list = new ArrayList<>(Arrays.asList(toEmail.getTos()));
            Iterator<String> iterator = list.iterator();
            while (iterator.hasNext()) {
                String next = iterator.next();
                if (next.equals(Constant.USERNAME)) {
                    iterator.remove();
                    continue;
                }
                //Remove email accounts that have been banned from sending emails
                if (null != userSendEmails && userSendEmails.size() > 0) {
                    for (UserSendEmail email : userSendEmails) {
                        if (email.getEmailAccounts().equals(next)) {
                            iterator.remove();
                            continue;
                        }
                    }

                }
            }
            if (list.size() < 1) {
                return;
            }

            String[] tos = list.stream().toArray(String[]::new);
            toEmail.setTos(tos);
            Map basic = getBasic();

            if (emailModel.getMessage().contains("instdbName")) {
                emailModel.setMessage(emailModel.getMessage().replaceAll("instdbName", basic.get("org").toString()));
            }
            if (emailModel.getMessage().contains("org")) {
                emailModel.setMessage(emailModel.getMessage().replaceAll("org", basic.get("org").toString()));
            }

            String message = emailModel.getMessage().replaceAll("name", attachment.get("name").toString()).replaceAll("instdbName", basic.get("org").toString());
            String call = emailModel.getCall().replaceAll("name", attachment.get("name").toString()).replaceAll("instdbName", basic.get("org").toString());

            attachment.put("call", call);
            attachment.put("message", message);
            attachment.put("copyright", basic.get("copyright").toString());
            attachment.put("homeUrl", instdbUrl.getCallHost());
            attachment.put("image", basic.get("image").toString());
            attachment.put("org", basic.get("org").toString());

            toEmail.setSubject("【" + basic.get("org").toString() + emailModel.getSubject());
            MimeMessage mimeMessage = getInstance().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true,"utf-8");
            helper.setFrom(from, basic.get("org").toString());
            helper.setTo(tos);
            if (null != toEmail.getCc() && toEmail.getCc().length > 0) {
                helper.setCc(toEmail.getCc());
            }
            helper.setSubject(toEmail.getSubject());

            Context context = new Context();

            //Define template data
            context.setVariables(attachment);
            helper.setText(templateEngine.process(emailModel.getTemplate(), context), true);
            getInstance().send(mimeMessage);
           // log.info("Successfully sent email：Successfully sent email：{},Successfully sent email：{},Successfully sent email：{}", tos, toEmail.getSubject(), new Date());
        } catch (Exception e) {
            log.error("Template email sending failed->message:{}", e);
            EmailErrorInfo emailErrorInfo= new EmailErrorInfo();
            emailErrorInfo.setTemplate(emailModel.getTemplate());
            emailErrorInfo.setSubject("【" + attachment.get("org").toString() + emailModel.getSubject());
            emailErrorInfo.setFromOrg(attachment.get("org").toString());
            emailErrorInfo.setTos(toEmail.getTos());
            emailErrorInfo.setAttachment(attachment);
            mongoTemplate.save(emailErrorInfo);
            log.error("Failure record saved");
        }
    }


    public boolean sendSimpleMail(ToEmail toEmail, Map<String, Object> attachment, EmailModel emailModel) {
        try {
            Map basic = getBasic();

            //Encapsulation template information
            attachment.put("title", emailModel.getTitle());
            attachment.put("call",  emailModel.getCall());
            attachment.put("message", emailModel.getMessage());
            attachment.put("copyright", basic.get("copyright").toString());
            attachment.put("homeUrl", instdbUrl.getCallHost());
            attachment.put("image", basic.get("image").toString());
            attachment.put("org", basic.get("org").toString());

            attachment.put("button", emailModel.getButton());
            attachment.put("alert", emailModel.getAlert());
            attachment.put("alertTo", emailModel.getAlertTo());
            attachment.put("end", emailModel.getEnd());
            attachment.put("last", emailModel.getLast());


            MimeMessage mimeMessage = getInstance().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true,"utf-8");
            helper.setFrom(from, basic.get("org").toString());
            helper.setTo(toEmail.getTos());
            toEmail.setSubject("【" + basic.get("org").toString() + emailModel.getSubject());
            helper.setSubject(toEmail.getSubject());

            //Define template data
            Context context = new Context();
            context.setVariables(attachment);
            helper.setText(templateEngine.process(emailModel.getTemplate(), context), true);
            getInstance().send(mimeMessage);
            log.info("Successfully sent email：Successfully sent email：{},Successfully sent email：{},Successfully sent email：{},Successfully sent email：{},Successfully sent email：{}", from, toEmail.getTos(), toEmail.getCc(), toEmail.getSubject(), new Date());
            return true;
        } catch (Exception e) {
            log.error("Template email sending failed->message:{}", e.getMessage());
        }
        return false;
    }







    private Map getBasic() {
        Map<String, String> map = new HashMap<>();
        BasicConfigurationVo indexCopyrightLinks = systemConfigService.getBasicConfig();

        String logo = indexCopyrightLinks.getLogo();
        String path = instdbUrl.getBanaer_icoLogo() + logo;

        map.put("image", StringUtils.isBlank(logo) ? Constant.LOGO_BASE64 : CommonUtils.imageToBase64Str(path));
        map.put("org", StringUtils.isBlank(indexCopyrightLinks.getName()) ? "Institutional data repository" : indexCopyrightLinks.getName());
        map.put("copyright", StringUtils.isBlank(indexCopyrightLinks.getCopyright()) ? "" : indexCopyrightLinks.getCopyright());
        return map;
    }

    /**
     * Automatically resend error messages
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void autoSendEmail() {
        List<EmailErrorInfo> emailErrorInfos = mongoTemplate.find(new Query(), EmailErrorInfo.class);
        if (null != emailErrorInfos && emailErrorInfos.size() > 0) {
            for (EmailErrorInfo errorInfo : emailErrorInfos) {
                Map<String, Object> attachment = errorInfo.getAttachment();
                try {
                    MimeMessage mimeMessage = getInstance().createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");
                    helper.setFrom(from, attachment.get("org").toString());
                    helper.setTo(errorInfo.getTos());
                    helper.setSubject(errorInfo.getSubject());
                    //Define template data
                    Context context = new Context();
                    context.setVariables(attachment);
                    helper.setText(templateEngine.process(errorInfo.getTemplate(), context), true);
                    getInstance().send(mimeMessage);
                    log.info("Resend email successfully：Resend email successfully：{},Resend email successfully：{},Resend email successfully：{},Resend email successfully：{}", from, errorInfo.getTos(), errorInfo.getSubject(), new Date());
                    Query query = new Query();
                    query.addCriteria(Criteria.where("_id").is(errorInfo.getId()));
                    mongoTemplate.remove(query, EmailErrorInfo.class);
                } catch (Exception e) {
                    log.error("Resend email failed to send->message:{}", e.getMessage());
                    break;
                }
            }
            log.info("Resend all incorrect emails completed。" + DateUtils.getCurrentDateTimeString());
        }
    }


}
