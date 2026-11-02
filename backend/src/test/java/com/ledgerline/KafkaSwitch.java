package com.ledgerline;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaOperations;

/**
 * Simulates Kafka going down for the producer side. The application sends through {@link
 * #operations()}, which normally passes every call to the real {@code KafkaTemplate}; while the
 * switch is off, every send fails the way it would with the broker unreachable.
 */
public final class KafkaSwitch {

    private final AtomicBoolean down = new AtomicBoolean();
    private final KafkaOperations<?, ?> operations;

    KafkaSwitch(KafkaOperations<?, ?> real) {
        this.operations = (KafkaOperations<?, ?>) Proxy.newProxyInstance(
                KafkaOperations.class.getClassLoader(),
                new Class<?>[] {KafkaOperations.class},
                (proxy, method, args) -> call(real, method, args));
    }

    public void down() {
        down.set(true);
    }

    public void up() {
        down.set(false);
    }

    KafkaOperations<?, ?> operations() {
        return operations;
    }

    private Object call(KafkaOperations<?, ?> real, Method method, Object[] args) throws Throwable {
        if (down.get() && method.getName().startsWith("send")) {
            throw new KafkaException("Kafka is unreachable (simulated by the test)");
        }
        try {
            return method.invoke(real, args);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }
}
