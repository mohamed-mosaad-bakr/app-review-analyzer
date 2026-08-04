from fastapi.testclient import TestClient

from app_review_analyzer.api import app

client = TestClient(app)


def test_health() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_analyze_endpoint() -> None:
    payload = [
        {
            "id": "r1",
            "app_id": "a1",
            "platform": "app_store",
            "rating": 5,
            "body": "I love this app, it is great.",
        },
        {
            "id": "r2",
            "app_id": "a1",
            "platform": "google_play",
            "rating": 1,
            "body": "I hate this app, it keeps crashing.",
        },
    ]
    response = client.post("/analyze", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["total_reviews"] == 2
