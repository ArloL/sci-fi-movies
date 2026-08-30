package io.github.arlol.scifimovies;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Scrapes Rotten Tomatoes and rewrites data.sql. Excluded from the test profile
 * because it needs the network and writes into the source tree.
 */
@Component
@Profile("!test")
public class MovieImporter implements CommandLineRunner {

	private static Logger LOG = LoggerFactory.getLogger(MovieImporter.class);

	@Autowired
	MovieRepository movieRepository;
	@Autowired
	JdbcTemplate template;

	@Override
	public void run(String... args) throws Exception {
		Document doc = Jsoup.connect(
				"https://editorial.rottentomatoes.com/guide/best-sci-fi-movies-of-all-time/"
		).timeout(30_000).get();
		List<Movie> movies = doc.select("div.article_movie_title h2")
				.stream()
				.map(e -> {
					return new Movie(
							extractTitle(e),
							extractYear(e),
							extractUrl(e),
							extractTomatoes(e)
					);
				})
				.filter(m -> m.getTitle().length() > 0)
				.collect(Collectors.toList());
		for (Movie movie : movies) {
			movieRepository
					.findByTitleAndYear(movie.getTitle(), movie.getYear())
					.ifPresent(existing -> movie.setId(existing.getId()));
			movieRepository.save(movie);
		}
		LOG.info("Imported these movies: {}", movies);
		List<String> data = template.queryForStream(
				"SCRIPT SIMPLE NOPASSWORDS NOSETTINGS TABLE movie",
				(resultSet, rowNum) -> {
					return resultSet.getString(1);
				}
		)
				.filter(s -> s.startsWith("INSERT"))
				.map(s -> s.replaceAll("VALUES\\([0-9]+,", "VALUES(default,"))
				.toList();
		Files.write(Path.of("./src/main/resources/data.sql"), data);
	}

	/**
	 * The scrape is meaningless if Rotten Tomatoes drops one of these elements,
	 * so fail loudly rather than dereferencing a null from selectFirst.
	 */
	private Element required(Element e, String cssQuery) {
		return Objects.requireNonNull(
				e.selectFirst(cssQuery),
				() -> "no element matched " + cssQuery
		);
	}

	private String extractUrl(Element e) {
		return required(e, "a").attr("href");
	}

	private int extractTomatoes(Element e) {
		String tomatoScore = required(e, ".tMeterScore").text()
				.trim()
				.replace("%", "");
		if (tomatoScore.length() == 0) {
			return 0;
		}
		if (tomatoScore.equalsIgnoreCase("- -")) {
			return 0;
		}
		return Integer.parseInt(tomatoScore);
	}

	private String extractTitle(Element e) {
		return required(e, "a").text();
	}

	private int extractYear(Element e) {
		String year = required(e, "span.start-year").text();
		if (year.length() == 2) {
			year = "0";
		} else {
			year = year.substring(1, 5);
		}
		return Integer.parseInt(year);
	}

}
