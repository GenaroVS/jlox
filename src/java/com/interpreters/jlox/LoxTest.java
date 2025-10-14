package com.interpreters.jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LoxTest {

    private static final Path TEST_DIR = Paths.get("src/resources/tests").toAbsolutePath();
    private static final Path CLASS_PATH = Paths.get("out/production/jlox").toAbsolutePath();

    public static void main(String[] args) throws IOException, InterruptedException {
        List<Path> files = Files.walk(TEST_DIR)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".lox"))
                .collect(Collectors.toList());
        for (Path loxFile : files) {
            System.out.printf("------------  Test File: %s  -------------%n", loxFile.getFileName().toString());
            runLox(loxFile.toString());
            System.out.println();
        }
    }

    private static void runLox(String file) throws IOException, InterruptedException {
        List<String> command = Arrays.asList(
                "java",
                "-cp", CLASS_PATH.toString(),
                "com.interpreters.jlox.Lox",
                file
        );

        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();

        Thread outThread = new Thread(() -> {
            try (BufferedReader outReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = outReader.readLine()) != null) {
                    System.out.println("[STDOUT] " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Thread errThread = new Thread(() -> {
            try (BufferedReader errReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errReader.readLine()) != null) {
                    System.err.println("[STDERR] " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        outThread.start();
        errThread.start();

        int exitCode = process.waitFor();
        outThread.join();
        errThread.join();

        System.out.println("Jlox exited with code: " + exitCode);
    }
}
