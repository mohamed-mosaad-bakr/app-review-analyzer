from app_review_analyzer.models import Platform, Review
from app_review_analyzer.topics import extract_topics


def _review(review_id: str, body: str) -> Review:
    return Review(id=review_id, app_id="a1", platform=Platform.APP_STORE, rating=3, body=body)


def test_topic_counts_distinct_reviews_not_word_occurrences() -> None:
    reviews = [
        _review("r1", "dashboard dashboard dashboard is great"),
        _review("r2", "the dashboard crashed on me"),
        _review("r3", "login failed again"),
    ]
    topics = extract_topics(reviews, top_n=5)
    dashboard = next(t for t in topics if t.topic == "dashboard")
    assert dashboard.count == 2
    assert set(dashboard.example_review_ids) == {"r1", "r2"}


def test_stopwords_are_excluded() -> None:
    reviews = [_review("r1", "this app is the best app that I have ever used")]
    topics = extract_topics(reviews, top_n=10)
    assert all(t.topic not in {"this", "that", "have", "the"} for t in topics)
