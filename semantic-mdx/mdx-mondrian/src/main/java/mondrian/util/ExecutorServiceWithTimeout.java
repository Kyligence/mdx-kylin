package mondrian.util;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public interface ExecutorServiceWithTimeout extends ExecutorService {
    void submitAll(List<Runnable> commands, long timeout, TimeUnit unit);
}
