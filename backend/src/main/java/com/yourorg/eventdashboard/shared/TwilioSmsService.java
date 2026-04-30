package com.yourorg.eventdashboard.shared;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Production SMS implementation backed by the Twilio REST API.
 * Active only when the {@code twilio} Spring profile is active.
 */
@Service
@Profile("twilio")
@Slf4j
public class TwilioSmsService implements SmsService {

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    // Set in @PostConstruct for production; injected directly by the package-private test constructor.
    private TwilioMessageSender messageSender;

    // Production constructor — Spring uses this with @Value injection.
    public TwilioSmsService(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.phone-number}") String fromNumber) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
    }

    // Test constructor — bypasses @PostConstruct and Twilio SDK initialisation.
    // Inject a TwilioMessageSender lambda to control Twilio behaviour in unit tests
    // without requiring Mockito.mockStatic on the Twilio SDK static methods.
    TwilioSmsService(String accountSid, String authToken, String fromNumber,
                     TwilioMessageSender messageSender) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.messageSender = messageSender;
    }

    @PostConstruct
    void init() {
        Twilio.init(accountSid, authToken);
        this.messageSender = (to, from, body) -> {
            Message msg = Message.creator(new PhoneNumber(to), new PhoneNumber(from), body).create();
            if (msg == null) {
                throw new SmsDeliveryException("Twilio create() returned null");
            }
            return msg.getSid();
        };
    }

    @Override
    public String send(String toPhoneNumber, String messageBody) {
        log.info("Sending SMS to subscription [masked] for message length {}", messageBody.length());
        try {
            String sid = messageSender.send(toPhoneNumber, fromNumber, messageBody);
            log.info("SMS sent, sid={}", sid);
            return sid;
        } catch (SmsDeliveryException e) {
            log.warn("SMS delivery failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("SMS delivery failed: {}", e.getMessage());
            throw new SmsDeliveryException("SMS delivery failed: " + e.getMessage(), e);
        }
    }
}
