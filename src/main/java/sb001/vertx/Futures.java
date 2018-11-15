package sb001.vertx;

import co.paralleluniverse.strands.Strand;
import io.vertx.core.Future;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Futures {

    @SneakyThrows
    public static <T> T getResult(Future<T> ft, long waitUntil) {

        long startedAt = System.currentTimeMillis();
        while (!ft.isComplete() && System.currentTimeMillis() - startedAt < waitUntil) {
            Strand.sleep(50);
        }

        if (!ft.isComplete()) {
            log.warn("Request look isn't complete! Timeout of {}ms has been reach.", waitUntil);
        }

        return ft.result();

    }

    public static <T> T getResult(Future<T> ft) {
        return getResult(ft, 2000);
    }

}
