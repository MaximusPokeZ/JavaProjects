package ru.mai.lessons.rpks.impl;

import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.ILineFinder;
import ru.mai.lessons.rpks.exception.LineCountShouldBePositiveException;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class LineFinder implements ILineFinder {

  private static final int ONE_BLOCK_SIZE = 500;
  private static final AtomicInteger processedBlocks = new AtomicInteger(0);
  private static final AtomicInteger countOfLines = new AtomicInteger(0);

  @Override
  public void find(String inputFilename, String outputFilename, String keyWord, int lineCount)
          throws LineCountShouldBePositiveException {

    if (!validateParameters(inputFilename, outputFilename, keyWord, lineCount)) {
      log.error("Invalid data in parameters");
      return;
    }

    if (lineCount < 0) {
      throw new LineCountShouldBePositiveException("Line count should be a positive number");
    }

    if (keyWord.isEmpty()) {
      try {
        createEmptyFile(outputFilename);
      } catch (IOException e) {
        log.error("Error while creating an empty file {}", outputFilename, e);
      }
      return;
    }

    Set<Long> linesToWrite;
    try {
      linesToWrite = findLinesInFile(inputFilename, keyWord);
    } catch (IOException e) {
      log.error("Error while reading file {}", inputFilename, e);
      return;
    } catch (ExecutionException | InterruptedException e) {
      log.error("Error while getting futures", e);
      return;
    }

    writeLines(outputFilename, inputFilename, lineCount, linesToWrite);
  }

  private Set<Long> findLinesInFile(String inFile, String keyWord) throws IOException, ExecutionException, InterruptedException {
    ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    int keySize = keyWord.getBytes().length;
    long currentPosition = 0;
    List<Future<List<Long>>> futureList = new ArrayList<>();

    long fileSize;

    try (RandomAccessFile file = new RandomAccessFile(inFile, "r")) {
      fileSize = file.length();
    } catch (IOException e) {
      log.error("Error while opening the file {}", inFile, e);
      return new HashSet<>();
    }

    long totalBlocks = (fileSize + ONE_BLOCK_SIZE - keySize - 1) / (ONE_BLOCK_SIZE - keySize);

    while (currentPosition < fileSize) {
      long finalCurrentPosition = currentPosition;
      futureList.add(exec.submit(() -> {
        try {
          return findLineThreadTask(inFile, finalCurrentPosition, keyWord);
        } catch (Exception e) {
          log.error("Error while processing block at position {}", finalCurrentPosition, e);
          return new ArrayList<>();
        }
      }));
      currentPosition += ONE_BLOCK_SIZE - keySize;
    }

    shutdownExecutor(exec);

    Set<Long> linesToWrite = new TreeSet<>();
    List<Future<List<Long>>> newFutureList;

    while (!futureList.isEmpty()) {
      newFutureList = new ArrayList<>();
      for (Future<List<Long>> longFuture : futureList) {
        if (longFuture.isDone()) {
          try {
            linesToWrite.addAll(longFuture.get());
          } catch (InterruptedException e) {
            log.error("Error while getting result from future", e);
          }
        } else {
          newFutureList.add(longFuture);
        }
      }
      futureList = newFutureList;
      displayProgress(totalBlocks);
    }

    return linesToWrite;
  }

  private void writeLines(String outputFile, String inputFile, int numLines, Set<Long> linePositions) {
    log.info("Writing result to file");

    try (RandomAccessFile OFile = new RandomAccessFile(outputFile, "rw");
         RandomAccessFile IFile = new RandomAccessFile(inputFile, "r")) {

      Set<Long> positions = new TreeSet<>();
      long outFilePointer = 0;

      for (long position : linePositions) {
        byte c;
        long currentPosition = position - 1;
        int lines = numLines;

        List<Byte> buffer = new ArrayList<>();

        while (currentPosition > -1 && lines > -1) {
          IFile.seek(currentPosition--);
          c = IFile.readByte();

          if (c == '\n') {
            --lines;
          }
          if (lines != -1) {
            buffer.add(c);
          }
        }

        if (!positions.contains(currentPosition)) {

          if (outFilePointer != 0) {
            OFile.writeBytes("\n");
          }

          positions.add(currentPosition);
          OFile.seek(outFilePointer);
          outFilePointer += buffer.size();
          writeFile(OFile, buffer, buffer.size() - 1, -1);

          buffer = new ArrayList<>();
          currentPosition = position;
          lines = numLines;
          while (currentPosition < IFile.length() && lines > -1) {
            IFile.seek(currentPosition++);
            c = IFile.readByte();
            if (c == '\n') {
              --lines;
            }
            if (lines != -1) {
              buffer.add(c);
            }
          }

          OFile.seek(outFilePointer);
          outFilePointer += buffer.size() + 1;
          writeFile(OFile, buffer, 0, buffer.size());
        }
      }
      log.info("{} lines containing keyword were written", positions.size());
    } catch (IOException e) {
      log.error("An error occurred while processing files", e);
    }
  }


  private void writeFile(RandomAccessFile file, List<Byte> buffer, int start, int finish) throws IOException {
    if (start < finish) {
      for (int i = start; i < finish; i++) {
        file.write(buffer.get(i));
      }
    } else {
      for (int i = start; i > finish; i--) {
        file.write(buffer.get(i));
      }
    }
  }

  private List<Long> findLineThreadTask(String fileName, long seek, String keyWord) {
    byte[] bytesToSearch;
    int bytesToRead;

    try (RandomAccessFile file = new RandomAccessFile(fileName, "r")) {
      file.seek(seek);
      bytesToRead = (int) Math.min(file.length() - seek, ONE_BLOCK_SIZE);
      bytesToSearch = new byte[bytesToRead];
      file.read(bytesToSearch);
    } catch (IOException e) {
      log.error("Error while reading file in a thread", e);
      return new ArrayList<>();
    }

    byte[] upperKeyWord = keyWord.toUpperCase().getBytes();
    byte[] lowerKeyWord = keyWord.toLowerCase().getBytes();
    List<Long> positions = new ArrayList<>();

    for (int i = 0; i < bytesToSearch.length - lowerKeyWord.length; i++) {
      if (isKeywordMatch(bytesToSearch, i, upperKeyWord, lowerKeyWord)) {
        positions.add(seek + i);
      }
    }

    return positions;
  }

  private boolean isKeywordMatch(byte[] bytes, int index, byte[] upperKey, byte[] lowerKey) {
    for (int i = 0; i < lowerKey.length; i++) {
      byte currentByte = bytes[index + i];
      if (currentByte != upperKey[i] && currentByte != lowerKey[i]) {
        return false;
      }
    }
    return true;
  }

  private void createEmptyFile(String outputFilename) throws IOException {
    File file = new File(outputFilename);
    file.createNewFile();
  }

  private boolean validateParameters(String inputFilename, String outputFilename, String keyWord, int lineCount)
          throws LineCountShouldBePositiveException {
    if (inputFilename == null || inputFilename.isEmpty() || !Files.exists(Paths.get(inputFilename))) {
      log.error("Input file is not valid");
      return false;
    }
    if (outputFilename == null || outputFilename.isEmpty()) {
      log.error("Output filename is not specified");
      return false;
    }
    if (keyWord == null || keyWord.isEmpty()) {
      log.error("Keyword is not specified");
      return false;
    }
    if (lineCount < 0) {
      throw new LineCountShouldBePositiveException("The number of lines should be non-negative");
    }
    return true;
  }

  private void shutdownExecutor(ExecutorService executor) {
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
      log.error("Thread interrupted while waiting for executor termination", e);
    }
  }

  private void displayProgress(long countOfBlocks) {
    processedBlocks.addAndGet(1);
    double progress = (((double) processedBlocks.get() / countOfBlocks * 100) > 100.0) ? 100 : ((double) processedBlocks.get() / countOfBlocks * 100);
    log.info(String.format("File read: %.2f%%. Matches Found: %d%n", progress, countOfLines.get()));
  }
}
