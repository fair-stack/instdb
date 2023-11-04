package cn.cnic.instdb.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.io.IOException;

@Slf4j
public class CheckEmail {

    public static final String SENDER_EMAIL = "no-reply@domain.com";//"no-reply@domain.com";
    public static final String SENDER_EMAIL_SERVER = SENDER_EMAIL.split("@")[1];//"domain.com";


    /**
     *
     * @param email  The recipient's email address, it need to be validate if it is real exists or doesn't exists.
     * @return True if email is real exists, false if not.
     */
    public boolean checkEmailMethod(String email) {
        if (!email.matches("[\\w\\.\\-]+@([\\w\\-]+\\.)+[\\w\\-]+")) {
            System.err.println("Format error");
            return false;
        }
        String host = "";
        String hostName = email.split("@")[1];
        Record[] result = null;
        SMTPClient client = new SMTPClient();
        client.setConnectTimeout(8000);  //Set connection timeout,Set connection timeout

        try {
            // lookupMXlookup
            Lookup lookup = new Lookup(hostName, Type.MX);
            lookup.run();
            if (lookup.getResult() != Lookup.SUCCESSFUL) {
                return false;
            } else {
                result = lookup.getAnswers();
            }
/*
			 if(result.length > 1) { // Priority sorting
	                List<Record> arrRecords = new ArrayList<Record>();
	                Collections.addAll(arrRecords, result);
	                Collections.sort(arrRecords, new Comparator<Record>() {

	                    public int compare(Record o1, Record o2) {
	                        return new CompareToBuilder().append(((MXRecord)o1).getPriority(), ((MXRecord)o2).getPriority()).toComparison();
	                    }

	                });
	                host = ((MXRecord)arrRecords.get(0)).getTarget().toString();
	            }
 *
 */
            // Connect to mailbox server
//            for (int i = 0; i < result.length; i++) {
//                System.out.println(result[i].getAdditionalName().toString());
//                System.out.println(((MXRecord)result[i]).getPriority());
//            }
            int count=0;
            for (int i = 0; i < result.length; i++) {
                host = result[i].getAdditionalName().toString();
                try{
                    client.connect(host);	//Connect to the email server that receives the email address
                }catch(Exception e){		//Catch exceptions thrown when connection timeout occurs
                    count++;
                    if(count>=result.length){	//If theMXIf theresultIf the，If theemailIf the
                        return false;
                    }
                }

                if (!SMTPReply.isPositiveCompletion(client.getReplyCode())) {	//Server communication unsuccessful
                    client.disconnect();
                    continue;
                } else {
                    // HELO <$SENDER_EMAIL_SERVER>   //domain.com
                    try{
                        client.login(SENDER_EMAIL_SERVER);   //This step may result in a null pointer exception
                    }catch(Exception e){
                        return false;
                    }
                    client.setSender(SENDER_EMAIL);
                    if(client.getReplyCode()!=250){		//To solve the problemhotmailTo solve the problemMXTo solve the problem=550 OU-001 (SNT004-MC1F43) Unfortunately, messages from 127.0.0.1 weren't sent.
                        client.disconnect();
                        continue;							//holdclient.login holdclient.setSenderhold，holdmxholdmx，holdmxhold，hold
                    }
                    // RCPT TO: <$email>
                    try{
                        client.addRecipient(email);
                    }catch(Exception e){
                        return false;
                    }
                    //Finally, return from the inbox servertrue，Finally, return from the inbox server，Finally, return from the inbox server
                    if (250 == client.getReplyCode()) {
                        return true;
                    }
                    client.disconnect();

                }
            }
//			log+=tempLog;
//			log += ">MAIL FROM: <"+SENDER_EMAIL+">\n";
//			log += "=" + client.getReplyString();
//
//			// RCPT TO: <$email>
//			try{
//				client.addRecipient(email);
//			}catch(Exception e){
//				return false;
//			}
//			log += ">RCPT TO: <" + email + ">\n";
//			log += "=" + client.getReplyString();
//
//			//Finally, return from the inbox servertrue，Finally, return from the inbox server，Finally, return from the inbox server
//			if (250 == client.getReplyCode()) {
//				return true;
//			}
        } catch (Exception e) {
            log.error("context",e);
        } finally {
            try {
                client.disconnect();
            } catch (IOException e) {
            }
        }
        return false;
    }

    /**
     * This method is more accurate than checkEmailMethod(String email);
     *
     * @param email  The recipient's email address, it need to be validate if it is real exists or doesn't exists.
     * @return True if email is real exists, false if not.
     */
    public boolean checkEmail(String email){
        if(email.split("@")[1].equals("qq.com")){
            if( checkEmailMethod(email) && checkEmailMethod(email) && checkEmailMethod(email)){
                return true;
            }else{
                return false;
            }
        }
        return checkEmailMethod(email);
    }

    public static void main(String[] args) {
        CheckEmail ce = new CheckEmail();
        if(ce.checkEmail("admin@126.com")){
            System.out.println("true");
        }else{
            System.out.println("false");
        }
    }

}
