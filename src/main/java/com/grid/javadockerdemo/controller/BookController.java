package com.grid.javadockerdemo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.grid.javadockerdemo.exception.BookNotFoundException;
import com.grid.javadockerdemo.model.Book;
import com.grid.javadockerdemo.repository.BookRepository;

@RestController
public class BookController {

	private final BookRepository repository;
		
	BookController(BookRepository repository) {
		this.repository = repository;
	}
	
	@GetMapping("/api/v1/books")
	List<Book> all() {
		return repository.findAll();
	}
	
	@GetMapping("/api/v1/book/{isbn}")
	Book one(@PathVariable String isbn) {
		return repository.findById(isbn)
	      .orElseThrow(() -> new BookNotFoundException(isbn));
	}
	
	@PostMapping("/api/v1/books")
	Book newBook(@RequestBody Book newBook) {
		return repository.save(newBook);
	}
	
	@DeleteMapping("/api/v1/book/{isbn}")
	void deleteBook(@PathVariable String isbn) {
		repository.deleteById(isbn);
	}
}
