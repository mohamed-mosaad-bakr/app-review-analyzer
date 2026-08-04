# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Python library/service that analyzes exported app store reviews (Apple App Store / Google
Play) for sentiment and recurring topics, exposed via a Click CLI and a FastAPI service. Both
entry points share the same core pipeline in `analyzer.py`.

## Commands

```bash
# Install (editable, with dev dependencies)
pip install -e ".[dev]"

# Run the full test suite
pytest

# Run a single test file / test
pytest tests/test_topics.py
pytest tests/test_analyzer.py::test_analyze_reviews_summary

# Lint
ruff check .

# Run the CLI against the bundled sample data
app-review-analyzer analyze data/sample_reviews.json --top-n 5
app-review-analyzer analyze data/sample_reviews.json --output summary.json

# Run the API locally
uvicorn app_review_analyzer.api:app --reload
```

There is no build step — this ships as a `src/`-layout Python package (see
`[tool.setuptools.packages.find]` in `pyproject.toml`), installed with `pip install -e`.

## Architecture

The package lives under `src/app_review_analyzer/`. Data flows through one pipeline regardless
of which entry point (CLI or API) drives it:

```
ingest.load_reviews()  ->  list[Review]  ->  analyzer.analyze_reviews()  ->  AnalysisSummary
                                                   |
                                     sentiment.score_reviews()  +  topics.extract_topics()
```

- **`models.py`** — the only source of truth for shapes. `Review` is the input record
  (validated by Pydantic from JSON/CSV); `AnalysisSummary`, `ReviewSentiment`, and
  `TopicMention` are outputs. Both `cli.py` and `api.py` serialize through these models, so a
  field added here should show up consistently in CLI JSON output and API responses without
  further plumbing.
- **`ingest.py`** — loads reviews from a `.json` file (list of review objects) or `.csv` file
  (headers matching `Review` field names). Extend this if a new input format is needed;
  extension dispatch is a simple suffix check.
- **`sentiment.py`** — scores each review with VADER (`vaderSentiment`) on the concatenated
  title+body text, using fixed thresholds (`compound >= 0.05` positive,
  `<= -0.05` negative, else neutral).
- **`topics.py`** — extracts candidate topics by counting words (>= 4 chars, stopword-filtered)
  that recur **across distinct reviews**. Each review contributes a given word at most once to
  the count, so one verbose review can't dominate the ranking by itself — this is deliberate;
  don't "simplify" it into a flat word-occurrence count.
- **`analyzer.py`** — the orchestration point: calls `sentiment.score_reviews()` and
  `topics.extract_topics()` and rolls the results into one `AnalysisSummary`. This is the
  function both `cli.py` and `api.py` call — new cross-cutting analysis (e.g. a new summary
  field) belongs here, not duplicated in both entry points.
- **`cli.py`** — Click group; the `analyze` command loads reviews, runs the pipeline, and
  prints/writes the `AnalysisSummary` as JSON via Pydantic's `model_dump_json`.
- **`api.py`** — FastAPI app with `/health`, `/analyze` (JSON body of reviews), and
  `/analyze/upload` (multipart file upload, reuses `ingest.load_reviews` via a temp file).

## Conventions

- `src/` layout: import as `app_review_analyzer.*`, not from the repo root.
- Every public function is typed; models are Pydantic `BaseModel`/`Enum`, not plain dicts.
- Sample review data for manual testing and as a fixture source lives in
  `data/sample_reviews.json`; tests reference it directly rather than duplicating fixtures.
- `Review.date` is intentionally imported as `date_` in `models.py` (`from datetime import date
  as date_`) to avoid the field name `date` shadowing the `date` type when Pydantic resolves
  string annotations (`from __future__ import annotations` is active file-wide) — don't rename
  the import back without checking model construction still works.
- `ruff` has a per-file ignore for `B008` on `api.py` only, because FastAPI's
  `File(...)`/`Depends(...)` defaults are the framework's idiomatic dependency-injection
  pattern, not a real mutable-default bug.
