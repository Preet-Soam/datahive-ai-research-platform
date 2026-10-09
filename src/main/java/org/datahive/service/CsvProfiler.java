package org.datahive.service;

import org.datahive.model.ColumnProfile;

import java.io.IOException;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Profiles a bounded CSV upload without loading its entire contents into memory. */
public final class CsvProfiler {
    public static final int MAX_COLUMNS = 50;
    public static final long MAX_ROWS = 50_000;

    public Profile inspect(Path path) throws IOException {
        try (PushbackReader reader = new PushbackReader(
                Files.newBufferedReader(path, StandardCharsets.UTF_8), 2)) {
            List<String> header = readRecord(reader);
            if (header == null || header.isEmpty()) {
                throw new IOException("The CSV file is empty");
            }
            if (header.size() > MAX_COLUMNS) {
                throw new IOException("This workspace supports up to " + MAX_COLUMNS + " columns per CSV");
            }
            if (!header.isEmpty() && header.get(0).startsWith("\uFEFF")) {
                header.set(0, header.get(0).substring(1));
            }
            validateHeaders(header);

            List<Accumulator> columns = new ArrayList<>(header.size());
            for (String name : header) {
                columns.add(new Accumulator(name.trim()));
            }

            long rows = 0;
            List<String> record;
            while ((record = readRecord(reader)) != null) {
                if (record.size() > columns.size()) {
                    throw new IOException("A CSV row has more values than the header");
                }
                rows++;
                if (rows > MAX_ROWS) {
                    throw new IOException("This workspace supports up to " + MAX_ROWS + " data rows per CSV");
                }
                for (int index = 0; index < columns.size(); index++) {
                    String value = index < record.size() ? record.get(index).trim() : "";
                    columns.get(index).accept(value);
                }
            }

            List<ColumnProfile> profiles = columns.stream().map(Accumulator::toProfile).toList();
            return new Profile(rows, header.size(), profiles);
        }
    }

    /** Reads a bounded table for a training run; the existing upload limits also cap memory use. */
    public Table readTable(Path path) throws IOException {
        try (PushbackReader reader = new PushbackReader(
                Files.newBufferedReader(path, StandardCharsets.UTF_8), 2)) {
            List<String> header = readRecord(reader);
            if (header == null || header.isEmpty()) throw new IOException("The CSV file is empty");
            if (header.get(0).startsWith("\uFEFF")) header.set(0, header.get(0).substring(1));
            if (header.size() > MAX_COLUMNS) throw new IOException("The CSV has too many columns");
            validateHeaders(header);
            List<List<String>> rows = new ArrayList<>();
            List<String> row;
            while ((row = readRecord(reader)) != null) {
                if (rows.size() >= MAX_ROWS) throw new IOException("The CSV has too many rows for a training run");
                if (row.size() > header.size()) throw new IOException("A CSV row has more values than the header");
                List<String> padded = new ArrayList<>(header.size());
                for (int column = 0; column < header.size(); column++) {
                    padded.add(column < row.size() ? row.get(column).trim() : "");
                }
                rows.add(List.copyOf(padded));
            }
            return new Table(List.copyOf(header.stream().map(String::trim).toList()), List.copyOf(rows));
        }
    }

    private static void validateHeaders(List<String> headers) throws IOException {
        Set<String> seen = new HashSet<>();
        for (String header : headers) {
            String normalized = header == null ? "" : header.trim();
            if (normalized.isEmpty()) {
                throw new IOException("Every CSV column needs a header");
            }
            if (normalized.length() > 190) {
                throw new IOException("A CSV header is longer than 190 characters");
            }
            if (!seen.add(normalized.toLowerCase(Locale.ROOT))) {
                throw new IOException("CSV headers must be unique (case-insensitive)");
            }
        }
    }

    private static List<String> readRecord(PushbackReader reader) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean quoteClosed = false;
        boolean recordStarted = false;

        while (true) {
            int next = reader.read();
            if (next == -1) {
                if (inQuotes) {
                    throw new IOException("A quoted CSV field was not closed");
                }
                if (!recordStarted && fields.isEmpty() && field.isEmpty()) {
                    return null;
                }
                fields.add(field.toString());
                return fields;
            }

            char character = (char) next;
            recordStarted = true;
            if (inQuotes) {
                if (character == '"') {
                    int afterQuote = reader.read();
                    if (afterQuote == '"') {
                        field.append('"');
                    } else {
                        inQuotes = false;
                        quoteClosed = true;
                        if (afterQuote != -1) {
                            reader.unread(afterQuote);
                        }
                    }
                } else {
                    field.append(character);
                }
                continue;
            }

            if (quoteClosed) {
                if (character == ',') {
                    fields.add(field.toString());
                    field.setLength(0);
                    quoteClosed = false;
                    continue;
                }
                if (character == '\n' || character == '\r') {
                    consumeLineFeed(reader, character);
                    fields.add(field.toString());
                    return fields;
                }
                if (Character.isWhitespace(character)) {
                    continue;
                }
                throw new IOException("Unexpected text after a quoted CSV field");
            }

            if (character == '"') {
                if (field.length() != 0) {
                    throw new IOException("A quote appeared inside an unquoted CSV field");
                }
                inQuotes = true;
            } else if (character == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else if (character == '\n' || character == '\r') {
                consumeLineFeed(reader, character);
                fields.add(field.toString());
                return fields;
            } else {
                field.append(character);
            }
        }
    }

    private static void consumeLineFeed(PushbackReader reader, char newline) throws IOException {
        if (newline == '\r') {
            int next = reader.read();
            if (next != '\n' && next != -1) {
                reader.unread(next);
            }
        }
    }

    public record Profile(long rowCount, int columnCount, List<ColumnProfile> columns) {
        public Profile {
            columns = List.copyOf(columns);
        }
    }

    public record Table(List<String> headers, List<List<String>> rows) {
        public Table {
            headers = List.copyOf(headers);
            rows = List.copyOf(rows);
        }
    }

    private static final class Accumulator {
        private final String name;
        private final Set<String> distinct = new HashSet<>();
        private final List<String> samples = new ArrayList<>(4);
        private long missing;
        private boolean anyValue;
        private boolean allBoolean = true;
        private boolean allInteger = true;
        private boolean allDecimal = true;

        private Accumulator(String name) {
            this.name = name;
        }

        private void accept(String value) {
            if (value == null || value.isBlank()) {
                missing++;
                return;
            }
            anyValue = true;
            distinct.add(value);
            if (samples.size() < 4 && samples.stream().noneMatch(value::equals)) {
                samples.add(value.length() > 42 ? value.substring(0, 39) + "…" : value);
            }
            String normalized = value.toLowerCase(Locale.ROOT);
            allBoolean &= normalized.equals("true") || normalized.equals("false") || normalized.equals("yes") || normalized.equals("no");
            try {
                Long.parseLong(value);
            } catch (NumberFormatException exception) {
                allInteger = false;
            }
            try {
                new BigDecimal(value);
            } catch (NumberFormatException exception) {
                allDecimal = false;
            }
        }

        private ColumnProfile toProfile() {
            String type = !anyValue ? "EMPTY" : allBoolean ? "BOOLEAN" : allInteger ? "INTEGER" :
                    allDecimal ? "DECIMAL" : "TEXT";
            return new ColumnProfile(name, type, missing, distinct.size(), String.join(" · ", samples));
        }
    }
}
