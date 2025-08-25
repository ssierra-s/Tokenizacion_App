package com.challenge.tokenizacion_app.service;

import com.challenge.tokenizacion_app.model.entity.Order;
import com.challenge.tokenizacion_app.model.entity.OrderItem;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from:no-reply@tu-dominio.com}")
    private String from;

    @Value("${mail.enabled:true}")
    private boolean enabled;

    private static final Locale LOCALE_CO = new Locale("es", "CO"); // ajusta a tu preferencia

    public void sendOrderApproved(String to, Order order) {
        if (!enabled) { log.debug("Email deshabilitado (ORDER APPROVED)"); return; }
        String subject = "✅ Pedido #" + order.getId() + " APROBADO";
        String html = buildApprovedHtml(order);
        sendHtml(to, subject, html, null);
    }

    public void sendOrderRejected(String to, Order order) {
        if (!enabled) { log.debug("Email deshabilitado (ORDER REJECTED)"); return; }
        String subject = "❌ Pedido #" + order.getId() + " RECHAZADO";
        String html = buildRejectedHtml(order);
        sendHtml(to, subject, html, null);
    }

    private void sendHtml(String to, String subject, String html, @Nullable String plainFallback) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(plainFallback != null ? plainFallback : stripTags(html), html);
            mailSender.send(msg);
            log.info("Correo enviado a {}: {}", to, subject);
        } catch (Exception e) {
            // No queremos que una falla de correo rompa el flujo de la orden
            log.warn("No se pudo enviar correo a {}: {}", to, e.getMessage());
        }
    }

    private String buildApprovedHtml(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>¡Gracias por tu compra!</h2>");
        sb.append("<p><b>Pedido #").append(order.getId()).append("</b> aprobado.</p>");
        if (order.getDeliveryAddress() != null) {
            sb.append("<p>Dirección de entrega: ").append(escape(order.getDeliveryAddress())).append("</p>");
        }
        sb.append("<table border='1' cellpadding='6' cellspacing='0'>")
                .append("<thead><tr><th>Producto</th><th>Cantidad</th><th>Unitario</th><th>Subtotal</th></tr></thead><tbody>");
        for (OrderItem it : order.getItems()) {
            String name = it.getProduct() != null ? it.getProduct().getName() : "Producto";
            sb.append("<tr>")
                    .append("<td>").append(escape(name)).append("</td>")
                    .append("<td>").append(it.getQuantity()).append("</td>")
                    .append("<td>").append(fmt(it.getUnitPrice())).append("</td>")
                    .append("<td>").append(fmt(it.getSubtotal())).append("</td>")
                    .append("</tr>");
        }
        sb.append("</tbody></table>");
        sb.append("<p><b>Total: ").append(fmt(order.getTotal())).append("</b></p>");
        return sb.toString();
    }

    private String buildRejectedHtml(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Tu pedido no pudo ser aprobado</h2>");
        sb.append("<p><b>Pedido #").append(order.getId()).append("</b> fue rechazado tras ")
                .append(order.getAttempts()).append(" intentos.</p>");
        sb.append("<p>Verifica los datos de tu tarjeta o intenta con otro medio de pago.</p>");
        return sb.toString();
    }

    private String fmt(BigDecimal v) {
        if (v == null) return "-";
        return NumberFormat.getCurrencyInstance(LOCALE_CO).format(v);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    private static String stripTags(String html) {
        return html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }
}
