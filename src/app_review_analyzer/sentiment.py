from __future__ import annotations

from vaderSentiment.vaderSentiment import SentimentIntensityAnalyzer

from .models import Review, ReviewSentiment, SentimentLabel

_analyzer = SentimentIntensityAnalyzer()

_POSITIVE_THRESHOLD = 0.05
_NEGATIVE_THRESHOLD = -0.05


def score_review(review: Review) -> ReviewSentiment:
    text = f"{review.title or ''} {review.body}".strip()
    compound = _analyzer.polarity_scores(text)["compound"]
    if compound >= _POSITIVE_THRESHOLD:
        label = SentimentLabel.POSITIVE
    elif compound <= _NEGATIVE_THRESHOLD:
        label = SentimentLabel.NEGATIVE
    else:
        label = SentimentLabel.NEUTRAL
    return ReviewSentiment(review_id=review.id, compound=compound, label=label)


def score_reviews(reviews: list[Review]) -> list[ReviewSentiment]:
    return [score_review(review) for review in reviews]
