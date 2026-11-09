import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps the code shown in the task markdown files identical to the real, compiled and tested sources.
 *
 * <p>In a task file, write a pair of markers:
 *
 * <pre>
 * &lt;!-- embed: solutions/src/main/java/io/github/chris013/practice/task01/Money.java --&gt;
 * &lt;!-- /embed --&gt;
 * </pre>
 *
 * and run {@code java tools/EmbedCode.java} from the repository root. Everything between the markers is replaced
 * with a link to the file and its content. {@code java tools/EmbedCode.java --check} only reports outdated files and
 * exits with status 1 if there are any (CI uses this).
 */
public class EmbedCode {

    private static final Pattern EMBED_BLOCK =
            Pattern.compile("(<!-- embed: (\\S+) -->)\\R.*?(<!-- /embed -->)", Pattern.DOTALL);

    public static void main(String[] args) throws IOException {
        boolean checkOnly = List.of(args).contains("--check");
        Path root = Path.of("").toAbsolutePath();

        List<Path> outdated = new ArrayList<>();
        for (Path task : markdownFilesIn(root.resolve("tasks"))) {
            String original = Files.readString(task);
            String embedded = embedAll(original, root);
            if (!embedded.equals(original)) {
                outdated.add(root.relativize(task));
                if (!checkOnly) {
                    Files.writeString(task, embedded);
                }
            }
        }

        if (checkOnly && !outdated.isEmpty()) {
            System.err.println("Out of date, run `java tools/EmbedCode.java`: " + outdated);
            System.exit(1);
        }
        System.out.println((checkOnly ? "Up to date. " : "Updated: ") + outdated);
    }

    private static List<Path> markdownFilesIn(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return files.filter(file -> file.toString().endsWith(".md")).sorted().toList();
        }
    }

    private static String embedAll(String markdown, Path root) {
        return EMBED_BLOCK.matcher(markdown).replaceAll(block -> {
            String startMarker = block.group(1);
            String sourcePath = block.group(2);
            String endMarker = block.group(3);
            String replacement = startMarker + "\n" + renderedSource(root, sourcePath) + endMarker;
            return Matcher.quoteReplacement(replacement);
        });
    }

    private static String renderedSource(Path root, String sourcePath) {
        Path file = root.resolve(sourcePath);
        String fileName = file.getFileName().toString();
        String language = fileName.substring(fileName.lastIndexOf('.') + 1);
        return """
                **[`%s`](../%s)**

                ```%s
                %s
                ```
                """.formatted(fileName, sourcePath, language, read(file).stripTrailing());
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot embed " + file, e);
        }
    }
}
