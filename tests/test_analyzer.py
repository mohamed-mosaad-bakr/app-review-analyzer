from pathlib import Path

from app_review_analyzer.analyzer import analyze_reviews
from app_review_analyzer.ingest import load_reviews

SAMPLE = Path(__file__).resolve().parent.parent / "data" / "sample_reviews.json"


def test_analyze_reviews_summary() -> None:
    reviews = load_reviews(SAMPLE)
    summary = analyze_reviews(reviews)
    assert summary.total_reviews == 5
    assert summary.average_rating == 3.2
    assert sum(summary.sentiment_breakdown.values()) == 5
    assert len(summary.review_sentiments) == 5


def test_analyze_empty_reviews_returns_zeroed_summary() -> None:
    summary = analyze_reviews([])
    assert summary.total_reviews == 0
    assert summary.average_rating == 0.0
    assert summary.top_topics == []
