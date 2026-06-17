package com.example.campus.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private final String accessKeyId;
    private final String accessKeySecret;
    private final String signName;
    private final String templateCode;
    private final String endpoint;

    public SmsService(
            @Value("${sms.aliyun.access-key-id:}") String accessKeyId,
            @Value("${sms.aliyun.access-key-secret:}") String accessKeySecret,
            @Value("${sms.aliyun.sign-name:}") String signName,
            @Value("${sms.aliyun.template-code:}") String templateCode,
            @Value("${sms.aliyun.endpoint:dysmsapi.aliyuncs.com}") String endpoint) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.signName = signName;
        this.templateCode = templateCode;
        this.endpoint = endpoint;
    }

    public boolean isConfigured() {
        return !accessKeyId.isBlank()
                && !accessKeySecret.isBlank()
                && !signName.isBlank()
                && !templateCode.isBlank();
    }

    public void sendResetPasswordCode(String phone, String code) throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret);
        config.endpoint = endpoint;

        Client client = new Client(config);
        SendSmsRequest request = new SendSmsRequest()
                .setPhoneNumbers(phone)
                .setSignName(signName)
                .setTemplateCode(templateCode)
                .setTemplateParam("{\"code\":\"" + code + "\"}");

        SendSmsResponse response = client.sendSms(request);
        String responseCode = response.getBody().getCode();
        if (!"OK".equals(responseCode)) {
            throw new IllegalStateException(response.getBody().getMessage());
        }
    }
}
