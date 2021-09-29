package Servidores;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class GeneratePassWord {

    public static byte[] generateHMAC(String sharedKey) {
        try {
            Mac hasher = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(sharedKey.getBytes("UTF-8"), "HmacSHA256");
            hasher.init(key);

            return hasher.doFinal();
        }
        catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isAuthenticated(String sharedKey, byte[] hmac) {
        try {
            Mac hasher = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(sharedKey.getBytes("UTF-8"), "HmacSHA256");
            hasher.init(key);

            byte[] result = hasher.doFinal();

            return Arrays.equals(hmac, result);
        }
        catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

}
