package io.github.arlol.scifimovies;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieRepository extends CrudRepository<Movie, Long> {

    Optional<Movie> findByTitleAndYear(String title, int year);

}
