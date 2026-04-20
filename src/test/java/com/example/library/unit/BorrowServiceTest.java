package com.example.library.unit;

import com.example.library.dto.BorrowResponse;
import com.example.library.exception.*;
import com.example.library.model.*;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.MemberRepository;
import com.example.library.service.BookService;
import com.example.library.service.BorrowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UNIT TEST - Service Layer
 */
@ExtendWith(MockitoExtension.class)
class BorrowServiceTest {

    @Mock
    private BorrowRecordRepository borrowRecordRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private BorrowService borrowService;

    private Book sampleBook;
    private Member sampleMember;

    @BeforeEach
    void setUp() {
        sampleBook = new Book("978-0-13-468599-1", "Clean Code", "Robert C. Martin", 3, Genre.TECHNOLOGY);
        sampleBook.setId(1L);
        sampleBook.setAvailableCopies(3);

        sampleMember = new Member("Alice", "alice@example.com", MembershipType.STANDARD);
        sampleMember.setId(1L);
    }

    // =========================================================================
    // EXAMPLE: borrowBook() happy path and key error cases — filled in
    // =========================================================================

    @Nested
    @DisplayName("borrowBook()")
    class BorrowBookTests {

        @Test
        @DisplayName("should successfully borrow a book when all conditions are met")
        void shouldBorrowBook_WhenAllConditionsMet() {
            // Arrange
            when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
            when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
            when(borrowRecordRepository.countActiveBorrowsByMember(1L)).thenReturn(0);
            when(borrowRecordRepository.existsByBookIdAndMemberIdAndStatus(1L, 1L, BorrowStatus.BORROWED))
                    .thenReturn(false);
            when(borrowRecordRepository.save(any(BorrowRecord.class)))
                    .thenAnswer(invocation -> {
                        BorrowRecord record = invocation.getArgument(0);
                        record.setId(1L);
                        return record;
                    });
            when(bookRepository.save(any(Book.class))).thenReturn(sampleBook);

            // Act
            BorrowResponse response = borrowService.borrowBook(1L, 1L);

            // Assert
            assertNotNull(response);
            assertEquals("Clean Code", response.getBookTitle());
            assertEquals("Alice", response.getMemberName());
            assertEquals(BorrowStatus.BORROWED, response.getStatus());

            // Verify interactions
            verify(borrowRecordRepository).save(any(BorrowRecord.class));
            verify(bookRepository).save(any(Book.class));
        }

        @Test
        @DisplayName("should throw MemberNotFoundException when member does not exist")
        void shouldThrow_WhenMemberNotFound() {
            when(memberRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(MemberNotFoundException.class,
                    () -> borrowService.borrowBook(1L, 99L));

            // Verify no borrow record was saved
            verify(borrowRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when book has no available copies")
        void shouldThrow_WhenNoAvailableCopies() {
            sampleBook.setAvailableCopies(0);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
            when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

            assertThrows(BookNotAvailableException.class,
                    () -> borrowService.borrowBook(1L, 1L));
        }

        // =====================================================================
        // TODO: Students should write the remaining borrowBook() tests
        // =====================================================================

        @Test
        @DisplayName("should throw when member has reached borrowing limit")
        void shouldThrow_WhenBorrowLimitReached() {
            // TODO: Set up mocks so countActiveBorrowsByMember returns maxBooks (3 for STANDARD)
            //       Then verify BorrowLimitExceededException is thrown

            //arrenge
            when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
            when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
            //3 for standard (choosing limit for type)
            when(borrowRecordRepository.countActiveBorrowsByMember(1L)).thenReturn(3);
            //act and assert 
            assertThrows(BorrowLimitExceededException.class, () -> {
                borrowService.borrowBook(1L, 1L);
            });
            //verifies that when exception is throwed nothing will be saved 
            verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
        }

        @Test
        @DisplayName("should throw when member already has this book borrowed")
        void shouldThrow_WhenDuplicateBorrow() {
            // TODO: Set up mocks so existsByBookIdAndMemberIdAndStatus returns true
            //       Then verify IllegalStateException is thrown

            //arrenge
            when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
            when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
            //critical part for this scenario
            when(borrowRecordRepository.existsByBookIdAndMemberIdAndStatus(1L, 1L, BorrowStatus.BORROWED))
                    .thenReturn(true);
            //act and assert
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> borrowService.borrowBook(1L, 1L));
            //checking exception message 
            assertEquals("Member already has this book borrowed", exception.getMessage());
            //verify
            verify(borrowRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when inactive member tries to borrow")
        void shouldThrow_WhenMemberInactive() {
            // TODO: Set member.active = false
            //       Then verify IllegalStateException is thrown with appropriate message
            
            //arrenge
            sampleMember.setActive(false);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
            //act and assert
            IllegalStateException exception1 = assertThrows(IllegalStateException.class,
                () -> borrowService.borrowBook(1L, 1L));
            assertEquals("Inactive members cannot borrow books", exception1.getMessage());
            //verify (bookRecord and bookRepository can not be saved because member is not active)
            verify(borrowRecordRepository, never()).save(any()); 
            verify(bookRepository, never()).save(any()); 
        }

        @Test
        @DisplayName("should decrease available copies after successful borrow")
        void shouldDecreaseAvailableCopies() {
            // TODO: After borrowBook(), verify that book.availableCopies decreased by 1
            //       Hint: Use ArgumentCaptor to capture the Book saved to repository
            
            //arrenge
            when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
            when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
            when(borrowRecordRepository.countActiveBorrowsByMember(1L)).thenReturn(0);
            when(borrowRecordRepository.existsByBookIdAndMemberIdAndStatus(1L, 1L, BorrowStatus.BORROWED))
                    .thenReturn(false);
            //act 
            borrowService.borrowBook(1L, 1L);
            //assert and verify
            //creating captor for catch book object that saved to database
            org.mockito.ArgumentCaptor<Book> bookCaptor = org.mockito.ArgumentCaptor.forClass(Book.class);
            //catching that which book object is saved to bookRepository.save() method
            verify(bookRepository).save(bookCaptor.capture());
            Book savedBook = bookCaptor.getValue();
            //stock was 3 in setUp now it should be 2
            assertEquals(2, savedBook.getAvailableCopies());

        }
    }

    // =========================================================================
    // TODO: Students should write returnBook() tests
    // =========================================================================

    @Nested
    @DisplayName("returnBook()")
    class ReturnBookTests {

        @Test
        @DisplayName("should successfully return a borrowed book")
        void shouldReturnBook_WhenBorrowed() {
            // TODO: Create a BorrowRecord with BORROWED status
            //       Mock the repository to return it
            //       Call returnBook() and verify:
            //       - status changed to RETURNED
            //       - returnDate is set
            //       - available copies increased

            //arrenge
            BorrowRecord record = new BorrowRecord(sampleBook, sampleMember);
            record.setId(1L);
            record.setStatus(BorrowStatus.BORROWED);
            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(record));
            //act and assert
            borrowService.returnBook(1L);
            assertEquals(BorrowStatus.RETURNED, record.getStatus());
            assertNotNull(record.getReturnDate());
            assertEquals(4, sampleBook.getAvailableCopies());
            //verify
            verify(borrowRecordRepository).save(record);
            verify(bookRepository).save(sampleBook);           
        }

        @Test
        @DisplayName("should throw when trying to return an already returned book")
        void shouldThrow_WhenAlreadyReturned() {
            // TODO: Create a BorrowRecord with RETURNED status
            //       Verify IllegalStateException is thrown

            //arrenge
            BorrowRecord record = new BorrowRecord(sampleBook, sampleMember);
            record.setId(1L);
            record.setStatus(BorrowStatus.RETURNED);
            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(record));
            //act and assert
            IllegalStateException exception2 = assertThrows(IllegalStateException.class, () -> {
                borrowService.returnBook(1L);
            });
            assertEquals("This book has already been returned", exception2.getMessage()); 
            //verify
            verify(borrowRecordRepository, never()).save(any());
            verify(bookRepository, never()).save(any());
            

        }

        @Test
        @DisplayName("should throw when borrow record not found")
        void shouldThrow_WhenRecordNotFound() {
            // TODO: Mock repository to return empty Optional
            //       Verify IllegalStateException is thrown
            
            //arrenge 
            when(borrowRecordRepository.findById(10L)).thenReturn(Optional.empty());
            // act and asser
            IllegalStateException exception3 = assertThrows(IllegalStateException.class, () -> {
                borrowService.returnBook(10L);
            });
            assertTrue(exception3.getMessage().contains("Borrow record not found"));
            //verify
            verify(borrowRecordRepository, never()).save(any());
            verify(bookRepository, never()).save(any());
        }
    }
}
