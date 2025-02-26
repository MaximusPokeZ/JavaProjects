package ru.mai.lessons.rpks.impl;

import ru.mai.lessons.rpks.IDirectorySizeChecker;
import ru.mai.lessons.rpks.exception.DirectoryAccessException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DirectorySizeChecker implements IDirectorySizeChecker {
  private static final String START_PATH = "src/test/resources/";

  @Override
  public String checkSize(String directoryName) throws DirectoryAccessException {
    Path path = Paths.get(START_PATH, directoryName);

    if (!Files.exists(path)) {
      throw new DirectoryAccessException("Error: directory " + path + " doesn't exist");
    }

    try {
      long totalSize = calculateSizeOfDirectory(path);
      SizeResult sizeResult = calculateMbGb(totalSize);

      return totalSize == 0 ? "0 bytes" : String.format("%s ---- %d bytes / %d Mb / %d Gb", directoryName, totalSize, sizeResult.sizeInMb, sizeResult.sizeInGb);
    } catch (IOException e) {
      throw  new DirectoryAccessException("Error: Unable to calculate size for directory " + path + " " + e);
    }
  }

  private long calculateSizeOfDirectory(Path path) throws IOException {
    long totalSize = 0;

    try (var stream = Files.list(path)) {
      for (Path file : (Iterable<Path>) stream::iterator) {
        if (Files.isDirectory(file)) {
          totalSize += calculateSizeOfDirectory(file);
        } else {
          totalSize += Files.size(file);
        }
      }
    }

    return totalSize;
  }

  private SizeResult calculateMbGb(long totalSizeInBytes) {
    long sizeInMb = totalSizeInBytes / (1024 * 1024);
    long sizeInGb = totalSizeInBytes / (1024 * 1024 * 1024);
    return new SizeResult(sizeInMb, sizeInGb);
  }

  static class SizeResult {
    long sizeInMb;
    long sizeInGb;

    public SizeResult(long sizeInMb, long sizeInGb) {
      this.sizeInMb = sizeInMb;
      this.sizeInGb = sizeInGb;
    }
  }
}
