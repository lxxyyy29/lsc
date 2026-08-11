package com.changping.platform.modules.auth.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.changping.platform.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * @Author tangxinglin
 * @Description //阿里云短信服务：发送登录验证码短信，配置从环境变量读取（docker/.env，不入库）
 * @Date 2026/08/11 18:00
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final String accessKeyId;
    private final String accessKeySecret;
    private final String signName;
    private final String templateCode;
    private final SecureRandom random = new SecureRandom();

    public SmsService(
            @Value("${sms.access-key-id:}") String accessKeyId,
            @Value("${sms.access-key-secret:}") String accessKeySecret,
            @Value("${sms.sign-name:}") String signName,
            @Value("${sms.template-code:}") String templateCode) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.signName = signName;
        this.templateCode = templateCode;
    }

    /** 是否已配置阿里云短信（配置齐全才可真实发送） */
    public boolean isConfigured() {
        return accessKeyId != null && !accessKeyId.isBlank()
                && accessKeySecret != null && !accessKeySecret.isBlank()
                && signName != null && !signName.isBlank()
                && templateCode != null && !templateCode.isBlank();
    }

    /** 生成 6 位随机数字验证码 */
    public String generateCode() {
        return String.valueOf(100000 + random.nextInt(900000));
    }

    /**
     * 发送验证码短信
     * @param phone 收件手机号
     * @param code 验证码
     * @return 是否发送成功
     */
    public boolean sendCode(String phone, String code) {
        if (!isConfigured()) {
            log.warn("阿里云短信未配置（sms.access-key-id 等缺失），无法发送验证码给 {}", phone);
            return false;
        }
        try {
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret);
            config.endpoint = "dysmsapi.aliyuncs.com";
            Client client = new Client(config);

            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");
            SendSmsResponse response = client.sendSms(request);
            String bizCode = response.getBody() != null ? response.getBody().getCode() : null;
            if ("OK".equals(bizCode)) {
                log.info("验证码短信发送成功: {} -> {} (code={})", signName, phone, code);
                return true;
            }
            String message = response.getBody() != null ? response.getBody().getMessage() : "未知错误";
            log.error("验证码短信发送失败: code={}, message={}", bizCode, message);
            return false;
        } catch (Exception e) {
            log.error("验证码短信发送异常: {}", e.getMessage(), e);
            return false;
        }
    }
}
