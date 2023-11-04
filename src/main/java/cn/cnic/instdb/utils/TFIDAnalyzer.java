package cn.cnic.instdb.utils;

import com.huaban.analysis.jieba.aliyun.nlp.NlpTerm;
import com.qianxinyao.analysis.jieba.keyword.Keyword;
import com.qianxinyao.analysis.jieba.keyword.TFIDFAnalyzerNlp;

import java.util.ArrayList;
import java.util.List;

/**
 *  Stuttering participle call
 */
public class TFIDAnalyzer {

    public static List<String> init(String content){
        //filter
        content = XSSUtils.stripXSS(content);
        List<String> keywordList =  new ArrayList<>();
        TFIDFAnalyzerNlp tfidfAnalyzer=new TFIDFAnalyzerNlp();
        List<NlpTerm> nlpList = tfidfAnalyzer.getNlpList(content);
        List<Keyword> list2=tfidfAnalyzer.analyze(nlpList);
        for (Keyword word : list2) {
            keywordList.add(word.getName());
        }
        return keywordList;
    }

}
