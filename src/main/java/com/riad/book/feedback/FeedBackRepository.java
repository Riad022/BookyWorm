package com.riad.book.feedback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedBackRepository extends JpaRepository<FeedBack,Integer> {

    @Query("""
            SELECT fb
            FROM FeedBack fb
            WHERE fb.book.id= :bookId
            """)
    Page<FeedBack> findAllFeedBacksByBookId(Pageable pageable, Integer bookId);
}
