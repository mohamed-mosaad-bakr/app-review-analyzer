# App Review Analyzer

Analyzes exported app store reviews (Apple App Store / Google Play) for sentiment and
recurring topics, via a CLI and a FastAPI service.

## Install

```bash
pip install -e ".[dev]"
```

## CLI usage

```bash
app-review-analyzer analyze data/sample_reviews.json
app-review-analyzer analyze data/sample_reviews.json --top-n 5 --output summary.json
```

Input files are `.json` (a list of review objects) or `.csv` (with matching column
headers). See `data/sample_reviews.json` for the expected review shape.

## API usage

```bash
uvicorn app_review_analyzer.api:app --reload
```

- `GET /health`
- `POST /analyze` — JSON body: a list of review objects
- `POST /analyze/upload` — multipart file upload (`.json` or `.csv`)

## Tests

```bash
pytest
```
