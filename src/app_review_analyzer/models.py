from __future__ import annotations

from datetime import date as date_
from enum import Enum

from pydantic import BaseModel, Field


class Platform(str, Enum):
    APP_STORE = "app_store"
    GOOGLE_PLAY = "google_play"
    UNKNOWN = "unknown"


class Review(BaseModel):
    id: str
    app_id: str
    platform: Platform = Platform.UNKNOWN
    author: str | None = None
    rating: int = Field(ge=1, le=5)
    title: str | None = None
    body: str
    date: date_ | None = None
    version: str | None = None


class SentimentLabel(str, Enum):
    POSITIVE = "positive"
    NEUTRAL = "neutral"
    NEGATIVE = "negative"


class ReviewSentiment(BaseModel):
    review_id: str
    compound: float
    label: SentimentLabel


class TopicMention(BaseModel):
    topic: str
    count: int
    example_review_ids: list[str]


class AnalysisSummary(BaseModel):
    total_reviews: int
    average_rating: float
    sentiment_breakdown: dict[SentimentLabel, int]
    top_topics: list[TopicMention]
    review_sentiments: list[ReviewSentiment]
