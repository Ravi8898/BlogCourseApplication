package org.project.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESUtil {

    private static final String SECRET_KEY = "MY_KEY_AMXBLOGAP";

    public static String decrypt(String encrypted) throws Exception {
        SecretKeySpec key = new SecretKeySpec(
                SECRET_KEY.getBytes(), "AES");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decodedBytes = Base64.getDecoder().decode(encrypted);
        return new String(cipher.doFinal(decodedBytes));
    }
}
