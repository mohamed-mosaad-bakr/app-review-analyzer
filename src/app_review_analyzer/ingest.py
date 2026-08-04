from __future__ import annotations

import csv
import json
from pathlib import Path

from .models import Review


def load_reviews(path: str | Path) -> list[Review]:
    """Load reviews from a .json or .csv export."""
    path = Path(path)
    suffix = path.suffix.lower()
    if suffix == ".json":
        return _load_json(path)
    if suffix == ".csv":
        return _load_csv(path)
    raise ValueError(f"Unsupported review file type: {suffix or '(none)'}")


def _load_json(path: Path) -> list[Review]:
    data = json.loads(path.read_text(encoding="utf-8"))
    return [Review.model_validate(item) for item in data]


def _load_csv(path: Path) -> list[Review]:
    with path.open(newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        return [Review.model_validate(row) for row in reader]
