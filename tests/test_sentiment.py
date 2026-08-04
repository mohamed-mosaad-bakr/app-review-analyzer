from app_review_analyzer.models import Platform, Review, SentimentLabel
from app_review_analyzer.sentiment import score_review


def _review(body: str) -> Review:
    return Review(id="r1", app_id="a1", platform=Platform.APP_STORE, rating=3, body=body)


def test_positive_review_scores_positive() -> None:
    result = score_review(_review("I absolutely love this app, it is wonderful and fast."))
    assert result.label == SentimentLabel.POSITIVE
    assert result.compound > 0


def test_negative_review_scores_negative() -> None:
    result = score_review(_review("This app is terrible and crashes constantly, I hate it."))
    assert result.label == SentimentLabel.NEGATIVE
    assert result.compound < 0
