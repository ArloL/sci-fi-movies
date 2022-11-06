package io.github.arlol.scifimovies;

import org.springframework.data.annotation.Id;

public class Movie {

	@Id
	private long id;
	private String title;
	private int year;
	private String url;
	private int tomatoes;

	public Movie() {
	}

	public Movie(String title, int year, String url, int tomatoes) {
		this.id = -1;
		this.title = title;
		this.year = year;
		this.url = url;
		this.tomatoes = tomatoes;
	}

	public Movie(long id, String title, int year, String url, int tomatoes) {
		this.id = id;
		this.title = title;
		this.year = year;
		this.url = url;
		this.tomatoes = tomatoes;
	}

	@Override
	public String toString() {
		return "Movie [title=" + title + ", year=" + year + "]";
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getTomatoes() {
		return tomatoes;
	}

	public void setTomatoes(int tomatoes) {
		this.tomatoes = tomatoes;
	}

}
