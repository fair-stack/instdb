package cn.cnic.instdb.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public   class XSSUtils {
    public static String stripXSS(String value) {
        if (value != null) {
            String script = "<script[^>]*?>[\\s\\S]*?<\\/script>"; //definitionscriptdefinition
            String style = "<style[^>]*?>[\\s\\S]*?<\\/style>"; //definitionstyledefinition
            String html = "<[^>]+>"; //definitionHTMLdefinition
            //Filter special characters
            String regEx="[`~!@#$%^*()+=|';'\\[\\]<>/?~！@#￥%……*（）——+|【】‘；：”“’。，、？]";
            Pattern p_script = Pattern.compile(script, Pattern.CASE_INSENSITIVE);
            Matcher m_script = p_script.matcher(value);
            value = m_script.replaceAll(""); //filterscriptfilter
            Pattern p_style = Pattern.compile(style, Pattern.CASE_INSENSITIVE);
            Matcher m_style = p_style.matcher(value);
            value = m_style.replaceAll(""); //filterstylefilter
            Pattern p_html = Pattern.compile(html, Pattern.CASE_INSENSITIVE);
            Matcher m_html = p_html.matcher(value);
            value = m_html.replaceAll(""); //filterhtmlfilter
            Pattern p = Pattern.compile(regEx);
            Matcher m = p.matcher(value);
            value = m.replaceAll("");
            //----- ---------------
            value = value.replaceAll("", "");
            Pattern scriptPattern = Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("</script>", Pattern.CASE_INSENSITIVE);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("e­xpression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile(".*<.*", Pattern.CASE_INSENSITIVE );
            value = scriptPattern.matcher(value).replaceAll("");
        }
        return value;
    }
}
