from __future__ import annotations

from pathlib import Path

import click

from .analyzer import analyze_reviews
from .ingest import load_reviews


@click.group()
def main() -> None:
    """Analyze app store review exports for sentiment and recurring topics."""


@main.command()
@click.argument("input_path", type=click.Path(exists=True, dir_okay=False))
@click.option(
    "--output",
    "-o",
    type=click.Path(dir_okay=False),
    help="Write the JSON summary to a file instead of stdout.",
)
@click.option("--top-n", default=10, show_default=True, help="Number of top topics to include.")
def analyze(input_path: str, output: str | None, top_n: int) -> None:
    """Analyze reviews from INPUT_PATH (.json or .csv)."""
    reviews = load_reviews(input_path)
    summary = analyze_reviews(reviews, top_n_topics=top_n)
    payload = summary.model_dump_json(indent=2)
    if output:
        Path(output).write_text(payload, encoding="utf-8")
        click.echo(f"Wrote summary to {output}")
    else:
        click.echo(payload)


if __name__ == "__main__":
    main()
