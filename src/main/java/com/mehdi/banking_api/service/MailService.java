package com.mehdi.banking_api.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final Resend resendClient;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Sends an account verification email asynchronously.
     * Fires-and-forgets — failures are logged but do not affect the registration response.
     */
    @Async
    public void sendVerificationEmail(String toEmail, String firstName, String verificationToken) {
        String verificationUrl = frontendUrl + "/verify?token=" + verificationToken;

        CreateEmailOptions email = CreateEmailOptions.builder()
                .from(fromAddress)
                .to(List.of(toEmail))
                .subject("Confirm your Banking account")
                .html(buildEmailHtml(firstName, verificationUrl))
                .build();

        try {
            resendClient.emails().send(email);
            log.info("Verification email sent to {}", toEmail);
        } catch (ResendException e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildEmailHtml(String firstName, String verificationUrl) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
                <body style="margin:0;padding:0;background-color:#f4f6f9;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f9;padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="560" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                          <!-- Header -->
                          <tr>
                            <td style="background-color:#1a1a2e;padding:32px 40px;">
                              <p style="margin:0;color:#ffffff;font-size:22px;font-weight:700;letter-spacing:0.5px;">Banking</p>
                            </td>
                          </tr>
                          <!-- Body -->
                          <tr>
                            <td style="padding:40px 40px 32px;">
                              <p style="margin:0 0 16px;color:#1a1a2e;font-size:24px;font-weight:600;">Welcome, %s!</p>
                              <p style="margin:0 0 24px;color:#555e6d;font-size:15px;line-height:1.6;">
                                Your account has been created. Please confirm your email address to activate it.
                              </p>
                              <table cellpadding="0" cellspacing="0">
                                <tr>
                                  <td style="border-radius:6px;background-color:#1a1a2e;">
                                    <a href="%s" target="_blank"
                                       style="display:inline-block;padding:14px 32px;color:#ffffff;font-size:15px;font-weight:600;text-decoration:none;letter-spacing:0.3px;">
                                      Confirm my account
                                    </a>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:28px 0 0;color:#888f9a;font-size:13px;line-height:1.5;">
                                This link expires in <strong>24 hours</strong>. If you did not create this account, you can safely ignore this email.
                              </p>
                            </td>
                          </tr>
                          <!-- Footer -->
                          <tr>
                            <td style="padding:20px 40px;border-top:1px solid #f0f0f0;">
                              <p style="margin:0;color:#aab0bb;font-size:12px;">© 2025 Banking — This is an automated message, please do not reply.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(firstName, verificationUrl);
    }
}
