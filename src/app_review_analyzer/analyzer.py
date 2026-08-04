from __future__ import annotations

from .models import AnalysisSummary, Review, SentimentLabel
from .sentiment import score_reviews
from .topics import extract_topics


def analyze_reviews(reviews: list[Review], top_n_topics: int = 10) -> AnalysisSummary:
    """Run sentiment scoring and topic extraction and roll them into a summary."""
    if not reviews:
        return AnalysisSummary(
            total_reviews=0,
            average_rating=0.0,
            sentiment_breakdown={label: 0 for label in SentimentLabel},
            top_topics=[],
            review_sentiments=[],
        )

    sentiments = score_reviews(reviews)
    breakdown = {label: 0 for label in SentimentLabel}
    for sentiment in sentiments:
        breakdown[sentiment.label] += 1

    average_rating = sum(review.rating for review in reviews) / len(reviews)

    return AnalysisSummary(
        total_reviews=len(reviews),
        average_rating=round(average_rating, 2),
        sentiment_breakdown=breakdown,
        top_topics=extract_topics(reviews, top_n=top_n_topics),
        review_sentiments=sentiments,
    )
