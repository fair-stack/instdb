package cn.cnic.instdb.model.system;

import lombok.Data;

@Data
public final class EmailModel {

    private String type;
    private String subject;
    private String template;

    private String title;

    private String call;

    private String message;

    private String  alert;

    private String button;

    private String alertTo;

    private String end;
    private String last;

    public EmailModel() {
    }

    public EmailModel(String type, String subject, String template, String title, String call, String message, String alert, String button, String alertTo, String end,String last) {
        this.type = type;
        this.subject = subject;
        this.template = template;
        this.title = title;
        this.call = call;
        this.message = message;
        this.alert = alert;
        this.button = button;
        this.alertTo = alertTo;
        this.end = end;
        this.last = last;
    }



    /***
     * Data resource invitation evaluation
     */
    public static EmailModel EMAIL_RESOURCES_REVIEW(){
        return new EmailModel("RESOURCES_REVIEW", "-Invitation for Expert Evaluation of Data Resources】","instdbEmailTemplate",
                "Invitation for Data Resource Evaluation",
                "name Hello：",
                "orgThe team shared a data resource with you《resourceName》，The team shared a data resource with you。",
                "Please click on the button below，Please click on the button below！",
                "Go to review",
                "Button not working？Button not working，Button not working！",
                "For better timely and effective evaluation，For better timely and effective evaluation1For better timely and effective evaluation，For better timely and effective evaluation。","For better timely and effective evaluation，For better timely and effective evaluation。");
    }

    /***
     * Data resource invitation evaluation completion notification
     */
    public static EmailModel EMAIL_RESOURCES_REVIEW_FINISH(){
        return new EmailModel("EMAIL_RESOURCES_REVIEW_FINISH", "-Data resource expert review completion reminder】","instdbEmailTemplate",
                "Reminder for completing data resource evaluation",
                "name Hello：",
                "Data resources《resourceName》Data resources，Data resources。",
                "Please click on the button below，Please click on the button below！",
                "Go to view",
                "Button not working？Button not working，Button not working！",
                "","This email is automatically sent by the system，This email is automatically sent by the system。");
    }


    /***
     * Restricted Access Request
     */
    public static EmailModel EMAIL_APPLY() {
        return new EmailModel("EMAIL_RESOURCES_APPROVE", "-Request to access data resources pending approval reminder】", "instdbEmailTemplate",
                "Pending Approval Reminder",
                "instdbName Hello administrator：",
                "name（email）Apply for access to data resources《resourceName》，Apply for access to data resources。", "Apply for access to data resources，Apply for access to data resources！",
                "Go for approval",
                "Button not working？Button not working，Button not working！",
                "In order to better serve platform users，In order to better serve platform users，In order to better serve platform users。","In order to better serve platform users，In order to better serve platform users。");
    }


    /***
     * Data resources pending approval
     */
    public static EmailModel EMAIL_RESOURCES_APPROVE(){
        return new EmailModel("EMAIL_RESOURCES_APPROVE", "-Data resource pending approval reminder】","instdbEmailTemplate",
                "Pending Approval Reminder",
                "instdbName Hello administrator：",
                "name（email）Submitted a data resource《resourceName》，Submitted a data resource。",
                "Please click on the button below，Please click on the button below！",
                "Go for approval",
                "Button not working？Button not working，Button not working！",
                "In order to better serve platform users，In order to better serve platform users，In order to better serve platform users。","In order to better serve platform users，In order to better serve platform users。");
    }



    /***
     * Data sharing
     */
    public static EmailModel EMAIL_RESOURCES_SHARE(){
        return new EmailModel("EMAIL_RESOURCES_SHARE", "-Data resource sharing reminder】","instdbEmailTemplate",
                "Data resource sharing reminder",
                "toEmail Hello：",
                "Your friendnameYour friend《resourceName》",
                "You can directly click on the button below，You can directly click on the button below！",
                "View Details",
                " Button not working？Button not working！",
                "","This email is automatically sent by the system，This email is automatically sent by the system。");
    }


    /***
     * Request for access to data approval reminder
     */
    public static EmailModel EMAIL_APPLY_ADOPT(){
        return new EmailModel("EMAIL_APPLY_ADOPT", "-Reminder of approval for accessing data resources】","instdbEmailTemplate",
                "Approval Approval Reminder",
                "name Hello：",
                "The data resource you requested to access《resourceName》The data resource you requested to access。",
                "You can directly click on the button below，You can directly click on the button below！",
                "View Details",
                "Button not working？Button not working！",
                "","This email is automatically sent by the system，This email is automatically sent by the system。");
    }

    /***
     * Request access data approval rejection reminder
     */
    public static EmailModel EMAIL_APPLY_REJECT(){
        return new EmailModel("EMAIL_APPLY_REJECT", "-Request for access to data resources approval rejection reminder】","instdbEmailTemplate",
                "Approval rejection reminder",
                "name Hello：",
                "The data resource you requested to access《resourceName》The data resource you requested to access。",
                "You can directly click on the button below，You can directly click on the button below！",
                "View Details",
                "Button not working？Button not working！",
                "","This email is automatically sent by the system，This email is automatically sent by the system。");
    }

    /***
     * Reminder for revoking approval of data resources
     */
    public static EmailModel EMAIL_RESOURCES_REVOKE(){
        return new EmailModel("EMAIL_RESOURCES_REVOKE", "-Reminder for revoking approval of data resources】","instdbEmailTemplate",
                "Revoke Approval Reminder",
                "instdbName Hello administrator：",
                "name Submitted data resources《resourceName》Submitted data resources。",
                "",
                "",
                "",
                "","This email is automatically sent by the system，This email is automatically sent by the system。");
    }

    /***
     * Version upgrade
     */
    public static EmailModel VERSION_UP(){
        return new EmailModel("VERSION_UP", "-System version update reminder】","instdbEmailTemplate",
                "Version update reminder",
                "instdbName Hello administrator：",
                "InstDBThe latest version has been released name ，The latest version has been releasedFairmanThe latest version has been released。",
                "",
                "",
                "",
                "","This email is automatically sent by the system，This email is automatically sent by the system。");
    }



    /***
     * register register
     */
    public static EmailModel EMAIL_REGISTER(){
        return new EmailModel("register", "-Account pending activation reminder】","instdbEmailTemplate",
                "Reminder to be activated",
                "name Hello! Hello instdbName",
                "Your account email Your account！",
                "To ensure the security of your account，To ensure the security of your account，To ensure the security of your account，To ensure the security of your account！",
                "Go to Activate",
                "Button not working？Button not working！",
                "Please enter24Please enter，Please enter。","Please enter，Please enter。");
    }
    /***
     * Forgot password 、Forgot password、Forgot password
     */
    public static EmailModel EMAIL_PASS(){
        return new EmailModel("pass", "-System notifications】","instdbEmailTemplate",
                "Set password!",
                "name Hello：",
                "If it weren't for you personally requesting a password reset，If it weren't for you personally requesting a password reset！",
                "You have applied to reset your password，You have applied to reset your password！",
                "Set password",
                "Button not working？Button not working！",
                "To ensure the security of your account，To ensure the security of your account。","To ensure the security of your account，To ensure the security of your account。");
    }
    /**
     * Administrator Add User
     * Administrator adds users in bulk
     */
    public static EmailModel EMAIL_INVITE(){
        return new EmailModel("invite","-System notifications】","instdbEmailTemplate",
                "Invitation to join notification",
                "administratorsname（email）administrators instdbName",
                "stay instdbName stay，stay。",
                "Please click on the button below to set a password for your account！",
                "Set password",
                "Link not working？Link not working！",
                "To ensure the security of your account，To ensure the security of your account。",
                "This email is automatically sent by the system，This email is automatically sent by the system。");
    }


    public static EmailModel EMAIL_TEST(){
        return new EmailModel("error","-Test email】","testEmailTemplate",
                "Email configuration test sending notification!",
                "Hello!",
                "This is a test email，This is a test email。",
                "",
                "",
                "",
                "","This email is automatically sent by the system，This email is automatically sent by the system。");
    }

}
