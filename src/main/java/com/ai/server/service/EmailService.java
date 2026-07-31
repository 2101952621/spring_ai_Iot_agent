package com.ai.server.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮件服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * 发送注册验证邮件
     */
    public void sendRegisterMail(String to, String activateToken) {
        String subject = "【Agent AI】请激活您的账号";
        String activateUrl = baseUrl + "/api/noauth/activate?activateToken=" + activateToken;
        String content = buildRegisterEmailContent(activateUrl);
        sendHtmlMail(to, subject, content);
    }

    /**
     * 发送密码重置邮件
     */
    public void sendResetPasswordMail(String to, String resetToken) {
        String subject = "【Agent AI】密码重置请求";
        String resetUrl = baseUrl + "/api/noauth/resetPassword?resetToken=" + resetToken;
        String content = buildResetPasswordEmailContent(resetUrl);
        sendHtmlMail(to, subject, content);
    }

    private void sendHtmlMail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
            log.info("邮件发送成功, to={}, subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("邮件发送失败, to={}, error={}", to, e.getMessage());
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    private String buildRegisterEmailContent(String activateUrl) {
        return """
                <div style="max-width:600px;margin:0 auto;padding:20px;font-family:Arial,sans-serif;">
                    <h2 style="color:#333;">欢迎注册 Agent AI 平台</h2>
                    <p>感谢您注册 Agent AI 平台账号！</p>
                    <p>请点击下方按钮激活您的账号：</p>
                    <div style="text-align:center;margin:30px 0;">
                        <a href="%s" style="background:#4CAF50;color:white;padding:12px 30px;text-decoration:none;border-radius:4px;font-size:16px;">激活账号</a>
                    </div>
                    <p>如果按钮无法点击，请复制以下链接到浏览器打开：</p>
                    <p style="color:#666;">%s</p>
                    <hr style="border:1px solid #eee;margin:30px 0;">
                    <p style="color:#999;font-size:12px;">如果您没有注册此账号，请忽略此邮件。</p>
                </div>
                """.formatted(activateUrl, activateUrl);
    }

    private String buildResetPasswordEmailContent(String resetUrl) {
        return """
                <div style="max-width:600px;margin:0 auto;padding:20px;font-family:Arial,sans-serif;">
                    <h2 style="color:#333;">密码重置请求</h2>
                    <p>您正在请求重置 Agent AI 平台账号的密码。</p>
                    <p>请点击下方按钮完成密码重置：</p>
                    <div style="text-align:center;margin:30px 0;">
                        <a href="%s" style="background:#2196F3;color:white;padding:12px 30px;text-decoration:none;border-radius:4px;font-size:16px;">重置密码</a>
                    </div>
                    <p>如果按钮无法点击，请复制以下链接到浏览器打开：</p>
                    <p style="color:#666;">%s</p>
                    <hr style="border:1px solid #eee;margin:30px 0;">
                    <p style="color:#999;font-size:12px;">此链接24小时内有效。如果您没有请求重置密码，请忽略此邮件。</p>
                </div>
                """.formatted(resetUrl, resetUrl);
    }
}
