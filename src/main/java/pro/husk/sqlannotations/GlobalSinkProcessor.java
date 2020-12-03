package pro.husk.sqlannotations;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GlobalSinkProcessor {

    private static GlobalSinkProcessor globalSinkProcessor;

    @Getter
    private final ScheduledThreadPoolExecutor threadPoolExecutor;

    @Getter
    private final List<SinkProcessor> processorList;

    private final ScheduledFuture<?> updateTask;

    public GlobalSinkProcessor() {
        this((ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5));
    }

    public GlobalSinkProcessor(ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
        this.threadPoolExecutor = scheduledThreadPoolExecutor;
        this.processorList = new ArrayList<>();
        this.updateTask = threadPoolExecutor
                .scheduleAtFixedRate(this::runUpdates, 10, 10, TimeUnit.SECONDS);
    }

    public void registerSinkProcessor(SinkProcessor sinkProcessor, Runnable finishedLoadingTask) {
        sinkProcessor.setLoadFuture(CompletableFuture
                .runAsync(sinkProcessor::loadFromDatabase, threadPoolExecutor)
                .thenRun(() -> processorList.add(sinkProcessor)).thenRun(finishedLoadingTask));
    }

    private void runUpdates() {
        processorList.forEach(SinkProcessor::runUpdate);
    }

    public void cancelUpdateTask() {
        updateTask.cancel(false);
    }

    public static GlobalSinkProcessor getInstance() {
        if (globalSinkProcessor == null) {
            globalSinkProcessor = new GlobalSinkProcessor();
        }

        return globalSinkProcessor;
    }
}
