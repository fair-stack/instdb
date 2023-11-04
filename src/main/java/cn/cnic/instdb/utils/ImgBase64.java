package cn.cnic.instdb.utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import sun.misc.BASE64Decoder;

import java.io.*;

/**
 * @Auther: wdd
 * @Date: 2021/10/14/10:11
 * @Description:
 */

@Slf4j
public class ImgBase64 {

    /**
     * Convert image toBase64Convert image to
     * @param imgFile Pending images
     * @return
     */
    public static String getImgStr(String imgFile) {
        // Convert image files into byte array strings，Convert image files into byte array stringsBase64Convert image files into byte array strings

        InputStream in = null;
        byte[] data = null;
        // Read image byte array
        try {
            in = new FileInputStream(imgFile);
            data = new byte[in.available()];
            int count = 0;
            while((count = in.read(data)) != -1) {
                in.read(data);
            }
        } catch (IOException e) {
            log.error("context",e);
        }finally {
            try {
                if(null != in){
                    in.close();
                }
            } catch (IOException e) {
                log.error("context",e);
            }
        }
        return Base64.encodeBase64String(data);
    }

    public static void main(String[] args) {
        String imgStr = getImgStr("C:\\Users\\86176\\Desktop\\0efc17e57b7f4bb2ac8fa3155142a33f_00b151fe7f3df62abc2490f586f4a3f.png");
        System.out.println(imgStr);
    }



    /**
     * Apply to byte array stringsBase64Apply to byte array strings
     * @param imgStr Image data
     * @param imgFilePath Save image full path address
     * @return
     */
    public static boolean generateImage(String imgStr, String imgFilePath) {
        OutputStream out = null;
        if (imgStr == null) // Image data is empty
        {
            return false;
        }
        BASE64Decoder decoder = new BASE64Decoder();
        try {
            // Base64decoding
            byte[] b = decoder.decodeBuffer(imgStr);
            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {// Adjusting abnormal data
                    b[i] += 256;
                }
            }
            // generatejpggenerate
            out = new FileOutputStream(imgFilePath);
            out.write(b);
            out.flush();

            return true;
        } catch (Exception e) {
            return false;
        }finally {
            try {
                if(null != out){
                    out.close();
                }
            } catch (IOException e) {
                log.error("context",e);
            }
        }
    }

}
