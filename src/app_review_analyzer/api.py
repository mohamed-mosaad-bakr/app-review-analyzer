from __future__ import annotations

import tempfile
from pathlib import Path

from fastapi import FastAPI, File, HTTPException, UploadFile

from .analyzer import analyze_reviews
from .ingest import load_reviews
from .models import AnalysisSummary, Review

app = FastAPI(title="App Review Analyzer", version="0.1.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/analyze", response_model=AnalysisSummary)
def analyze(reviews: list[Review]) -> AnalysisSummary:
    return analyze_reviews(reviews)


@app.post("/analyze/upload", response_model=AnalysisSummary)
async def analyze_upload(file: UploadFile = File(...)) -> AnalysisSummary:
    suffix = Path(file.filename or "").suffix or ".json"
    contents = await file.read()
    with tempfile.NamedTemporaryFile(suffix=suffix) as tmp:
        tmp.write(contents)
        tmp.flush()
        try:
            reviews = load_reviews(tmp.name)
        except ValueError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc
    return analyze_reviews(reviews)
