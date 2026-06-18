package com.poc.testdata;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Automated Test Data Generator (PoC) - zero external dependencies.
 *
 * Generates configurable, realistic, privacy-safe test data for a User Profile
 * domain model. Mixes in deliberately INVALID records so QA can exercise both
 * happy-path and negative/edge-case scenarios. Outputs JSON and CSV.
 *
 * Build:  javac -d out src/main/java/com/poc/testdata/TestDataGenerator.java
 * Run:    java -cp out com.poc.testdata.TestDataGenerator [-n N] [-i R] [-o DIR] [--seed N]
 */
public class TestDataGenerator {

    static final String[] FIRST = {
        "Liam","Olivia","Noah","Emma","Aarav","Diya","Mateo","Sofia","Yuki","Hana",
        "Chen","Mei","Omar","Fatima","Lucas","Ava","Ethan","Isla","Raj","Priya"
    };
    static final String[] LAST = {
        "Smith","Johnson","Patel","Garcia","Kim","Nguyen","Ali","Rossi","Mueller",
        "Silva","Khan","Okafor","Tanaka","Lopez","Brown","Wang","Singh","Costa"
    };
    static final String[] CITIES = {
        "Springfield","Riverton","Fairview","Madison","Georgetown","Franklin",
        "Clinton","Salem","Bristol","Auburn","Greenville","Kingston"
    };
    static final String[] STATES = {
        "CA","NY","TX","FL","IL","WA","MA","GA","CO","OH","NC","AZ"
    };
    static final String[] STREETS = {
        "Maple St","Oak Ave","Pine Rd","Cedar Ln","Elm Dr","Birch Way",
        "Sunset Blvd","Lake View Dr","Hillcrest Ave","Park Pl"
    };
    static final String[] DOMAINS = { "example.com","testmail.org","sample.net","demo.io" };

    // --- Invalid edge-case values (for negative testing) --------------------
    static final String[] INVALID_EMAILS = {
        "plainaddress","@missinglocal.com","missingat.com","two@@at.com",
        "spaces in@email.com","trailingdot@email.",""
    };
    static final String[] INVALID_PHONES = { "123","abc-def-ghij","+1-000","","999999999999999999" };
    static final String[] BAD_NAMES = { ""," ","123456","<script>x</script>" };
    static final String[] BAD_DATES = { "not-a-date","2099-13-40","" };

    final Random rnd;

    public TestDataGenerator(Long seed) {
        this.rnd = (seed != null) ? new Random(seed) : new Random();
    }

    String pick(String[] a) { return a[rnd.nextInt(a.length)]; }

    LinkedHashMap<String, Object> validRecord() {
        String first = pick(FIRST);
        String last  = pick(LAST);
        var r = new LinkedHashMap<String, Object>();
        r.put("user_id", UUID.randomUUID().toString());
        r.put("name", first + " " + last);
        r.put("email", (first + "." + last).toLowerCase() + rnd.nextInt(1000) + "@" + pick(DOMAINS));
        r.put("phone", "+1" + (2000000000L + (long)(rnd.nextDouble() * 7999999999L)));
        r.put("street", (100 + rnd.nextInt(9900)) + " " + pick(STREETS));
        r.put("city", pick(CITIES));
        r.put("state", pick(STATES));
        r.put("zip", String.format("%05d", rnd.nextInt(100000)));
        r.put("age", 18 + rnd.nextInt(73));        // 18..90
        r.put("signup_dt", Instant.now().minus(rnd.nextInt(1095), ChronoUnit.DAYS).toString());
        r.put("_scenario", "valid");
        return r;
    }

    LinkedHashMap<String, Object> invalidRecord() {
        var r = validRecord();
        r.put("_scenario", "invalid");
        String[] fields = { "email","phone","age","name","signup_dt" };
        int corruptions = 1 + rnd.nextInt(3);
        var chosen = new ArrayList<String>();
        while (chosen.size() < corruptions) {
            String f = fields[rnd.nextInt(fields.length)];
            if (!chosen.contains(f)) chosen.add(f);
        }
        for (String f : chosen) {
            switch (f) {
                case "email" -> r.put("email", pick(INVALID_EMAILS));
                case "phone" -> r.put("phone", pick(INVALID_PHONES));
                case "age"   -> r.put("age", new int[]{-5,0,7,150,999}[rnd.nextInt(5)]);
                case "name"  -> r.put("name", pick(BAD_NAMES));
                case "signup_dt" -> r.put("signup_dt", pick(BAD_DATES));
            }
        }
        return r;
    }

    List<LinkedHashMap<String, Object>> generate(int count, double invalidRatio) {
        var list = new ArrayList<LinkedHashMap<String, Object>>();
        for (int i = 0; i < count; i++) {
            list.add(rnd.nextDouble() < invalidRatio ? invalidRecord() : validRecord());
        }
        return list;
    }

    static void writeJson(List<LinkedHashMap<String, Object>> records, Path path) throws IOException {
        var sb = new StringBuilder("[\n");
        for (int i = 0; i < records.size(); i++) {
            var rec = records.get(i);
            sb.append("  {\n");
            int j = 0;
            for (var e : rec.entrySet()) {
                sb.append("    \"").append(e.getKey()).append("\": ");
                Object v = e.getValue();
                if (v instanceof Number) sb.append(v);
                else sb.append("\"").append(jsonEscape(String.valueOf(v))).append("\"");
                sb.append(++j < rec.size() ? ",\n" : "\n");
            }
            sb.append(i + 1 < records.size() ? "  },\n" : "  }\n");
        }
        sb.append("]\n");
        try (FileWriter w = new FileWriter(path.toFile())) { w.write(sb.toString()); }
    }

    static void writeCsv(List<LinkedHashMap<String, Object>> records, Path path) throws IOException {
        if (records.isEmpty()) return;
        var headers = new ArrayList<>(records.get(0).keySet());
        try (FileWriter w = new FileWriter(path.toFile())) {
            w.write(String.join(",", headers) + "\n");
            for (var rec : records) {
                var cells = new ArrayList<String>();
                for (String h : headers) cells.add(csvEscape(String.valueOf(rec.get(h))));
                w.write(String.join(",", cells) + "\n");
            }
        }
    }

    static String jsonEscape(String v) {
        return v.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
    static String csvEscape(String v) {
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    public static void main(String[] args) throws IOException {
        int count = 50;
        double invalidRatio = 0.2;
        String outdir = "output";
        Long seed = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-n", "--count"         -> count = Integer.parseInt(args[++i]);
                case "-i", "--invalid-ratio" -> invalidRatio = Double.parseDouble(args[++i]);
                case "-o", "--outdir"        -> outdir = args[++i];
                case "--seed"                -> seed = Long.parseLong(args[++i]);
                case "-h", "--help"          -> { printHelp(); return; }
            }
        }

        var gen = new TestDataGenerator(seed);
        var records = gen.generate(count, invalidRatio);

        Path dir = Paths.get(outdir);
        Files.createDirectories(dir);
        writeJson(records, dir.resolve("users.json"));
        writeCsv(records, dir.resolve("users.csv"));

        long valid = records.stream().filter(r -> "valid".equals(r.get("_scenario"))).count();
        System.out.printf("Generated %d records (%d valid, %d invalid).%n",
                records.size(), valid, records.size() - valid);
        System.out.printf("Output written to %s/users.json and %s/users.csv%n", outdir, outdir);
    }

    static void printHelp() {
        System.out.println("""
            Automated Test Data Generator (PoC)
            Usage: java -cp out com.poc.testdata.TestDataGenerator [options]
              -n, --count N           number of records (default 50)
              -i, --invalid-ratio R   fraction invalid 0.0-1.0 (default 0.2)
              -o, --outdir DIR        output directory (default output)
                  --seed N            seed for reproducible output
              -h, --help              show this help
            """);
    }
}
