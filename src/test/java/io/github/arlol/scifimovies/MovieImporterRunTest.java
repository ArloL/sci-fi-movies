package io.github.arlol.scifimovies;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MovieImporterRunTest {

	private static final Pattern NUMERIC_ID = Pattern
			.compile("VALUES\\([0-9]+,");

	/**
	 * The last entry has no title so it exercises the filter, the middle one
	 * already exists in data.sql so it exercises the update branch.
	 */
	private static final String GUIDE = """
			<div class="article_movie_title">
				<h2>
					<a href="https://example.org/m/2001">2001: A Space Odyssey</a>
					<span class="start-year">(1968)</span>
					<span class="tMeterScore">92%</span>
				</h2>
			</div>
			<div class="article_movie_title">
				<h2>
					<a href="https://example.org/m/the_endless">The Endless</a>
					<span class="start-year">(2017)</span>
					<span class="tMeterScore">88%</span>
				</h2>
			</div>
			<div class="article_movie_title">
				<h2>
					<a href="https://example.org/m/unnamed"></a>
					<span class="start-year">(1999)</span>
					<span class="tMeterScore">- -</span>
				</h2>
			</div>
			""";

	@Autowired
	private MovieRepository movieRepository;

	@Autowired
	private JdbcTemplate template;

	@TempDir
	private Path tempDir;

	private void run(Path output) throws Exception {
		Document document = Jsoup.parse(GUIDE);
		new MovieImporter(movieRepository, template, () -> document, output)
				.run();
	}

	@Test
	void run_savesTheScrapedMovies() throws Exception {
		run(tempDir.resolve("saved.sql"));

		assertThat(
				movieRepository
						.findByTitleAndYear("2001: A Space Odyssey", 1968)
		).hasValueSatisfying(movie -> {
			assertThat(movie.getUrl()).isEqualTo("https://example.org/m/2001");
			assertThat(movie.getTomatoes()).isEqualTo(92);
		});
	}

	@Test
	void run_skipsTitlelessEntries() throws Exception {
		run(tempDir.resolve("skipped.sql"));

		assertThat(movieRepository.findByTitleAndYear("", 1999)).isEmpty();
	}

	@Test
	void run_updatesAMovieThatIsAlreadyStored() throws Exception {
		long before = movieRepository.count();

		run(tempDir.resolve("updated.sql"));

		assertThat(movieRepository.findByTitleAndYear("The Endless", 2017))
				.hasValueSatisfying(
						movie -> assertThat(movie.getTomatoes()).isEqualTo(88)
				);
		assertThat(movieRepository.count()).isLessThanOrEqualTo(before + 1);
	}

	@Test
	void run_writesInsertsWithADefaultId() throws Exception {
		Path output = tempDir.resolve("data.sql");

		run(output);

		List<String> lines = Files.readAllLines(output);
		assertThat(lines).isNotEmpty()
				.allMatch(line -> line.startsWith("INSERT"))
				.noneMatch(line -> NUMERIC_ID.matcher(line).find())
				.anyMatch(
						line -> line.contains("VALUES(default,")
								&& line.contains("2001: A Space Odyssey")
				);
	}

}
