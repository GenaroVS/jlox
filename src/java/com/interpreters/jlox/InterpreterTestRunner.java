package com.interpreters.jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class InterpreterTestRunner {

    private static final Path TEST_DIR = Paths.get("src/resources/tests").toAbsolutePath();
    private static final Path EXPECTED_DIR = Paths.get("src/resources/expected").toAbsolutePath();
    private static final Path CLASS_PATH = Paths.get("out/production/jlox").toAbsolutePath();

    private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors() * 2;
    private static final ExecutorService TEST_EXECUTOR = Executors.newFixedThreadPool(MAX_THREADS);
    private static boolean GENERATE_MODE = false;

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && (args[0].equals("--generate") || args[0].equals("-g"))) {
            GENERATE_MODE = true;
        }

        List<Path> files = Files.walk(TEST_DIR)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".lox"))
                .collect(Collectors.toList());

        println("Mode: " + (GENERATE_MODE ? "GENERATE golden outputs" : "VERIFY against golden outputs"));
        if (!GENERATE_MODE) {
            println("Running " + files.size() + " test files...");
        }

        List<Future<TestResult>> results = new ArrayList<>();
        for (Path program : files) {
            results.add(TEST_EXECUTOR.submit(() -> run(program)));
        }
        TEST_EXECUTOR.shutdown();

        if (!GENERATE_MODE) {
            int passed = 0;
            for (Future<TestResult> future : results) {
                TestResult result = future.get();
                System.out.println(result);
                if (result.passed()) {
                    passed++;
                }
            }
            println("");
            System.out.printf("Summary: %d/%d tests passed.%n", passed, results.size());
        } else {
            for (Future<TestResult> future : results) {
                future.get(); // wait for completion
            }
            println("\nGolden files updated successfully.");
        }
    }

    private static TestResult run(Path program) {
        Path expectedOutputPath = resolveExpectedOutputPath(program);
        List<String> command = Arrays.asList(
                "java",
                "-cp", CLASS_PATH.toString(),
                "com.interpreters.jlox.Lox",
                program.toString()
        );
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            Process process = builder.start();
            Thread outThread = new Thread(() -> readStream(process.getInputStream(), stdout));
            Thread errThread = new Thread(() -> readStream(process.getErrorStream(), stderr));
            outThread.start();
            errThread.start();

            int exitCode = process.waitFor();
            outThread.join();
            errThread.join();

            String actualOutput = stdout.toString();

            if (GENERATE_MODE) {
                //println(expectedOutputPath.toString());
                Files.writeString(expectedOutputPath, actualOutput);
                return new TestResult(program, true, exitCode, actualOutput, stderr.toString(), "[generated]");
            } else {
                String expectedOutput = readExpectedOutput(expectedOutputPath);
                boolean passed = normalize(expectedOutput).equals(normalize(actualOutput));
                return new TestResult(program, passed, exitCode, actualOutput, stderr.toString(), expectedOutput);
            }
        } catch (Exception e) {
            return new TestResult(program, false, -1, stdout.toString(), stderr.toString(),
                    "[Error: " + e.getMessage() + "]");
        }
    }


    private static void readStream(InputStream input, StringBuilder target) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            String line;
            while ((line = reader.readLine()) != null) {
                target.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            target.append("[Stream read error: ").append(e.getMessage()).append("]");
        }
    }

    private static String readExpectedOutput(Path expectedPath) throws IOException {
        if (Files.exists(expectedPath)) {
            return new String(Files.readAllBytes(expectedPath));
        }
        return "";
    }

    private static Path resolveExpectedOutputPath(Path program) {
        return EXPECTED_DIR.resolve(program.getFileName().toString().replace(".lox", ".txt"));
    }

    private static String normalize(String text) {
        return text.trim().replaceAll("\\r\\n?", "\n");
    }

    private static class TestResult {
        private final Path file;
        private final boolean passed;
        private final int exitCode;
        private final String stdout;
        private final String stderr;
        private final String expected;

        public TestResult(Path file, boolean passed, int exitCode,
                          String stdout, String stderr, String expected) {
            this.file = file;
            this.passed = passed;
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.expected = expected;
        }

        public boolean passed() {
            return passed;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== ").append(file.getFileName()).append(" ===\n");
            sb.append("Exit code: ").append(exitCode).append("\n");
            sb.append("Result: ").append(passed ? "✅ PASSED" : "❌ FAILED").append("\n");
            sb.append("--- STDOUT ---\n");
            sb.append(stdout.isBlank() ? "[empty]\n" : stdout);
            sb.append("--- STDERR ---\n");
            sb.append(stderr.isBlank() ? "[empty]\n" : stderr);
            sb.append("--- EXPECTED ---\n");
            sb.append(expected.isBlank() ? "[none]\n" : expected);
            sb.append("\n");
            return sb.toString();
        }
    }
    private static void println(String s) {
        System.out.println(s);
    }
}
