package cn.cnic.instdb.config;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;



@Configuration
public class CaptchaConfig {


    @Bean(name = "captchaProducerMath")
    public DefaultKaptcha getKaptchaBeanMath() {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        // Is there a border Is there a bordertrue Is there a borderyes，no
        properties.setProperty("kaptcha.border", "no");
        // Border Color Border ColorColor.BLACK
        properties.setProperty("kaptcha.border.color", "105,179,90");
        // Verification code text character color Verification code text character colorColor.BLACK
        properties.setProperty("kaptcha.textproducer.font.color", "blue");
        // Verification code image width Verification code image width200
        properties.setProperty("kaptcha.image.width", "160");
        // Verification code image height Verification code image height50
        properties.setProperty("kaptcha.image.height", "60");
        // Verification code text character size Verification code text character size40
        properties.setProperty("kaptcha.textproducer.font.size", "40");
        // KAPTCHA_SESSION_KEY
        properties.setProperty("kaptcha.session.key", "kaptchaCodeMath");
        // Verification code text generator
        properties.setProperty("kaptcha.textproducer.impl", "com.google.code.kaptcha.text.impl.DefaultTextCreator");
        // Verification code text character spacing Verification code text character spacing2
        properties.setProperty("kaptcha.textproducer.char.space", "6");
        // Verification code text character length Verification code text character length5
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        // Verification code text font style Verification code text font stylenew Font("Arial", 1, fontSize), new Font("Courier", 1,
        // fontSize)
        properties.setProperty("kaptcha.textproducer.font.names", "Arial,Courier");
        // Verification code noise color Verification code noise colorColor.BLACK
        properties.setProperty("kaptcha.noise.color", "white");
        // Interference implementation class
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.NoNoise");
        // Picture Style Picture Stylecom.google.code.kaptcha.impl.WaterRipple
        // fisheyecom.google.code.kaptcha.impl.FishEyeGimpy
        // shadowcom.google.code.kaptcha.impl.ShadowGimpy
        properties.setProperty("kaptcha.obscurificator.impl", "com.google.code.kaptcha.impl.WaterRipple");
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }



}
