package assos.hytale.shop.storage;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JsonFiles {

    private static final Logger LOGGER = Logger.getLogger(JsonFiles.class.getName());

    private JsonFiles() {
    }

    /**
     * Writes to a temporary sibling first and keeps a {@code .bak} copy of the previous file, so a crash
     * mid-write never leaves a truncated data file behind.
     */
    public static void writeAtomically(Path file, Gson gson, Object value) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            gson.toJson(value, writer);
        }

        if (Files.exists(file)) {
            Path backup = file.resolveSibling(file.getFileName() + ".bak");
            try {
                Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                LOGGER.log(Level.WARNING, "Could not back up " + file, exception);
            }
        }

        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
