package cn.cnic.instdb.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;


@Slf4j
public class HttpClient {
    final String CONTENT_TYPE_TEXT_JSON = "text/json";

    /**
     * GET---Ginseng (Ginseng,GinsengURIGinseng,GinsengURIGinsengHttpGetGinseng)
     *
     * @date 2020year8year17year year4:19:23
     */
    public String doGetWayTwo(List<NameValuePair> params, String url,Map<String,String> header) {
        // parameter
        URI uri = null;
        try {
            int i = url.indexOf(":");
            String scheme = url.substring(0, i);
            String substring = url.substring(i + 3);
            int i1 = substring.indexOf("/");
            String host = substring.substring(0,i1);
            String path = substring.substring(i1);
            uri = new URIBuilder().setScheme(scheme).setHost(host).setPath(path).setParameters(params).build();
        } catch (URISyntaxException e1) {
            log.error("context",e1);
        }
        // establishGetestablish
        HttpGet httpGet = new HttpGet(uri);
        if (null != header && header.size() > 0) {
            for (String key : header.keySet()) {
                httpGet.setHeader(key, header.get(key));
            }
        }
        httpGet.setHeader("Referer", "instdb");
        return doGetResult(httpGet);
    }


    public String doGetWayTwo(String host, String path) {
        // parameter
        URI uri = null;
        try {
            uri =  new URIBuilder().setScheme("http").setHost(host).setPath(path).build();
        } catch (URISyntaxException e1) {
            log.error("context",e1);
        }
        // establishGetestablish
        HttpGet httpGet = new HttpGet(uri);
        return doGetResult(httpGet);
    }

    public String doGetWayTwo(String url,Map<String, String> header) {
        // establishGetestablish
        HttpGet httpGet = new HttpGet(url);
        if (null != header && header.size() > 0) {
            for (String key : header.keySet()) {
                httpGet.setHeader(key, header.get(key));
            }
        }
        return doGetResult(httpGet);
    }

    public String doGetScidb(String url,String api_key) {
        // establishGetestablish
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("api_key",api_key);
        return doGetResult(httpGet);
    }

    public String doGetCstr(String url,String clientId,String secret) {
        // establishGetestablish
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("clientId", clientId);
        httpGet.setHeader("secret", secret);
        return doGetResult(httpGet);
    }

    RequestConfig getRequestConfig() {
        // configuration information
        RequestConfig requestConfig = RequestConfig.custom()
                // Set connection timeout(Set connection timeout)
                .setConnectTimeout(150000)
                // Set request timeout time(Set request timeout time)
                .setConnectionRequestTimeout(150000)
                // socketRead/write timeout(Read/write timeout)
                .setSocketTimeout(150000)
                // Set whether redirection is allowed(Set whether redirection is allowedtrue)
                .setRedirectsEnabled(true).build();
        return requestConfig;
    }


    public String doPostCstr(String url,String param,String clientId,String secret) {

        // obtainHttpobtain(obtain:obtain;obtain:obtainHttpClientobtain)
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String result = "";
        // establishPostestablish
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("clientId", clientId);
        httpPost.setHeader("secret", secret);
        httpPost.setHeader("user-agent", "instdb");
        // Response model
        CloseableHttpResponse response = null;
        try {

            // Add the configuration information above Add the configuration information abovePostAdd the configuration information above
            httpPost.setConfig(getRequestConfig());
            httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
            httpPost.setHeader("Referer", "instdb");
            StringEntity se = new StringEntity(param,"UTF-8");
            se.setContentType(CONTENT_TYPE_TEXT_JSON);
            httpPost.setEntity(se);
            // Executed by client(Executed by client)PostExecuted by client
            response = httpClient.execute(httpPost);
            // Obtain response entities from the response model
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                String s1 = EntityUtils.toString(responseEntity);
                result = s1;
            }
        } catch (ClientProtocolException e) {
            log.error("context",e);
        } catch (ParseException e) {
            log.error("context",e);
        } catch (IOException e) {
            log.error("context",e);
        } finally {
            try {
                // Release resources
                if (httpClient != null) {
                    httpClient.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error("context",e);
            }
        }
        return result;




    }
//
//    /**
//     * checkcstrcheck
//     * @param prefix
//     * @param clientid
//     * @param secret
//     * @return
//     */
//    public int checkCstr(String prefix, String clientid, String secret) {
//        int code = 500;
//        // establishGetestablish
//        HttpGet httpGet = new HttpGet();
//        httpGet.setHeader("clientid", clientid);
//        httpGet.setHeader("secret", secret);
//        String result = "";
//        try {
//            result = doGetResult(httpGet);
//            Map resultMap = JSONObject.parseObject(result, Map.class);
//            code = (int) resultMap.get("code");
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error("cstrAbnormal verification result returned，Abnormal verification result returned");
//            return code;
//        }
//        return code;
//    }




    private String doGetResult(HttpGet httpGet){
        // obtainHttpobtain(obtain:obtain;obtain:obtainHttpClientobtain)
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String result = "";
        // Response model
        CloseableHttpResponse response = null;
        try {
            // Add the configuration information above Add the configuration information aboveGetAdd the configuration information above
            httpGet.setConfig(getRequestConfig());

            // Executed by client(Executed by client)GetExecuted by client
            response = httpClient.execute(httpGet);

            // Obtain response entities from the response model
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                result = EntityUtils.toString(responseEntity);
            }
        } catch (Exception e) {
            log.error("context",e.getMessage());
        } finally {
            try {
                // Release resources
                if (httpClient != null) {
                    httpClient.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (Exception e) {
                log.error("context",e.getMessage());
            }
        }
        return result;
    }


    /**
     *  post Form Value Transfer
     * @param params
     * @return
     */
    public String doPostJsonWayTwo(List<NameValuePair> params, String url) {
        // obtainHttpobtain(obtain:obtain;obtain:obtainHttpClientobtain)
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String result = "";
        // establishPostestablish
        HttpPost httpPost = new HttpPost(url);
        // Response model
        CloseableHttpResponse response = null;
        try {
            // Add the configuration information above Add the configuration information abovePostAdd the configuration information above
            httpPost.setConfig(getRequestConfig());
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            httpPost.setHeader("Referer", "instdb");
            httpPost.setEntity(new UrlEncodedFormEntity(params,"utf-8"));
            // Executed by client(Executed by client)PostExecuted by client
            response = httpClient.execute(httpPost);
            // Obtain response entities from the response model
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                String s1 = EntityUtils.toString(responseEntity);
                result = s1;
            }
        } catch (ClientProtocolException e) {
            log.error("context",e);
        } catch (ParseException e) {
            log.error("context",e);
        } catch (IOException e) {
            log.error("context",e);
        } finally {
            try {
                // Release resources
                if (httpClient != null) {
                    httpClient.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error("context",e);
            }
        }
        return result;
    }


    /**
     *  post request  requestJSON
     * @param param
     * @return
     */
    public String doPostJsonWayTwo(String param,String url) {
        // obtainHttpobtain(obtain:obtain;obtain:obtainHttpClientobtain)
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String result = "";
        // establishPostestablish
        HttpPost httpPost = new HttpPost(url);
        // Response model
        CloseableHttpResponse response = null;
        try {
            // Add the configuration information above Add the configuration information abovePostAdd the configuration information above
            httpPost.setConfig(getRequestConfig());
            httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
            httpPost.setHeader("Referer", "instdb");
            StringEntity se = new StringEntity(param,"UTF-8");
            se.setContentType(CONTENT_TYPE_TEXT_JSON);
            httpPost.setEntity(se);
            // Executed by client(Executed by client)PostExecuted by client
            response = httpClient.execute(httpPost);
            // Obtain response entities from the response model
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                String s1 = EntityUtils.toString(responseEntity);
                result = s1;
            }
        } catch (ClientProtocolException e) {
            log.error("context",e);
        } catch (ParseException e) {
            log.error("context",e);
        } catch (IOException e) {
            log.error("context",e);
        } finally {
            try {
                // Release resources
                if (httpClient != null) {
                    httpClient.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error("context",e);
            }
        }
        return result;
    }

    /**
     * doPostrequest
     * Only fordoiOnly for Only for doiOnly for
     *
     * @author wangzhilaing
     */
    public String doPostToDoi(String url, String requestString, String doiPassword) throws Exception {
        CloseableHttpResponse httpResponse = null;
        String result = "";
        // establishhttpClientestablish
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        // establishhttpPostestablish
        HttpPost httpPost = new HttpPost(url);
        // Configure request parameter instances
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000)// Set the timeout time for connecting to the host service
                .setConnectionRequestTimeout(35000)// Set connection request timeout time
                .setSocketTimeout(60000)// Set the timeout time for reading data connection
                .build();
        // byhttpPostby
        httpPost.setConfig(requestConfig);
        // Set request header
        httpPost.addHeader("Content-Type", "application/vnd.api+json");
        httpPost.addHeader("Authorization", "Basic " + CommonUtils.baseEncoding(doiPassword));
        httpPost.setEntity(new StringEntity(requestString, "UTF-8"));

        try {
            // httpClientObject ExecutionpostObject Execution,Object Execution
            httpResponse = httpClient.execute(httpPost);
            // Obtain response content from the response object
            HttpEntity entity = httpResponse.getEntity();
            if (null != entity) {
                return EntityUtils.toString(entity);
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // close resource
            if (null != httpResponse) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }



    /**
     *  Technology Cloud Request
     * @param url
     *            Sending a request URL
     * @param param
     *            Request parameters，Request parameters name1=value1&name2=value2 Request parameters。
     * @return The response result of the remote resource represented
     */
    public  String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // Open andURLOpen and
            URLConnection conn = realUrl.openConnection();
            // Set universal request properties
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // sendPOSTsend
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // obtainURLConnectionobtain
            out = new PrintWriter(conn.getOutputStream());
            // Send request parameters
            out.print(param);
            // flushBuffering of output streams
            out.flush();
            // definitionBufferedReaderdefinitionURLdefinition
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("send POST send！"+e);
            log.error("context",e);
        }
        //applyfinallyapply、apply
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                log.error("context",ex);
            }
        }
        return result;
    }


    public String upload(HttpEntity httpEntity,String url) {
        String result = "";
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpPost httppost = new HttpPost(url);
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(20000).setSocketTimeout(20000).build();
            httppost.setConfig(requestConfig);
            //setparameter
            httppost.setEntity(httpEntity);
            //implementpost implement
            CloseableHttpResponse response = httpclient.execute(httppost);
            try {
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    result = EntityUtils.toString(response.getEntity());
                }
                EntityUtils.consume(resEntity);
            } finally {
                response.close();
            }
        } catch (ClientProtocolException e) {
            log.error("context",e);
        } catch (IOException e) {
            log.error("context",e);
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                log.error("context",e);
            }
        }
        return result;
    }

}
