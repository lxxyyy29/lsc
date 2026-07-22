package com.changping.platform.modules.drone.util;

import com.changping.platform.modules.drone.exception.DroneApiException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * @Author tangxinglin
 * @Description //无人机SM4加密工具类，使用BouncyCastle提供SM4/CBC/PKCS7Padding算法对密码进行加密，
 * 用于向上游平台API认证时的密码保护
 * @Date 2026/04/18 10:00
 */
public final class DroneSm4Utils {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private DroneSm4Utils() {
    }

    /**
     * @Author tangxinglin
     * @Description //使用SM4/CBC/PKCS7Padding算法对明文进行加密，结果以Base64编码返回
     * @Date 2026/04/18 10:00
     * @Param [plainText 待加密明文, secretKey SM4密钥（16字节）, iv 初始向量（16字节）]
     * @return String Base64编码的加密结果
     */
    public static String encrypt(String plainText, String secretKey, String iv) {
        try {
            Cipher cipher = Cipher.getInstance("SM4/CBC/PKCS7Padding", BouncyCastleProvider.PROVIDER_NAME);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "SM4");
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new DroneApiException("加密无人机 API 密码失败");
        }
    }
}
