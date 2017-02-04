package kaleidok.net.http.cache;

import org.apache.http.annotation.ThreadSafe;
import org.apache.http.impl.client.cache.AsynchronousValidationRequest;
import org.apache.http.impl.client.cache.SchedulingStrategy;

import java.util.concurrent.ExecutorService;


@ThreadSafe
public class ExecutorSchedulingStrategy implements SchedulingStrategy {

    private final ExecutorService executor;

    public ExecutorSchedulingStrategy( ExecutorService executor ) {
        this.executor = executor;
    }

    @Override
    public void schedule( AsynchronousValidationRequest revalidationRequest ) {
        executor.execute(revalidationRequest);
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
