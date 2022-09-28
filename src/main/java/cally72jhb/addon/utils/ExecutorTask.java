package cally72jhb.addon.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorTask {
    private static ExecutorService EXECUTOR;

    public static void init() {
        EXECUTOR = Executors.newSingleThreadExecutor();
    }

    // Utils

    public static void execute(Runnable task) {
        EXECUTOR.execute(task);
    }
}
