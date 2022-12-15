package io.github.arlol.scifimovies;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface MovieRepository extends CrudRepository<Movie, Long> {

	Optional<Movie> findByTitleAndYear(String title, int year);

}
