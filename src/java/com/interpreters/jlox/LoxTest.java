package com.interpreters.jlox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class LoxTest {

    // jlox/interpreters/com/java/resources/tests
    private static final Path TEST_DIR = Paths.get("src/resources/tests").toAbsolutePath();

    public static void main(String[] args) throws IOException {
        List<Path> files = Files.walk(TEST_DIR)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".lox"))
                .collect(Collectors.toList());
        for (Path loxFile : files) {
            Lox.main(new String[]{ loxFile.toAbsolutePath().toString() });
        }
    }
}
