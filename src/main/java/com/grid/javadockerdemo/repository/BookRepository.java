package com.grid.javadockerdemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.grid.javadockerdemo.model.Book;

public interface BookRepository extends JpaRepository<Book, String> {

}
