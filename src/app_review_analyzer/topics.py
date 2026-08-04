from __future__ import annotations

import re
from collections import Counter, defaultdict

from .models import Review, TopicMention

_WORD_RE = re.compile(r"[a-zA-Z']+")

# Common English stopwords plus generic review filler words that would
# otherwise dominate topic frequency counts without carrying any signal.
STOPWORDS = {
    "a", "about", "after", "again", "all", "also", "am", "an", "and", "any",
    "are", "as", "at", "be", "because", "been", "before", "being", "but",
    "by", "can", "cant", "could", "did", "didnt", "do", "does", "doesnt",
    "doing", "dont", "down", "even", "ever", "every", "for", "from", "get",
    "gets", "getting", "had", "has", "have", "having", "he", "her", "here",
    "hers", "herself", "him", "himself", "his", "how", "i", "if", "im",
    "into", "is", "isnt", "it", "its", "itself", "just", "me", "more",
    "most", "my", "myself", "no", "nor", "not", "now", "of", "off", "on",
    "once", "only", "or", "other", "our", "ours", "ourselves", "out",
    "over", "own", "really", "same", "she", "should", "so", "some", "still",
    "such", "than", "that", "the", "their", "theirs", "them", "themselves",
    "then", "there", "these", "they", "this", "those", "through", "to",
    "too", "under", "until", "up", "use", "used", "very", "was", "wasnt",
    "we", "were", "what", "when", "where", "which", "while", "who", "whom",
    "why", "will", "with", "wont", "would", "you", "your", "yours",
    "yourself", "yourselves", "app", "apps",
}


def extract_topics(
    reviews: list[Review], top_n: int = 10, min_word_length: int = 4
) -> list[TopicMention]:
    """Rank words that recur across distinct reviews as candidate topics.

    Each review contributes a word at most once so a single long review
    repeating a word can't dominate the ranking.
    """
    counts: Counter[str] = Counter()
    examples: defaultdict[str, list[str]] = defaultdict(list)
    for review in reviews:
        text = f"{review.title or ''} {review.body}".lower()
        words = {
            word
            for word in _WORD_RE.findall(text)
            if len(word) >= min_word_length and word not in STOPWORDS
        }
        for word in words:
            counts[word] += 1
            if len(examples[word]) < 3:
                examples[word].append(review.id)

    return [
        TopicMention(topic=word, count=count, example_review_ids=examples[word])
        for word, count in counts.most_common(top_n)
    ]
