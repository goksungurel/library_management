package com.example.library.api;

import com.example.library.integration.AbstractIntegrationTest;
import com.example.library.model.*;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.MemberRepository;
import com.example.library.dto.BorrowRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API TEST (End-to-End)
 * In this class, the full Spring Boot application is started.
 * The tests send real HTTP requests with TestRestTemplate and check the API responses.
 * (controller, service, repository and database layers work together in these tests.)
 */

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LibraryApiIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        // The application starts on a random port, so we build the base API URL here.
        baseUrl = "http://localhost:" + port + "/api";
        // clean the database before each test. makes each test independent from the other tests.
        borrowRecordRepository.deleteAll();
        bookRepository.deleteAll();
        memberRepository.deleteAll();
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private Book createTestBook(String isbn, String title, String author) {
        // creates a test book with 3 available copies.
        //used to avoid writing the same book creation code again and again.
        Book book = new Book(isbn, title, author, 3, Genre.TECHNOLOGY);
        return bookRepository.save(book);
    }

    private Member createTestMember(String name, String email, MembershipType type) {
        //creates a test member with the given membership type.
        // Different membership types are important bc borrow limits r different.
        Member member = new Member(name, email, type);
        return memberRepository.save(member);
    }

    // =========================================================================
    // EXAMPLE: Book API tests — filled in
    // =========================================================================

    @Nested
    @DisplayName("POST /api/books")
    class CreateBookApi {

        @Test
        @DisplayName("should create a book and return 201")
        void shouldCreateBook() {
            //create a valid book object and send it to the POST /api/books endpoint
            // If the request is successful, the API should return 201 Created
            Book newBook = new Book("978-0-13-468599-1", "Clean Code", "Robert C. Martin", 3, Genre.TECHNOLOGY);

            ResponseEntity<Book> response = restTemplate.postForEntity(
                    baseUrl + "/books", newBook, Book.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isNotNull();
            assertThat(response.getBody().getTitle()).isEqualTo("Clean Code");
            assertThat(response.getBody().getAvailableCopies()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return 400 when required fields are missing")
        void shouldReturn400_WhenFieldsMissing() {// This book has no required fields like ISBN, title and author.
        // Because of validation rules the API should reject this request
            Book invalidBook = new Book(); // no required fields set

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/books", invalidBook, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should return 400 when duplicate ISBN")
        void shouldReturn400_WhenDuplicateIsbn() {// 1st save a book with this ISBN.
        // Then create another book with the same ISBN.
            createTestBook("978-0-13-468599-1", "Clean Code", "Robert C. Martin");

            Book duplicate = new Book("978-0-13-468599-1", "Another Book", "Another Author", 2, Genre.FICTION);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/books", duplicate, Map.class);
        // ISBN must be uniquem so duplicate ISBN should give a bad request response      
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("GET /api/books")
    class GetBooksApi {

        @Test
        @DisplayName("should return all books")
        void shouldReturnAllBooks() {
            createTestBook("978-1", "Book A", "Author A");
            createTestBook("978-2", "Book B", "Author B");

            ResponseEntity<Book[]> response = restTemplate.getForEntity(
                    baseUrl + "/books", Book[].class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("should return 404 for non-existent book")
        void shouldReturn404_WhenBookNotFound() {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    baseUrl + "/books/999", Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // =========================================================================
    // EXAMPLE: Borrow flow — the most important E2E test
    // =========================================================================

    @Nested
    @DisplayName("Borrow Flow (POST /api/borrows)")
    class BorrowFlowApi {

        @Test
        @DisplayName("should complete full borrow-return cycle")
        void shouldCompleteBorrowReturnCycle() {
            // Setup
            Book book = createTestBook("978-1", "Test Book", "Test Author");
            Member member = createTestMember("Alice", "alice@test.com", MembershipType.STANDARD);

            // 1. Borrow the book by sending bookId and memberId to the API.
            BorrowRequest borrowRequest = new BorrowRequest(book.getId(), member.getId());
            ResponseEntity<Map> borrowResponse = restTemplate.postForEntity(
                    baseUrl + "/borrows", borrowRequest, Map.class);

            assertThat(borrowResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(borrowResponse.getBody()).containsEntry("bookTitle", "Test Book");
            assertThat(borrowResponse.getBody()).containsEntry("memberName", "Alice");
            assertThat(borrowResponse.getBody()).containsEntry("status", "BORROWED");

            Number borrowId = (Number) borrowResponse.getBody().get("id");

            // 2. Verify book availability decreased
            ResponseEntity<Book> bookResponse = restTemplate.getForEntity(
                    baseUrl + "/books/" + book.getId(), Book.class);
            assertThat(bookResponse.getBody().getAvailableCopies()).isEqualTo(2);

            // 3. return the borrowed book by using the borrow record id.
            ResponseEntity<Map> returnResponse = restTemplate.postForEntity(
                    baseUrl + "/borrows/" + borrowId.longValue() + "/return",
                    null, Map.class);

            assertThat(returnResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(returnResponse.getBody()).containsEntry("status", "RETURNED");

            // 4. Verify book availability increased back
            bookResponse = restTemplate.getForEntity(
                    baseUrl + "/books/" + book.getId(), Book.class);
            assertThat(bookResponse.getBody().getAvailableCopies()).isEqualTo(3);
        }
    }

    // =========================================================================
    // TODO: Students should write these API tests
    // =========================================================================

    @Nested
    @DisplayName("POST /api/borrows - Error cases")
    class BorrowErrorsApi {

        @Test
        @DisplayName("should return 409 when borrowing limit exceeded")
        void shouldReturn409_WhenBorrowLimitExceeded() {// STUDENT members can borrow maximum 2 books.
        //  in this test, the first two borrow requests should work
        // but the third request should fail with 409 Conflict.
            Member student = createTestMember("Bob", "bob@test.com", MembershipType.STUDENT);
            Book book1 = createTestBook("978-1", "Book One", "Author A");
            Book book2 = createTestBook("978-2", "Book Two", "Author B");
            Book book3 = createTestBook("978-3", "Book Three", "Author C");

            ResponseEntity<Map> first = restTemplate.postForEntity(
                    baseUrl + "/borrows", new BorrowRequest(book1.getId(), student.getId()), Map.class);
            ResponseEntity<Map> second = restTemplate.postForEntity(
                    baseUrl + "/borrows", new BorrowRequest(book2.getId(), student.getId()), Map.class);

            assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            ResponseEntity<Map> third = restTemplate.postForEntity(
                    baseUrl + "/borrows", new BorrowRequest(book3.getId(), student.getId()), Map.class);

            assertThat(third.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("should return 409 when no copies available")
        void shouldReturn409_WhenNoCopiesAvailable() {// This book has only 1 copy.
        // after the first member borrows it there is no copy left
        // so the second member should not be able to borrow the same book.
            Book book = bookRepository.save(new Book("978-1", "Only Copy", "Author A", 1, Genre.TECHNOLOGY));
            Member first = createTestMember("Alice", "alice@test.com", MembershipType.STANDARD);
            Member second = createTestMember("Bob", "bob@test.com", MembershipType.STANDARD);

            ResponseEntity<Map> firstBorrow = restTemplate.postForEntity(
                    baseUrl + "/borrows", new BorrowRequest(book.getId(), first.getId()), Map.class);
            assertThat(firstBorrow.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            ResponseEntity<Map> secondBorrow = restTemplate.postForEntity(
                    baseUrl + "/borrows", new BorrowRequest(book.getId(), second.getId()), Map.class);

            assertThat(secondBorrow.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("should return 404 when member does not exist")
        void shouldReturn404_WhenMemberNotFound() {// The book exists, but the member id does not exist in the database.
        // bc of this the API should return 404 Not Found.
            Book book = createTestBook("978-1", "A Book", "Author A");

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/borrows", new BorrowRequest(book.getId(), 99999L), Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("should return 404 when book does not exist")
        void shouldReturn404_WhenBookNotFound() {// The member exists but the book id does not exist in the db
        // so the borrow request should fail with 404 Not Found.
            Member member = createTestMember("Alice", "alice@test.com", MembershipType.STANDARD);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/borrows", new BorrowRequest(99999L, member.getId()), Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Member API")
    class MemberApiTests {

        @Test
        @DisplayName("should create a member and return 201")
        void shouldCreateMember() {// We send a valid member object to the API.
        // the API should create the member and return 201 Created
            Member newMember = new Member("Carol", "carol@test.com", MembershipType.PREMIUM);

            ResponseEntity<Member> response = restTemplate.postForEntity(
                    baseUrl + "/members", newMember, Member.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isNotNull();
            assertThat(response.getBody().getName()).isEqualTo("Carol");
            assertThat(response.getBody().getEmail()).isEqualTo("carol@test.com");
            assertThat(response.getBody().getMembershipType()).isEqualTo(MembershipType.PREMIUM);
            assertThat(response.getBody().isActive()).isTrue();
        }

        @Test
        @DisplayName("should deactivate a member via DELETE")
        void shouldDeactivateMember() {// DELETE does not remove the member from the database completely.
        // It only changes the active field to false.
            Member member = createTestMember("Dave", "dave@test.com", MembershipType.STANDARD);

            ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                    baseUrl + "/members/" + member.getId(),
                    HttpMethod.DELETE,
                    null,
                    Void.class);
            assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // We get the same member again to check if active became false.
            ResponseEntity<Member> getResponse = restTemplate.getForEntity(
                    baseUrl + "/members/" + member.getId(), Member.class);
            assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(getResponse.getBody()).isNotNull();
            assertThat(getResponse.getBody().isActive()).isFalse();
        }

        @Test
        @DisplayName("should return 400 when creating member with invalid email")
        void shouldReturn400_WhenInvalidEmail() {// This email format is not valid.
        // Because of validation, the API should return 400 Bad Request.
            Member invalid = new Member("Eve", "not-an-email", MembershipType.STANDARD);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/members", invalid, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Search & Filter API")
    class SearchApiTests {

        @Test
        @DisplayName("should search books by keyword via GET /api/books/search?keyword=...")
        void shouldSearchBooks() {// We create two books with "Clean" in the title and one different book.
        // Searching with "clean" should return only the matching books.
            createTestBook("978-1", "Clean Code", "Robert C. Martin");
            createTestBook("978-2", "Clean Architecture", "Robert C. Martin");
            createTestBook("978-3", "Design Patterns", "Gang of Four");

            ResponseEntity<Book[]> response = restTemplate.getForEntity(
                    baseUrl + "/books/search?keyword=clean", Book[].class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody()).extracting(Book::getTitle)
                    .containsExactlyInAnyOrder("Clean Code", "Clean Architecture");
        }

        @Test
        @DisplayName("should get active borrows for a member")
        void shouldGetActiveBorrows() {// The member borrows two books.
        // Then one book is returned.
        // The active borrows endpoint should show only the book that is still borrowed.
            Member member = createTestMember("Frank", "frank@test.com", MembershipType.STANDARD);
            Book book1 = createTestBook("978-1", "Book One", "Author A");
            Book book2 = createTestBook("978-2", "Book Two", "Author B");

            ResponseEntity<Map> b1 = restTemplate.postForEntity(
                    baseUrl + "/borrows", new BorrowRequest(book1.getId(), member.getId()), Map.class);
            ResponseEntity<Map> b2 = restTemplate.postForEntity(
                    baseUrl + "/borrows", new BorrowRequest(book2.getId(), member.getId()), Map.class);
            assertThat(b1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(b2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        //return only the first borrowed book.
            Number borrowId1 = (Number) b1.getBody().get("id");
        
            ResponseEntity<Map> ret = restTemplate.postForEntity(
                    baseUrl + "/borrows/" + borrowId1.longValue() + "/return", null, Map.class);
            assertThat(ret.getStatusCode()).isEqualTo(HttpStatus.OK);
        //ask the API for active borrows of this member.       
            ResponseEntity<Map[]> active = restTemplate.getForEntity(
                    baseUrl + "/borrows/member/" + member.getId() + "/active", Map[].class);

            assertThat(active.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(active.getBody()).hasSize(1);
            assertThat(active.getBody()[0]).containsEntry("bookTitle", "Book Two");
        }
    }
}
