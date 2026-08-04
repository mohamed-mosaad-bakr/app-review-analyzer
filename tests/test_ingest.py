from pathlib import Path

from app_review_analyzer.ingest import load_reviews

SAMPLE = Path(__file__).resolve().parent.parent / "data" / "sample_reviews.json"


def test_load_json_reviews() -> None:
    reviews = load_reviews(SAMPLE)
    assert len(reviews) == 5
    assert reviews[0].id == "r1"
    assert reviews[0].rating == 5


def test_load_rejects_unsupported_extension(tmp_path: Path) -> None:
    bad_file = tmp_path / "reviews.txt"
    bad_file.write_text("not a real export")
    try:
        load_reviews(bad_file)
        raised = False
    except ValueError:
        raised = True
    assert raised
