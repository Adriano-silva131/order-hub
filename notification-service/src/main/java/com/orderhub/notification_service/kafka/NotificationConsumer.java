package com.orderhub.notification_service.kafka;

import com.orderhub.notification_service.event.OrderCreatedEvent;
import com.orderhub.notification_service.event.PaymentResultEvent;
import com.orderhub.notification_service.service.EmailSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Locale;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private final EmailSenderService emailService;

    @Value("${notification.demo-recipient-email:demo@example.com}")
    private String demoRecipientEmail = "demo@example.com";

    public NotificationConsumer(EmailSenderService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "order-events", groupId = "notification-group", containerFactory = "orderEventListenerContainerFactory")
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Evento de PEDIDO CRIADO recebido para notificação. Pedido: {}", event.orderId());

        try {
            sendOrderCreatedEmail(event);
        } catch (Exception e) {
            log.error("Falha ao processar notificação de pedido criado {}", event.orderId(), e);
        }
    }

    private void sendOrderCreatedEmail(OrderCreatedEvent event) {
        String customerEmail = resolveRecipient(event.customerEmail());
        String subject = "Seu pedido foi recebido!";
        String valorFormatado = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                .format(event.totalAmount());

        String body = """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4;padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                          <tr>
                            <td style="background-color:#1a73e8;padding:32px 40px;text-align:center;">
                              <h1 style="color:#ffffff;margin:0;font-size:24px;letter-spacing:1px;">OrderHub</h1>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:40px;">
                              <h2 style="color:#333333;margin:0 0 16px;">Pedido recebido com sucesso!</h2>
                              <p style="color:#555555;font-size:16px;line-height:1.6;margin:0 0 24px;">
                                Olá! Recebemos o seu pedido e ele já está sendo processado.
                              </p>
                              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f8f9fa;border-radius:6px;margin-bottom:24px;">
                                <tr>
                                  <td style="padding:20px;">
                                    <p style="margin:0 0 8px;color:#888888;font-size:13px;text-transform:uppercase;letter-spacing:1px;">Número do pedido</p>
                                    <p style="margin:0;color:#333333;font-size:15px;font-weight:bold;">%s</p>
                                  </td>
                                </tr>
                                <tr>
                                  <td style="padding:0 20px 20px;">
                                    <p style="margin:0 0 8px;color:#888888;font-size:13px;text-transform:uppercase;letter-spacing:1px;">Valor total</p>
                                    <p style="margin:0;color:#1a73e8;font-size:22px;font-weight:bold;">%s</p>
                                  </td>
                                </tr>
                              </table>
                              <p style="color:#555555;font-size:15px;line-height:1.6;margin:0 0 32px;">
                                Estamos aguardando a confirmação do pagamento para dar andamento ao envio. Você receberá uma notificação assim que o pagamento for processado.
                              </p>
                              <p style="color:#999999;font-size:13px;margin:0;border-top:1px solid #eeeeee;padding-top:24px;">
                                Este é um e-mail automático. Por favor, não responda.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(event.orderId(), valorFormatado);

        emailService.sendEmail(customerEmail, subject, body);
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-group", containerFactory = "paymentEventListenerContainerFactory")
    public void consumePaymentEvent(PaymentResultEvent event) {
        log.info("Evento de PAGAMENTO recebido para notificação. Pedido: {}", event.orderId());

        try {
            sendPaymentResultEmail(event);
        } catch (Exception e) {
            log.error("Falha ao processar notificação de pagamento {}", event.orderId(), e);
        }
    }

    private void sendPaymentResultEmail(PaymentResultEvent event) {
        String customerEmail = resolveRecipient(event.customerEmail());

        String subject;
        String statusBlock;

        if ("APPROVED".equals(event.status())) {
            subject = "Pagamento aprovado!";
            statusBlock = """
                    <td style="padding:20px;text-align:center;">
                      <div style="display:inline-block;background-color:#e6f4ea;border-radius:50%%;width:64px;height:64px;line-height:64px;font-size:32px;margin-bottom:16px;">✓</div>
                      <h2 style="color:#1e8e3e;margin:0 0 12px;">Pagamento aprovado!</h2>
                      <p style="color:#555555;font-size:15px;line-height:1.6;margin:0;">
                        O pagamento do seu pedido <strong>%s</strong> foi confirmado.<br>
                        Ele já está sendo separado no nosso estoque e em breve será enviado.
                      </p>
                    </td>
                    """.formatted(event.orderId());
        } else {
            subject = "Problema no pagamento do seu pedido";
            statusBlock = """
                    <td style="padding:20px;text-align:center;">
                      <div style="display:inline-block;background-color:#fce8e6;border-radius:50%%;width:64px;height:64px;line-height:64px;font-size:32px;margin-bottom:16px;">✕</div>
                      <h2 style="color:#d93025;margin:0 0 12px;">Pagamento não autorizado</h2>
                      <p style="color:#555555;font-size:15px;line-height:1.6;margin:0;">
                        Infelizmente o pagamento do pedido <strong>%s</strong> não foi autorizado pela operadora.<br>
                        Por favor, acesse o site e tente novamente com outro cartão.
                      </p>
                    </td>
                    """.formatted(event.orderId());
        }

        String body = """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4;padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                          <tr>
                            <td style="background-color:#1a73e8;padding:32px 40px;text-align:center;">
                              <h1 style="color:#ffffff;margin:0;font-size:24px;letter-spacing:1px;">OrderHub</h1>
                            </td>
                          </tr>
                          <tr>
                            %s
                          </tr>
                          <tr>
                            <td style="padding:0 40px 40px;">
                              <p style="color:#999999;font-size:13px;margin:0;border-top:1px solid #eeeeee;padding-top:24px;">
                                Este é um e-mail automático. Por favor, não responda.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(statusBlock);

        emailService.sendEmail(customerEmail, subject, body);
    }

    private String resolveRecipient(String customerEmail) {
        return (customerEmail != null && !customerEmail.isBlank()) ? customerEmail : demoRecipientEmail;
    }
}
