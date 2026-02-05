package redis.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Command {
    private final List<String> parts;

    public Command(List<String> parts) {
        if (parts == null || parts.isEmpty()) {
            this.parts = Collections.emptyList();
        } else {
            this.parts = new ArrayList<String>(parts);
        }
    }

    public String getName() {
        if (parts.isEmpty()) {
            return "";
        }
        return parts.get(0).toUpperCase();
    }

    public List<String> getArgs() {
        if (parts.size() <= 1) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(parts.subList(1, parts.size()));
    }

    public List<String> getParts() {
        return Collections.unmodifiableList(parts);
    }

    public boolean isEmpty() {
        return parts.isEmpty();
    }
}
