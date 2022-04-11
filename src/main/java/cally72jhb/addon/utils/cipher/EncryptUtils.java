package cally72jhb.addon.utils.cipher;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class EncryptUtils {
	public static String encrypt(String text, String key, String algorithm) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(Arrays.copyOf(DigestUtils.sha1(key), 16), algorithm));

        return Base64.encodeBase64String(cipher.doFinal(text.getBytes()));
	}

	public static String decrypt(String text, String key, String algorithm) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Arrays.copyOf(DigestUtils.sha1(key), 16), algorithm));

        return new String(cipher.doFinal(Base64.decodeBase64(text)));
	}
}
