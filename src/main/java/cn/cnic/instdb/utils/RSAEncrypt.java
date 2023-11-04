package cn.cnic.instdb.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@Slf4j
public class RSAEncrypt {

    //Public key
    private static String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCASph/ydFbA8T4uieDZ" +
            "eTnqFivJu8QH7fZfbo3w2ULeZ/JJ5+3RSe8i+kvoZcn8Vdy3RIXi9xEy/OCKOCk6CvnaHEtLgA20DKBWbhD" +
            "7SIn5UoNJP2E6S/ytSw7BCesENVSl+9iIrze9nuKeZ0982R0itUCTwjBB7Yw1mg+1QanGQIDAQAB";

    //Private key
    private static String privateKey = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAIBKmH/J0VsDxPi6J4Nl5OeoWK8" +
            "m7xAft9l9ujfDZQt5n8knn7dFJ7yL6S+hlyfxV3LdEheL3ETL84Io4KToK+docS0uADbQMoFZuEPtIiflSg0k/Y" +
            "TpL/K1LDsEJ6wQ1VKX72IivN72e4p5nT3zZHSK1QJPCMEHtjDWaD7VBqcZAgMBAAECgYAgZzb3Z9kqHNyeWh5q0My" +
            "tOlcT/kh5kRlVpKzpMsAN0u1p2Ek9+AieVdRTBIRyQUHuMCpGqju7YKwjnwGGhWG5FzGAEJ/DgrEaQD/pH39FKX2uMw" +
            "6Gc3FFX06ora8qmNNuA8Br4GQvA/aQxO4jwVMHL3OaSFgmtdrtKRftamZNgQJBANf538RBs3WQRkeVlrKYwunpvKfN" +
            "DH2+peWmPQZAMIiin+44NcAlPTQ8CAi36Ift2aMsjvXhf89bI2SenestPnECQQCYEN7dKmnkDOmX4N2FYi3lyfLJq50" +
            "2am+tJKcWBG34Cz95KaOcgnQOWD4zHNgYinB9vP4qy+4nrI4pz1fJo5cpAkBW2jp9Xvp/NZS9ps9iZQJFNOTUCiaSzr9C" +
            "Ofbic5/Q4q00DFC5Q4B4aAfHEcYmG6Vg9ENNZ/CQ/5KdHRhegeRRAkBzIZrK0nj3u7sETbEKcuoTJ5JVlERkVbOV4MFM" +
            "jy//c+yrvJXuQmrCDZeSNU17Tx2aZYP+PQZkLWY5S43I0b35AkBn9k8TrEmB8TVjBrVBY0OyafgPoudIWhvmH91SEVyyF" +
            "88fgLi4nIgI3m3v1SEa43b8/nJwE3L2f9IAPbiL+h9w";


    /**
     * Randomly generate key pairs
     * @throws NoSuchAlgorithmException
     */
    public static void genKeyPair() throws NoSuchAlgorithmException {
        // KeyPairGeneratorClass used to generate public and private key pairs，Class used to generate public and private key pairsRSAClass used to generate public and private key pairs
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        // Initialize key pair generator，Initialize key pair generator96-1024Initialize key pair generator
        keyPairGen.initialize(2048 ,new SecureRandom());
        // Generate a key pair，Generate a key pairkeyPairGenerate a key pair
        KeyPair keyPair = keyPairGen.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();   // Obtain private key
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();  // Obtain public key
        String publicKeyString = new String(Base64.encodeBase64(publicKey.getEncoded()));
        // Get private key string
        String privateKeyString = new String(Base64.encodeBase64((privateKey.getEncoded())));
        // Save public and private keys toMap
    }

    /**
     * RSAPublic key encryption
     * @param str Encrypted String
     * @return ciphertext
     */
    public static String encrypt( String str) {
        try {
            //base64Encoded public key
            byte[] decoded = Base64.decodeBase64(publicKey);
            RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
            //RSAencryption
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            String outStr = Base64.encodeBase64String(cipher.doFinal(str.getBytes("UTF-8")));
            return outStr;
        }catch (Exception e){
            log.error("context",e);
            return null;
        }
    }

    /**
     * RSAPrivate key decryption
     * @param str Encrypted String
     * @return Inscription
     */
    public static String decrypt(String str){
        try {
            //64Bit decoded encrypted string
            byte[] inputByte = Base64.decodeBase64(str.getBytes("UTF-8"));
            //base64Encoded private key
            byte[] decoded = Base64.decodeBase64(privateKey);
            RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
            //RSADecryption
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            return new String(cipher.doFinal(inputByte));
        }catch (Exception e){
            log.error("context",e);
            return null;
        }
    }

}

