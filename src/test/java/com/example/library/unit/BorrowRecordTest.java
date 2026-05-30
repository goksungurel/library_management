package com.example.library.unit;

import com.example.library.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UNIT TEST - Model Layer
 */
class BorrowRecordTest {

    private Book createSampleBook() {
        Book book = new Book("978-0-13-468599-1", "Clean Code", "Robert C. Martin", 3, Genre.TECHNOLOGY);
        book.setId(1L);
        return book;
    }

    private Member createSampleMember() {
        Member member = new Member("Alice", "alice@example.com", MembershipType.STANDARD);
        member.setId(1L);
        return member;
    }

    // =========================================================================
    // EXAMPLE: calculateFine() tests — filled in as reference
    // =========================================================================

    @Nested
    @DisplayName("calculateFine()")
    class CalculateFineTests {

        @Test
        @DisplayName("should return 0 when book is returned on time")
        void shouldReturnZeroFine_WhenReturnedOnTime() {
            BorrowRecord record = new BorrowRecord(createSampleBook(), createSampleMember());
            record.setReturnDate(record.getDueDate()); // returned exactly on due date

            assertEquals(0.0, record.calculateFine());
        }

        @Test
        @DisplayName("should return 0 when book is returned before due date")
        void shouldReturnZeroFine_WhenReturnedEarly() {
            BorrowRecord record = new BorrowRecord(createSampleBook(), createSampleMember());
            record.setReturnDate(record.getBorrowDate().plusDays(5)); // returned after 5 days

            assertEquals(0.0, record.calculateFine());
        }

        @Test
        @DisplayName("should calculate correct fine when returned 3 days late")
        void shouldCalculateCorrectFine_WhenReturnedLate() {
            BorrowRecord record = new BorrowRecord(createSampleBook(), createSampleMember());
            record.setReturnDate(record.getDueDate().plusDays(3)); // 3 days late

            double expectedFine = 3 * BorrowRecord.DAILY_FINE_RATE; // 3 * 1.50 = 4.50
            assertEquals(expectedFine, record.calculateFine());
        }

        @Test
        @DisplayName("should return 0 when book is not yet returned")
        void shouldReturnZeroFine_WhenNotYetReturned() {
            BorrowRecord record = new BorrowRecord(createSampleBook(), createSampleMember());
            // returnDate is null

            assertEquals(0.0, record.calculateFine());
        }
    }

    @Nested
    @DisplayName("isOverdue()")
    class IsOverdueTests {

        @Test
        @DisplayName("should return true when checked after due date and still borrowed")
        void shouldBeOverdue_WhenPastDueDateAndStillBorrowed() {
            // Create a borrow record and manually push the due date to yesterday
            // Since the member still has the book, isOverdue() should return true
            BorrowRecord record = new BorrowRecord(createSampleBook(), createSampleMember());
            record.setDueDate(LocalDate.now().minusDays(1));
            record.setStatus(BorrowStatus.BORROWED);
            assertTrue(record.isOverdue(LocalDate.now()));

        }

        @Test
        @DisplayName("should return false when checked before due date")
        void shouldNotBeOverdue_WhenBeforeDueDate() {
            // Due date is tomorrow, so checking today should not be overdue

            BorrowRecord record = new BorrowRecord(createSampleBook(), createSampleMember());

            record.setDueDate(LocalDate.now().plusDays(1));

            assertFalse(record.isOverdue(LocalDate.now()));
        }

        @Test
        @DisplayName("should return false when book is already returned (even if past due)")
        void shouldNotBeOverdue_WhenAlreadyReturned() {
            // Even though the due date was 5 days ago, the book is already returned
            // A returned book should never be considered overdue
            BorrowRecord record = new BorrowRecord(createSampleBook(), createSampleMember());
            record.setDueDate(LocalDate.now().minusDays(5));
            record.setStatus(BorrowStatus.RETURNED);

            assertFalse(record.isOverdue(LocalDate.now()));
        }

        @Test
        @DisplayName("should return false on exactly the due date")
        void shouldNotBeOverdue_OnExactDueDate() {
            // The book is due today, checking today should still not be overdue
            // Overdue means strictly past the due date, not on it
            BorrowRecord record = new BorrowRecord(createSampleBook(), createSampleMember());
            record.setDueDate(LocalDate.now());

            assertFalse(record.isOverdue(LocalDate.now()));
        }
    }

    @Nested
    @DisplayName("Constructor / default values")
    class ConstructorTests {

        @Test
        @DisplayName("should set borrow date to today")
        void shouldSetBorrowDateToToday() {
            // When a borrow record is created, it should automatically record today as the borrow date
            BorrowRecord record = new BorrowRecord(createSampleBook(), createSampleMember());
            assertEquals(LocalDate.now(), record.getBorrowDate());
        }

        @Test
        @DisplayName("should set due date to 14 days from today")
        void shouldSetDueDateTo14DaysFromToday() {
            // The default loan period is 14 days, so due date should be exactly 2 weeks from today
            BorrowRecord record = new BorrowRecord(createSampleBook(), createSampleMember());
            LocalDate expectedDate = LocalDate.now().plusDays(14);
            assertEquals(expectedDate, record.getDueDate());
        }

        @Test
        @DisplayName("should set status to BORROWED")
        void shouldSetStatusToBorrowed() {
            // A newly created borrow record should always start with BORROWED status
            BorrowRecord record = new BorrowRecord(createSampleBook(), createSampleMember());
            assertEquals(BorrowStatus.BORROWED, record.getStatus());
        }
    }
}
