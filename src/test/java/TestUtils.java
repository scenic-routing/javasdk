package me.callsen.taylor.scenicrouting.javasdk;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import me.callsen.taylor.scenicrouting.javasdk.data.GraphDb;

public class TestUtils {

  public static GraphDb getEmptyGraphDb() throws IOException, URISyntaxException {
    // create temporary dir
    Path tempDirectory = Files.createTempDirectory("scenicrouting-testdb");

    return new GraphDb(tempDirectory.toFile().getAbsolutePath());
  }

  public static GraphDb getLoadedGraphDb() throws IOException, URISyntaxException {
    ClassLoader classLoader = GraphDb.class.getClassLoader();

    // create temporary dir
    Path tempDirectory = Files.createTempDirectory("scenicrouting-testdb");

    // copy graph.db into temporary dir to avoid persisting any changes - supports
    //  standard copy for this module's tests, and special copy to enable this 
    //  module/method to be called from another module (by copying graph.db out of
    //  maven dependency jar)
    String dbResourcePath = classLoader.getResource("neo4j/graph.db").getPath();
    if (dbResourcePath.startsWith("jar:") || dbResourcePath.contains("!")) {
      // method called from other module - copy resource out of jar file
      copyDir(classLoader, "neo4j/graph.db", tempDirectory);
    } else {
      // method called by this module's tests - perform standard copy
      FileUtils.copyDirectory(new File(classLoader.getResource("neo4j/graph.db").getFile()), tempDirectory.toFile());
    }

    return new GraphDb(tempDirectory.toFile().getAbsolutePath());
  }

  // utility function to help copy graph.db dir out of jar
  //  https://stackoverflow.com/questions/70401207/copy-directory-from-a-jar-file-using-only-pure-java
  private static void copyDir(ClassLoader classLoader, String resPath, Path target) throws IOException, URISyntaxException {
    System.out.println("copyDir(" + resPath + ", " + target + ")");

    URI uri = classLoader.getResource(resPath).toURI();

    BiPredicate<Path, BasicFileAttributes> foreach = (p, a) -> copy(p, a, Path.of(target.toString(), p.toString()))
        && false;

    try (var fs = FileSystems.newFileSystem(uri, Map.of())) {
      final Path subdir = fs.getPath(resPath);
      for (Path root : fs.getRootDirectories()) {
        try (Stream<Path> stream = Files.find(subdir, Integer.MAX_VALUE, foreach)) {
          stream.count();
        }
      }
    }
  }

  // utility function to help copy graph.db dir out of jar
  //  https://stackoverflow.com/questions/70401207/copy-directory-from-a-jar-file-using-only-pure-java
  private static boolean copy(Path from, BasicFileAttributes a, Path target) {
    try {
      if (a.isDirectory())
        Files.createDirectories(target);
      else if (a.isRegularFile())
        Files.copy(from, target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return true;
  }

}
