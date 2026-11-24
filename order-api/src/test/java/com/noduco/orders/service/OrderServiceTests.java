package com.noduco.orders.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.noduco.orders.config.OrdersProperties;
import com.noduco.orders.domain.IdempotencyRecord;
import com.noduco.orders.domain.IdempotencyRepository;
import com.noduco.orders.domain.OrderAction;
import com.noduco.orders.domain.OrderDocument;
import com.noduco.orders.domain.OrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

    static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");

    @Mock
    OrderRepository orders;

    @Mock
    IdempotencyRepository idempotency;

    OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orders, idempotency, Clock.fixed(NOW, ZoneOffset.UTC), new OrdersProperties("INR"));
    }

    static OrderDocument order(String id) {
        OrderDocument o = new OrderDocument("Asha", 500, "INR", NOW);
        ReflectionTestUtils.setField(o, "id", id);
        return o;
    }

    @Test
    void noKeyCreatesOrderWithoutTouchingIdempotency() {
        when(orders.save(any(OrderDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateResult result = service.create(null, "Asha", 500);

        assertFalse(result.replayed());
        assertEquals("INR", result.order().getCurrency());
        verifyNoInteractions(idempotency);
    }

    @Test
    void newKeyClaimsThenStoresOrderId() {
        when(idempotency.insert(any(IdempotencyRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orders.save(any(OrderDocument.class))).thenAnswer(inv -> {
            OrderDocument o = inv.getArgument(0);
            ReflectionTestUtils.setField(o, "id", "o1");
            return o;
        });

        CreateResult result = service.create("k1", "Asha", 500);

        assertFalse(result.replayed());
        ArgumentCaptor<IdempotencyRecord> claimed = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(idempotency).insert(claimed.capture());
        assertEquals("Asha|500", claimed.getValue().fingerprint());
        assertNull(claimed.getValue().orderId());
        ArgumentCaptor<IdempotencyRecord> completed = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(idempotency).save(completed.capture());
        assertEquals("o1", completed.getValue().orderId());
    }

    @Test
    void duplicateKeyWithFinishedRecordReplays() {
        OrderDocument existing = order("o1");
        when(idempotency.insert(any(IdempotencyRecord.class))).thenThrow(new DuplicateKeyException("E11000"));
        when(idempotency.findById("k1")).thenReturn(Optional.of(new IdempotencyRecord("k1", "Asha|500", "o1", NOW)));
        when(orders.findById("o1")).thenReturn(Optional.of(existing));

        CreateResult result = service.create("k1", "Asha", 500);

        assertTrue(result.replayed());
        assertSame(existing, result.order());
        verify(orders, never()).save(any());
    }

    @Test
    void duplicateKeyWhileFirstRequestRunsIsInProgress() {
        when(idempotency.insert(any(IdempotencyRecord.class))).thenThrow(new DuplicateKeyException("E11000"));
        when(idempotency.findById("k1")).thenReturn(Optional.of(new IdempotencyRecord("k1", "Asha|500", null, NOW)));

        assertThrows(IdempotencyInProgressException.class, () -> service.create("k1", "Asha", 500));
        verify(orders, never()).save(any());
    }

    @Test
    void abandonedClaimIsTakenOver() {
        Instant old = NOW.minusSeconds(60);
        when(idempotency.insert(any(IdempotencyRecord.class)))
                .thenThrow(new DuplicateKeyException("E11000"))
                .thenAnswer(inv -> inv.getArgument(0));
        when(idempotency.findById("k1")).thenReturn(Optional.of(new IdempotencyRecord("k1", "Asha|500", null, old)));
        when(idempotency.deleteByKeyAndCreatedAtAndOrderIdIsNull("k1", old)).thenReturn(1L);
        when(orders.save(any(OrderDocument.class))).thenAnswer(inv -> {
            OrderDocument o = inv.getArgument(0);
            ReflectionTestUtils.setField(o, "id", "o2");
            return o;
        });

        CreateResult result = service.create("k1", "Asha", 500);

        assertFalse(result.replayed());
        assertEquals("o2", result.order().getId());
    }

    @Test
    void sameKeyDifferentBodyIsRejected() {
        when(idempotency.insert(any(IdempotencyRecord.class))).thenThrow(new DuplicateKeyException("E11000"));
        when(idempotency.findById("k1")).thenReturn(Optional.of(new IdempotencyRecord("k1", "Asha|999", "o1", NOW)));

        assertThrows(IdempotencyKeyReusedException.class, () -> service.create("k1", "Asha", 500));
    }

    @Test
    void failedCreateReleasesTheKey() {
        when(idempotency.insert(any(IdempotencyRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orders.save(any(OrderDocument.class))).thenThrow(new DataAccessResourceFailureException("db down"));

        assertThrows(DataAccessResourceFailureException.class, () -> service.create("k1", "Asha", 500));
        verify(idempotency, times(1)).deleteById("k1");
    }

    @Test
    void staleIfMatchIsRefusedAndNothingIsSaved() {
        OrderDocument current = order("o1");
        ReflectionTestUtils.setField(current, "version", 3L);
        when(orders.findById("o1")).thenReturn(Optional.of(current));

        assertThrows(VersionMismatchException.class, () -> service.apply("o1", OrderAction.PAY, "\"2\""));
        verify(orders, never()).save(any());
    }
}
