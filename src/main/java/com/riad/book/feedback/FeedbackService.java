package com.riad.book.feedback;
import com.riad.book.book.Book;
import com.riad.book.book.BookRepository;
import com.riad.book.common.PageResponse;
import com.riad.book.exception.OperationNotPermittedException;
import com.riad.book.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final BookRepository bookRepository;
    private final FeedBackMapper feedBackMapper;
    private final FeedBackRepository feedBackRepository;

    public Integer save(FeedbackRequest request, Authentication connectedUser) {
        Book book = bookRepository.findById(request.bookId()).orElseThrow(()-> new EntityNotFoundException("this book does not exist"));

        if(book.isArchived() || !book.isShareable()){
            throw new OperationNotPermittedException("this book is archived or not shareable");
        }

        User user = ((User) connectedUser.getPrincipal());

        if(Objects.equals(book.getOwner().getId() , user.getId())){
            throw new OperationNotPermittedException("you cant give a feedback for your own book");
        }

        FeedBack feedBack = feedBackMapper.toFeedBack(request);

        return feedBackRepository.save(feedBack).getId();
    }

    public PageResponse<FeedBackResponse> findAllFeedBacksByBookId(Integer bookId, int page, int size , Authentication connectedUser) {
        Pageable pageable = PageRequest.of(page , size);

        User user = ((User) connectedUser.getPrincipal());

        Page<FeedBack> feedbacks = feedBackRepository.findAllFeedBacksByBookId(pageable,bookId);

        List<FeedBackResponse> response = feedbacks.stream()
                .map(f -> feedBackMapper.toFeedBackResponse(f, user.getId()))
                .toList();

        return new PageResponse<>(
                response,
                feedbacks.getNumber(),
                feedbacks.getSize(),
                feedbacks.getTotalElements(),
                feedbacks.getTotalPages(),
                feedbacks.isFirst(),
                feedbacks.isLast()
                );
    }
}
