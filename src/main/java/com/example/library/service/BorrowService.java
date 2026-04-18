package com.example.library.service;

import com.example.library.dto.BorrowResponse;
import com.example.library.exception.*;
import com.example.library.model.*;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BorrowService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    public BorrowService(BorrowRecordRepository borrowRecordRepository,
                         BookRepository bookRepository,
                         MemberRepository memberRepository) {
        this.borrowRecordRepository = borrowRecordRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * Borrow a book for a member.
     * Business rules:
     * 1. Member must exist and be active
     * 2. Book must exist and have available copies
     * 3. Member must not exceed their borrowing limit
     * 4. Member must not already have this book borrowed
     */
    @Transactional
    public BorrowResponse borrowBook(Long bookId, Long memberId) {
        // 1. Validate member
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        if (!member.isActive()) {
            throw new IllegalStateException("Inactive members cannot borrow books");
        }

        // 2. Validate book
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        if (book.getAvailableCopies() <= 0) {
            throw new BookNotAvailableException(book.getTitle());
        }

        // 3. Check borrowing limit
        int activeBorrows = borrowRecordRepository.countActiveBorrowsByMember(memberId);
        int maxBooks = member.getMembershipType().getMaxBooks();

        if (activeBorrows >= maxBooks) {
            throw new BorrowLimitExceededException(member.getName(), maxBooks);
        }

        // 4. Check duplicate borrow
        if (borrowRecordRepository.existsByBookIdAndMemberIdAndStatus(
                bookId, memberId, BorrowStatus.BORROWED)) {
            throw new IllegalStateException("Member already has this book borrowed");
        }

        // Create borrow record
        BorrowRecord record = new BorrowRecord(book, member);
        borrowRecordRepository.save(record);

        // Decrease available copies
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        return BorrowResponse.fromEntity(record);
    }

    /**
     * Return a borrowed book.
     * Calculates any late fees and updates availability.
     */
    @Transactional
    public BorrowResponse returnBook(Long borrowRecordId) {
        BorrowRecord record = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(() -> new IllegalStateException("Borrow record not found: " + borrowRecordId));

        if (record.getStatus() == BorrowStatus.RETURNED) {
            throw new IllegalStateException("This book has already been returned");
        }

        record.setReturnDate(LocalDate.now());
        record.setStatus(BorrowStatus.RETURNED);
        borrowRecordRepository.save(record);

        // Increase available copies
        Book book = record.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        return BorrowResponse.fromEntity(record);
    }

    /**
     * Get all borrow records for a specific member.
     */
    public List<BorrowResponse> getMemberBorrowHistory(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException(memberId);
        }
        return borrowRecordRepository.findByMemberId(memberId)
                .stream()
                .map(BorrowResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all currently overdue records.
     */
    public List<BorrowResponse> getOverdueRecords() {
        return borrowRecordRepository.findOverdueRecords(LocalDate.now())
                .stream()
                .map(BorrowResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get active borrows for a member.
     */
    public List<BorrowResponse> getActiveBorrows(Long memberId) {
        return borrowRecordRepository.findByMemberIdAndStatus(memberId, BorrowStatus.BORROWED)
                .stream()
                .map(BorrowResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
