package io.github.chris013.practice.task14;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns configuration text into {@link Flag}s. This is the only code that knows the file format, and the only code
 * that knows line numbers, so every error is reported here, next to the line that caused it.
 *
 * <p>The parser works line by line. A non-indented line starts a flag; an indented line is a rule of the current
 * flag. Each line is split into a keyword and the rest, and the keyword decides what the line means. That is easier
 * to read and to get right than one regular expression for the whole grammar.
 */
final class FlagConfigParser {

    private static final Pattern FLAG_NAME = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Pattern COUNTRY_CODE = Pattern.compile("[A-Z]{2}");

    private final Map<String, FlagDraft> drafts = new LinkedHashMap<>();
    private FlagDraft current;
    private int lineNumber;

    private FlagConfigParser() {
    }

    static Map<String, Flag> parse(String configuration) {
        FlagConfigParser parser = new FlagConfigParser();
        // \R matches \n, \r\n and the other line terminators; -1 keeps trailing empty lines.
        String[] lines = configuration.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            parser.lineNumber = i + 1;
            parser.parseLine(lines[i]);
        }
        Map<String, Flag> flags = new LinkedHashMap<>();
        for (FlagDraft draft : parser.drafts.values()) {
            flags.put(draft.name, draft.build());
        }
        return Collections.unmodifiableMap(flags);
    }

    private void parseLine(String line) {
        String text = line.strip();
        if (text.isEmpty() || text.startsWith("#")) {
            return;
        }
        if (line.startsWith(" ") || line.startsWith("\t")) {
            parseRule(text);
        } else {
            parseFlagHeader(text);
        }
    }

    private void parseFlagHeader(String text) {
        Words words = Words.of(text);
        if (!words.keyword().equals("flag")) {
            throw error("expected 'flag <name>' at the start of a line but found '" + words.keyword() + "'");
        }
        String name = words.argument();
        if (name.isEmpty()) {
            throw error("flag needs a name");
        }
        if (!FLAG_NAME.matcher(name).matches()) {
            throw error("invalid flag name '" + name + "'");
        }
        if (drafts.containsKey(name)) {
            throw error("duplicate flag '" + name + "'");
        }
        current = new FlagDraft(name);
        drafts.put(name, current);
    }

    private void parseRule(String text) {
        Words words = Words.of(text);
        if (current == null) {
            throw error("rule '" + words.keyword() + "' appears before any flag");
        }
        String argument = words.argument();
        switch (words.keyword()) {
            case "users" -> current.rules.add(new UserRule(Set.copyOf(listOf("users", argument))));
            case "countries" -> current.rules.add(new CountryRule(Set.copyOf(countryCodes(argument))));
            case "percent" -> current.rules.add(new PercentRule(percent(argument)));
            case "default" -> setDefault(argument);
            case "off" -> killSwitch(argument);
            case "flag" -> throw error("flag lines must not be indented");
            default -> throw error("unknown keyword '" + words.keyword() + "'");
        }
    }

    private void setDefault(String argument) {
        if (current.defaultSet) {
            throw error("default is already set for flag '" + current.name + "'");
        }
        current.defaultSet = true;
        current.defaultOn = switch (argument) {
            case "on" -> true;
            case "off" -> false;
            default -> throw error("default must be on or off but was '" + argument + "'");
        };
    }

    private void killSwitch(String argument) {
        if (!argument.isEmpty()) {
            throw error("'off' takes no value but found '" + argument + "'");
        }
        current.killSwitch = true;
    }

    private List<String> countryCodes(String argument) {
        List<String> codes = listOf("countries", argument);
        for (String code : codes) {
            if (!COUNTRY_CODE.matcher(code).matches()) {
                throw error("country must be two uppercase letters but was '" + code + "'");
            }
        }
        return codes;
    }

    private int percent(String argument) {
        if (argument.isEmpty()) {
            throw error("percent needs a number");
        }
        int value;
        try {
            value = Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            throw error("percent must be a whole number but was '" + argument + "'");
        }
        if (value < 0 || value > 100) {
            throw error("percent must be between 0 and 100 but was " + value);
        }
        return value;
    }

    /** Splits a comma-separated argument. Every entry must be present; "a,,b" is an error, not a silent skip. */
    private List<String> listOf(String keyword, String argument) {
        if (argument.isEmpty()) {
            throw error(keyword + " needs at least one value");
        }
        List<String> values = new ArrayList<>();
        for (String raw : argument.split(",", -1)) {
            String value = raw.strip();
            if (value.isEmpty()) {
                throw error("empty entry in " + keyword + " list");
            }
            values.add(value);
        }
        return values;
    }

    private FlagConfigurationException error(String detail) {
        return new FlagConfigurationException(lineNumber, detail);
    }

    /** The first word of a line and the rest of it, trimmed. */
    private record Words(String keyword, String argument) {

        static Words of(String text) {
            String[] parts = text.split("\\s+", 2);
            return new Words(parts[0], parts.length == 2 ? parts[1].strip() : "");
        }
    }

    /** A flag while its lines are still being read. Turned into an immutable {@link Flag} at the end. */
    private static final class FlagDraft {

        private final String name;
        private final List<Rule> rules = new ArrayList<>();
        private boolean killSwitch;
        private boolean defaultSet;
        private boolean defaultOn;

        FlagDraft(String name) {
            this.name = name;
        }

        Flag build() {
            return new Flag(name, killSwitch, defaultOn, rules);
        }
    }
}
