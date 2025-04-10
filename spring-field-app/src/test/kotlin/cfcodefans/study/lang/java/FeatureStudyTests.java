package cfcodefans.study.lang.java;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class FeatureStudyTests {
    static Logger log = LoggerFactory.getLogger(FeatureStudyTests.class);

    @Test
    public void testPrivateMethodsInInterface() {
        interface IOper {
            Logger log = LoggerFactory.getLogger(IOper.class);
            AtomicLong count = new AtomicLong();
            AtomicLong failedCount = new AtomicLong();

            private static long count() {
                return count.getAndIncrement();
            }

            private static long failedCount() {
                return failedCount.getAndIncrement();
            }

            private void logBegin() {
                log.info("oper begin for {} times", count());
            }

            private void logFailed() {
                log.info("oper failed for {} times", failedCount());
            }

            default <T> T act(Supplier<T> operBody) {
                logBegin();
                try {
                    return operBody.get();
                } catch (Throwable t) {
                    logFailed();
                    log.error("failed", t);
                    return null;
                }
            }
        }

        IOper operRunner = new IOper() {
        };

        for (long i = -5; i < 5; i++) {
            final long _i = i;
            operRunner.act(() -> 10 / _i);
        }
    }

    @Test
    public void testStreamMethodJDK9() {
        Assertions.assertEquals(-15, IntStream
                .range(-5, 6)
                .takeWhile(i -> i < 0)
                .sum());

        Assertions.assertEquals(15, IntStream
                .range(-5, 6)
                .dropWhile(i -> i < 0)
                .sum());

        Assertions.assertEquals(15, IntStream
                .iterate(5, i -> i > 0, i -> i - 1)
                .sum());
    }
}
