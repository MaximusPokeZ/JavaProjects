package ru.mai.lessons.rpks.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.ILogAnalyzer;
import ru.mai.lessons.rpks.exception.WrongFilenameException;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class LogAnalyzer implements ILogAnalyzer {
  private static final String START_PATH = "src/test/resources/";
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private final Map<Integer, LocalDateTime> pendingResults = new ConcurrentHashMap<>();

  @Override
  public List<Integer> analyze(String filename, String deviation) throws WrongFilenameException {
    String fullFileName = START_PATH + filename;
    validateFile(fullFileName);

    Map<Integer, LogEntry> logEntries = new ConcurrentHashMap<>();
    long deviationMillis = parseDeviation(deviation);

    processLogFile(fullFileName, logEntries);
    long median = calculateMedian(logEntries);
    return findAnomalies(logEntries, deviationMillis, median);
  }

  private void validateFile(String filename) throws WrongFilenameException {
    File logFile = new File(filename);

    if (!logFile.exists() || !logFile.isFile()) {
      throw new WrongFilenameException("File not found: " + filename);
    }
  }

  private long parseDeviation(String deviation) {
    if (deviation == null || deviation.isBlank()) {
      return 1000;
    }

    String[] parts = deviation.split(" ");
    long value = Long.parseLong(parts[0]);
    String unit = parts[1].toLowerCase();

    return switch (unit) {
      case "sec" -> value * 1000;
      case "min" -> value * 60 * 1000;
      default -> throw new IllegalArgumentException("Unsupported deviation unit: " + unit);
    };
  }

  private void processLogFile(String filename, Map<Integer, LogEntry> logEntries) {
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    List<Future<Void>> futures = new ArrayList<>();

    try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
      String line;
      int totalLines = countLines(filename);
      int processedLines = 0;

      while ((line = reader.readLine()) != null) {
        String logLine = line;
        futures.add(executor.submit(() -> {
          processLogLine(logLine, logEntries);
          return null;
        }));
        processedLines++;
        displayProgress(processedLines, totalLines);
      }

      waitForCompletion(futures);
      shutdownAndAwaitTermination(executor);
    } catch (IOException e) {
      log.error("Error reading log file", e);
    }
  }

  private void processLogLine(String line, Map<Integer, LogEntry> logEntries) {
    try {
      String[] parts = line.split(" – ");
      LocalDateTime timestamp = LocalDateTime.parse(parts[0].trim(), formatter);
      String message = parts[2].trim();

      if (message.startsWith("QUERY FOR ID")) {
        int id = Integer.parseInt(message.split(" = ")[1]);
        logEntries.put(id, new LogEntry(timestamp));

        if (pendingResults.containsKey(id)) {
          LocalDateTime resultTime = pendingResults.remove(id);
          logEntries.get(id).setDuration(Duration.between(timestamp, resultTime).toMillis());
        }
      } else if (message.startsWith("RESULT QUERY FOR ID")) {
        int id = Integer.parseInt(message.split(" = ")[1]);
        LogEntry entry = logEntries.get(id);

        if (entry != null && entry.getStartTime() != null) {
          entry.setDuration(Duration.between(entry.getStartTime(), timestamp).toMillis());
        } else {
          pendingResults.put(id, timestamp);
        }
      }
    } catch (Exception e) {
      log.error("Error processing line: {}", line);
    }
  }

  private void displayProgress(int processedLines, int totalLines) {
      log.info("Processed {}/{} logs...", processedLines, totalLines);
  }

  private void waitForCompletion(List<Future<Void>> futures) {
    for (Future<Void> future : futures) {
      try {
        future.get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("Thread was interrupted while waiting for completion: {}", e.getMessage());
        return;
      } catch (ExecutionException e) {
        log.error("Error in thread execution: {}", e.getMessage());
      }
    }
  }


  private long calculateMedian(Map<Integer, LogEntry> logEntries) {
    List<Long> durationList = logEntries.values().stream()
            .map(LogEntry::getDuration)
            .sorted()
            .toList();

    int size = durationList.size();
    if (size == 0) {
      return 0;
    }
    if (size % 2 == 1) {
      return durationList.get(size / 2);
    }
    return (durationList.get(size / 2 - 1) + durationList.get(size / 2)) / 2;
  }

  private List<Integer> findAnomalies(Map<Integer, LogEntry> logEntries, long deviationMillis, long median) {
    return logEntries.entrySet().stream()
            .filter(entry -> Math.abs(entry.getValue().getDuration() - median) > deviationMillis)
            .map(Map.Entry::getKey)
            .toList();
  }

  private int countLines(String filename) throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
      return (int) reader.lines().count();
    }
  }

  @Setter
  @Getter
  private static class LogEntry {
    private LocalDateTime startTime;
    private Long duration;

    public LogEntry(LocalDateTime startTime) {
      this.startTime = startTime;
    }
  }

  private void shutdownAndAwaitTermination(ExecutorService executor) {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
          log.error("Executor did not terminate");
        }
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}