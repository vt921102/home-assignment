package com.toanlv.flashsale.auth.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.toanlv.flashsale.common.outbox.domain.OutboxEvent;
import com.toanlv.flashsale.common.outbox.handler.OutboxEventHandler;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class OtpDispatchHandler implements OutboxEventHandler {

  private static final Logger log = LoggerFactory.getLogger(OtpDispatchHandler.class);

  private final MeterRegistry meterRegistry;

  public OtpDispatchHandler(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public String supportedType() {
    return "OTP_DISPATCH";
  }

  /**
   * Mock OTP delivery — logs to console in lieu of real SMS/email.
   *
   * <p>Production replacement: Inject SmsGateway or EmailGateway and call the real API here.
   * OutboxDispatchWorker handles retries — no retry logic needed here.
   */
  @Override
  public void handle(OutboxEvent event) {
    var p = event.getPayload();
    var channel = (String) p.get("channel");
    var identifier = (String) p.get("identifier");
    var otp = (String) p.get("otp");
    var purpose = (String) p.get("purpose");
    var expiresAt = (String) p.get("expiresAt");

    log.info(
        """
                ╔══════════════════════════════════════════╗
                ║           [MOCK OTP DISPATCH]            ║
                ╠══════════════════════════════════════════╣
                ║ channel    : {}
                ║ recipient  : {}
                ║ purpose    : {}
                ║ otp_code   : {}
                ║ expires_at : {}
                ║ event_id   : {}
                ╚══════════════════════════════════════════╝
                """,
        channel,
        mask(identifier),
        purpose,
        otp,
        expiresAt,
        event.getId());

    meterRegistry.counter("otp.dispatched", "channel", channel, "purpose", purpose).increment();
  }

  /**
   * Partially mask identifier for safe logging. user@example.com → u***@example.com 0912345678 →
   * 091***678
   */
  private String mask(String identifier) {
    if (identifier == null) return "***";
    if (identifier.contains("@")) {
      int at = identifier.indexOf('@');
      if (at <= 1) return "***" + identifier.substring(at);
      return identifier.charAt(0) + "***" + identifier.substring(at);
    }
    if (identifier.length() > 6) {
      return identifier.substring(0, 3) + "***" + identifier.substring(identifier.length() - 3);
    }
    return "***";
  }
}
