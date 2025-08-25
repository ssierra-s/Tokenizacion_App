package com.challenge.tokenizacion_app.listeners;

import com.challenge.tokenizacion_app.events.OrderFinalizedEvent;
import com.challenge.tokenizacion_app.model.entity.Order;
import com.challenge.tokenizacion_app.repository.OrderRepository;
import com.challenge.tokenizacion_app.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEmailListener {

    private final OrderRepository orderRepository;
    private final MailService mailService;

    @Value("${payment.max-attempts:3}")
    private int maxAttempts;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderFinalized(OrderFinalizedEvent ev) {
        try {
            if ("APPROVED".equalsIgnoreCase(ev.status())) {
                // Para aprobado incluimos detalle (items y totales)
                Optional<Order> opt = orderRepository.findByIdWithItems(ev.orderId());
                opt.ifPresentOrElse(
                        order -> mailService.sendOrderApproved(ev.userEmail(), order),
                        ()   -> log.warn("No se encontró la orden {} para email aprobado", ev.orderId())
                );
            } else if ("REJECTED".equalsIgnoreCase(ev.status()) && ev.attempts() >= maxAttempts) {
                // Para rechazo final no necesitamos items (pero podemos cargarlos si quieres)
                Optional<Order> opt = orderRepository.findByIdWithItems(ev.orderId());
                mailService.sendOrderRejected(ev.userEmail(), opt.orElse(null));
            } else {
                log.debug("Orden {} en estado {} (attempts={}) — sin correo",
                        ev.orderId(), ev.status(), ev.attempts());
            }
        } catch (Exception e) {
            // No romper servicios por fallo de correo
            log.warn("Fallo enviando correo para orden {}: {}", ev.orderId(), e.getMessage());
        }
    }
}
