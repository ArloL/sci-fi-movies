package io.github.arlol.scifimovies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

class MovieImporterTest {

	private final MovieImporter importer = new MovieImporter(null, null);

	/**
	 * MovieImporter has two constructors, so Spring needs the @Autowired hint
	 * to pick the production one.
	 */
	@Test
	void springCanStillBuildTheBean() {
		new ApplicationContextRunner()
				.withBean(
						MovieRepository.class,
						() -> Mockito.mock(MovieRepository.class)
				)
				.withBean(
						JdbcTemplate.class,
						() -> Mockito.mock(JdbcTemplate.class)
				)
				.withUserConfiguration(MovieImporter.class)
				.run(
						context -> assertThat(context)
								.hasSingleBean(MovieImporter.class)
				);
	}

	/**
	 * Mirrors the markup Rotten Tomatoes puts inside div.meta-data-wrapper.
	 */
	private static Element title(String year, String tomatoes) {
		return Jsoup
				.parseBodyFragment(
						"""
								<div class="meta-data-wrapper">
									<div class="meta-title-wrapper">
										<a class="meta-title" href="https://example.org/m/2001">2001: A Space Odyssey</a>
										<span class="meta-year">%s</span>
									</div>
									<div class="meta-scores-wrapper">
										<span class="tMeterScore">%s</span>
										<span class="tMeterScore">50%%</span>
									</div>
								</div>
								"""
								.formatted(year, tomatoes)
				)
				.body()
				.child(0);
	}

	@Test
	void extractTitle_readsTheAnchorText() {
		assertThat(importer.extractTitle(title("(1968)", "92%")))
				.isEqualTo("2001: A Space Odyssey");
	}

	@Test
	void extractTitle_replacesEnDashesWithHyphens() {
		Element element = title("(1977)", "93%");
		element.selectFirst("a")
				.text("Star Wars: Episode IV \u2013 A New Hope");
		assertThat(importer.extractTitle(element))
				.isEqualTo("Star Wars: Episode IV - A New Hope");
	}

	@Test
	void extractUrl_readsTheAnchorHref() {
		assertThat(importer.extractUrl(title("(1968)", "92%")))
				.isEqualTo("https://example.org/m/2001");
	}

	@Test
	void extractYear_stripsTheParentheses() {
		assertThat(importer.extractYear(title("(1968)", "92%")))
				.isEqualTo(1968);
	}

	@Test
	void extractYear_isZeroWhenTheParenthesesAreEmpty() {
		assertThat(importer.extractYear(title("()", "92%"))).isZero();
	}

	@Test
	void extractTomatoes_stripsThePercentSign() {
		assertThat(importer.extractTomatoes(title("(1968)", "92%")))
				.isEqualTo(92);
	}

	@Test
	void extractTomatoes_isZeroWhenTheScoreIsMissing() {
		assertThat(importer.extractTomatoes(title("(1968)", ""))).isZero();
	}

	@Test
	void extractTomatoes_isZeroForTheDashPlaceholder() {
		assertThat(importer.extractTomatoes(title("(1968)", "- -"))).isZero();
	}

	@Test
	void extractTomatoes_isZeroForTheEnDashPlaceholder() {
		assertThat(importer.extractTomatoes(title("(1968)", "\u2013 \u2013")))
				.isZero();
	}

	@Test
	void required_throwsWhenNothingMatches() {
		Element element = title("(1968)", "92%");
		assertThatNullPointerException()
				.isThrownBy(() -> importer.required(element, "span.missing"))
				.withMessage("no element matched span.missing");
	}

}
