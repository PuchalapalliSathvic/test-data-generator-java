# Automated Test Data Generation (PoC) — Java

A proof-of-concept that automatically generates realistic, varied, and
privacy-safe test data for a **User Profile** domain model. It outputs both
**JSON** and **CSV** and deliberately injects **invalid records** so QA can
test both happy paths and negative/edge cases.

**No external libraries and no build tool required** — it runs on a plain JDK.

## Why

Hand-crafting test data is slow and rarely covers edge cases. This tool
generates a configurable number of records on demand, mixes in malformed data,
and is reproducible via a seed. All data is synthetic (assembled from built-in
value pools), so it contains no real personal information and is
privacy-compliant by design.

## Domain model: User Profile

| Field      | Type     | Notes                                            |
|------------|----------|--------------------------------------------------|
| user_id    | UUID     | Unique per record                                |
| name       | string   | Full name (blank/garbage in invalid mode)        |
| email      | string   | Valid, or intentionally malformed                |
| phone      | string   | `+1` number, or malformed                        |
| street/city/state/zip | string | US-style address components           |
| age        | int      | 18–90 valid; out-of-range in invalid mode        |
| signup_dt  | ISO-8601 | Last 3 years; invalid dates in invalid mode      |
| _scenario  | string   | `valid` or `invalid` tag for filtering           |

## Requirements

- Java 17 or newer (`java -version` to check). JDK recommended; a JRE works
  too via single-file source mode (see below).

## How to run

### Option A — with a JDK (compile then run)

```bash
bash run.sh                          # default: 50 records, ~20% invalid
bash run.sh -n 500 -i 0.3 --seed 42  # 500 records, 30% invalid, reproducible
bash run.sh -n 100 -o my_output      # custom output directory
```

Or manually:

```bash
javac -d out src/main/java/com/poc/testdata/TestDataGenerator.java
java -cp out com.poc.testdata.TestDataGenerator -n 200 -i 0.25 --seed 7
```

### Option B — Java 11+ single-file mode (no javac needed)

```bash
java src/main/java/com/poc/testdata/TestDataGenerator.java -n 50 -i 0.25 --seed 42
```

## Options

| Flag | Description | Default |
|------|-------------|---------|
| `-n, --count` | Number of records | 50 |
| `-i, --invalid-ratio` | Fraction of invalid records (0.0–1.0) | 0.2 |
| `-o, --outdir` | Output directory | `output` |
| `--seed` | Seed for reproducible runs | none |
| `-h, --help` | Show help | |

## Output

Two files are written to the output directory:

- `users.json` — array of record objects
- `users.csv`  — same records, flat/tabular

Every record carries a `_scenario` field (`valid` / `invalid`) so you can
filter and feed positive vs negative test suites separately.
A ready-made example is committed under `sample_output/`.

## Test scenarios simulated

- **Valid emails** vs malformed (`plainaddress`, `@x.com`, `two@@at.com`, …)
- **Valid phones** vs malformed (`123`, `abc-def`, empty, …)
- **In-range ages** (18–90) vs out-of-range (`-5`, `0`, `150`, `999`)
- **Clean names** vs blank/numeric/injection-style (`<script>…`)
- **Valid timestamps** vs unparseable dates (`not-a-date`, `2099-13-40`)

## How to extend

Swap the domain by replacing `validRecord()` / `invalidRecord()` (e.g. a
product catalog with name, price, stock). The JSON/CSV writers are
schema-agnostic and need no changes.

## Project layout

```
test-data-generator-java/
├── README.md
├── run.sh
├── pom.xml                         # optional Maven build (if you add Faker)
├── sample_output/
│   ├── users.json
│   └── users.csv
└── src/main/java/com/poc/testdata/
    └── TestDataGenerator.java
```
