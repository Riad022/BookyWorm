package com.riad.book.book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book,Integer> {

    @Query("""
            SELECT book
            FROM Book book
            WHERE book.archived = false
            AND book.shareable = true
            AND book.owner.id != :id
            
            """)
    Page<Book> findAllDisplayableBooks(Pageable pageable, Integer id);



    @Query("""
            SELECT book
            FROM Book book
            WHERE book.owner.id = :id
            """)
    Page<Book> findAllBooksByOwner(Pageable pageable, Integer id);


}
