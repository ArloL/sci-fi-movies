package io.github.arlol.scifimovies;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SciFiMoviesApplication implements CommandLineRunner {

	private static Logger LOG = LoggerFactory.getLogger("SampleLogger");

	public static void main(String[] args) {
		SpringApplication.run(SciFiMoviesApplication.class, args);
	}

	@Autowired
	MovieRepository movieRepository;

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
	}

	private String extractUrl(Element e) {
		return e.selectFirst("a").attr("href");
	}

	private int extractTomatoes(Element e) {
		String tomatoScore = e.selectFirst(".tMeterScore").text();
		if (tomatoScore.length() == 0) {
			return 0;
		}
		if (tomatoScore.equalsIgnoreCase("--")) {
			return 0;
		}
		return Integer.valueOf(tomatoScore.substring(0, 2));
	}

	private String extractTitle(Element e) {
		return e.selectFirst("a").text();
	}

	private int extractYear(Element e) {
		String year = e.selectFirst("span.start-year").text();
		if (year.length() == 2) {
			year = "0";
		} else {
			year = year.substring(1, 5);
		}
		return Integer.valueOf(year);
	}

}
