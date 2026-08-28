package com.dantu.personal.hack.assembler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Command line entry point.
 *
 * <pre>
 *   hack-assembler &lt;source.asm&gt; [output.hack]
 * </pre>
 *
 * With no output path the result is written beside the source, with the
 * {@code .asm} extension replaced by {@code .hack}.
 */
public class App {

    private static final String SOURCE_EXTENSION = ".asm";
    private static final String OUTPUT_EXTENSION = ".hack";

    private static final int EXIT_OK = 0;
    private static final int EXIT_ERROR = 1;
    private static final int EXIT_USAGE = 2;

    public static void main(String[] args) {
        System.exit(run(args));
    }

    /**
     * Runs the assembler.
     *
     * @return the process exit code
     */
    static int run(String[] args) {
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            printUsage(System.out);
            return EXIT_OK;
        }

        if (args.length < 1 || args.length > 2) {
            System.err.println("error: expected 1 or 2 arguments but got " + args.length);
            printUsage(System.err);
            return EXIT_USAGE;
        }

        Path source = Path.of(args[0]);
        Path output = args.length == 2 ? Path.of(args[1]) : defaultOutputPath(source);

        if (!Files.isRegularFile(source)) {
            System.err.println("error: no such file: " + source);
            return EXIT_ERROR;
        }

        try {
            String machineCode = new HackAssembler().assemble(source);
            writeOutput(output, machineCode);
            System.err.printf("assembled %s -> %s%n", source, output);
            return EXIT_OK;
        } catch (SyntaxException e) {
            System.err.println(source + ": " + e.getMessage());
            return EXIT_ERROR;
        } catch (IllegalArgumentException e) {
            System.err.println(source + ": " + e.getMessage());
            return EXIT_ERROR;
        } catch (RuntimeException e) {
            // TODO: narrow this once SymbolTable throws SyntaxException instead of
            // a bare RuntimeException for duplicate labels.
            System.err.println(source + ": " + e.getMessage());
            return EXIT_ERROR;
        } catch (IOException e) {
            System.err.println("error: " + e.getMessage());
            return EXIT_ERROR;
        }
    }

    /**
     * Replaces a trailing {@code .asm} with {@code .hack}, or appends
     * {@code .hack} if the source has no {@code .asm} extension.
     */
    static Path defaultOutputPath(Path source) {
        String name = source.getFileName().toString();
        String base = name.endsWith(SOURCE_EXTENSION)
                ? name.substring(0, name.length() - SOURCE_EXTENSION.length())
                : name;

        Path parent = source.getParent();
        String outputName = base + OUTPUT_EXTENSION;
        return parent == null ? Path.of(outputName) : parent.resolve(outputName);
    }

    private static void writeOutput(Path output, String machineCode) throws IOException {
        // A .hack file is line separated; keep the trailing newline so the file
        // is well formed even when the program is empty.
        Files.writeString(output, machineCode.isEmpty() ? "" : machineCode + "\n");
    }

    private static void printUsage(java.io.PrintStream out) {
        out.println("""
                usage: hack-assembler <source.asm> [output.hack]

                Assembles a Hack assembly file into 16-bit machine code.
                With no output path, writes alongside the source using the
                .hack extension.""");
    }
}
