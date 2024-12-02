package com.riad.book.book;


import com.riad.book.exception.OperationNotPermittedException;
import com.riad.book.file.FileStorageService;
import com.riad.book.history.BookTransactionHistoryRepository;
import com.riad.book.common.PageResponse;
import com.riad.book.history.BookTransactionHistory;
import com.riad.book.history.BorrowedBookResponse;
import com.riad.book.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;


@RequiredArgsConstructor
@Service
public class BookService {

    private final BookRepository repo ;
    private final BookMapper bookMapper;
    private final BookTransactionHistoryRepository bookTransactionHistoryRepo ;

    private final FileStorageService fileStorageService;

    public Integer save(BookRequest req , Authentication auth){
        User user = ((User) auth.getPrincipal());
        Book book = bookMapper.toBook(req);

        book.setOwner(user);
        return repo.save(book).getId();
    }

    public BookResponse findById(Integer bookId) {
        return repo.findById(bookId)
                .map(bookMapper::toBookResponse)
                .orElseThrow(() -> new EntityNotFoundException("Book not found"));
    }

    public PageResponse<BookResponse> findAllBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size , Sort.by("createdDate").descending());

        Page<Book> books = repo.findAllDisplayableBooks(pageable,user.getId());
        List<BookResponse> bookResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BookResponse> findAllBooksByOwner(int page, int size, Authentication connectedUser) {
        User user =((User) connectedUser.getPrincipal());

        Pageable pageable = PageRequest.of(page, size , Sort.by("createdDate").descending());

        Page<Book> books= repo.findAllBooksByOwner(pageable,user.getId());

        List<BookResponse> bookResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();

        return new PageResponse<>(
                bookResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable= PageRequest.of(page, size , Sort.by("createdDate").descending());

        Page<BookTransactionHistory> allBorrowedBooks = bookTransactionHistoryRepo.findAllBorrowedBooks(pageable,user.getId());

        List<BorrowedBookResponse> borrowedBookResponse = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();

        return new PageResponse<>(
                borrowedBookResponse,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable= PageRequest.of(page, size , Sort.by("createdDate").descending());

        Page<BookTransactionHistory> allReturnedBooks = bookTransactionHistoryRepo.findAllReturnedBooks(pageable,user.getId());

        List<BorrowedBookResponse> borrowedBookResponse = allReturnedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();

        return new PageResponse<>(
                borrowedBookResponse,
                allReturnedBooks.getNumber(),
                allReturnedBooks.getSize(),
                allReturnedBooks.getTotalElements(),
                allReturnedBooks.getTotalPages(),
                allReturnedBooks.isFirst(),
                allReturnedBooks.isLast()
        );
    }

    public Integer updateShareable(Integer bookId, Authentication connectedUser) {

        Book book = repo.findById(bookId).orElseThrow(() -> new EntityNotFoundException("Book not found"));
        User user = ((User) connectedUser.getPrincipal());

        if(!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot update books shareable status");
        }

        book.setShareable(!book.isShareable());
        repo.save(book);
        return bookId;
    }

    public Integer updateArchived(Integer bookId, Authentication connectedUser) {
        Book book = repo.findById(bookId).orElseThrow(() -> new EntityNotFoundException("Book not found"));
        User user = ((User) connectedUser.getPrincipal());

        if(!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot update books shareable status");
        }

        book.setArchived(!book.isArchived());
        repo.save(book);
        return bookId;
    }

    public Integer borrowBook(Integer bookId, Authentication connectedUser) {
        Book book = repo.findById(bookId).orElseThrow(() -> new EntityNotFoundException("Book not found"));

        if(book.isArchived() || !book.isShareable()){
            throw new OperationNotPermittedException("not permitted");
        }

        User user = ((User) connectedUser.getPrincipal());

        if(Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You are the owner of this book");
        }

        final boolean isAlreadyBorrowed = bookTransactionHistoryRepo.isAlreadyBorrowed(bookId,user.getId());

        if(isAlreadyBorrowed){
            throw new OperationNotPermittedException("already borrowed");
        }

        BookTransactionHistory bookTransactionHistory = BookTransactionHistory.builder()
                .user(user)
                .book(book)
                .returned(false)
                .returnApproved(false)
                .build();

        return bookTransactionHistoryRepo.save(bookTransactionHistory).getId();
    }

    public Integer returnBook(Integer bookId, Authentication connectedUser) {
        Book book = repo.findById(bookId).orElseThrow(()-> new EntityNotFoundException("Book not found"));
        User user = ((User) connectedUser.getPrincipal());

        if(book.isArchived() || !book.isShareable()){
            throw new OperationNotPermittedException("not permitted");
        }
        if(Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cant return your own book");
        }

        BookTransactionHistory bookTransactionHistory = bookTransactionHistoryRepo.findByBookIdAndUserId(bookId,user.getId())
                .orElseThrow(()-> new OperationNotPermittedException("you didnt borrow this book"));

        bookTransactionHistory.setReturned(true);

        return bookTransactionHistoryRepo.save(bookTransactionHistory).getId();
    }

    public Integer approveReturnBorrowBook(Integer bookId, Authentication connectedUser) {
        Book book = repo.findById(bookId).orElseThrow(()-> new EntityNotFoundException("Book not found"));
        User user = ((User) connectedUser.getPrincipal());

        if(book.isArchived() || !book.isShareable()){
            throw new OperationNotPermittedException("not permitted");
        }
        if(Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You are the return your own book");
        }
        BookTransactionHistory bookTransactionHistory = bookTransactionHistoryRepo.findByBookIdAndOwnerId(bookId,user.getId())
                .orElseThrow(()-> new OperationNotPermittedException("The book is not returned yet"));

        bookTransactionHistory.setReturnApproved(true);

        return bookTransactionHistoryRepo.save(bookTransactionHistory).getId();
    }

    public void uploadBookCoverPicture(MultipartFile file, Authentication connectedUser, Integer bookId) {
        Book book = repo.findById(bookId)
                .orElseThrow(()-> new EntityNotFoundException("Book not found"));

        User user = ((User) connectedUser.getPrincipal());

        var bookCover = fileStorageService.saveFile(file,user.getId());

        book.setBookCover(bookCover);
        repo.save(book);

    }
}
