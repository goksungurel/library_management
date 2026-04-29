package com.example.library.integration;

import com.example.library.model.Book;
import com.example.library.model.Genre;
import com.example.library.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }

    private Book createBook(String isbn, String title, String author, int copies, Genre genre) {
        Book book = new Book(isbn, title, author, copies, genre);
        book.setPublishedDate(LocalDate.of(2020, 1, 1));
        return bookRepository.save(book);
    }

    @Nested
    @DisplayName("Basic CRUD operations")
    class CrudTests {

        @Test
        @DisplayName("should save and retrieve a book by ID")
        void shouldSaveAndFindById() {
            Book saved = createBook("978-0-43-935806-9", "Harry Potter and the Sorcerer's Stone", "J.K. Rowling", 3, Genre.FICTION);

            Optional<Book> found = bookRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Harry Potter and the Sorcerer's Stone");
            assertThat(found.get().getIsbn()).isEqualTo("978-0-43-935806-9");
        }

        @Test
        @DisplayName("should find book by ISBN")
        void shouldFindByIsbn() {
            createBook("978-0-43-935806-9", "Harry Potter and the Sorcerer's Stone", "J.K. Rowling", 3, Genre.FICTION);

            Optional<Book> found = bookRepository.findByIsbn("978-0-43-935806-9");

            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Harry Potter and the Sorcerer's Stone");
        }

        @Test
        @DisplayName("should return empty when ISBN not found")
        void shouldReturnEmpty_WhenIsbnNotFound() {
            Optional<Book> found = bookRepository.findByIsbn("000-0-00-000000-0");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Custom query methods")
    class CustomQueryTests {

        @Test
        @DisplayName("should search books by keyword in title or author (case insensitive)")
        void shouldSearchByKeyword() {
            createBook("978-0-43-935806-9", "Harry Potter and the Sorcerer's Stone", "J.K. Rowling", 3, Genre.FICTION);
            createBook("978-0-43-935807-6", "Harry Potter and the Chamber of Secrets", "J.K. Rowling", 2, Genre.FICTION);
            createBook("978-0-26-110325-1", "The Lord of the Rings", "J.R.R. Tolkien", 5, Genre.FICTION);

            List<Book> results = bookRepository.searchBooks("harry potter");

            assertThat(results).hasSize(2);
            assertThat(results).extracting(Book::getTitle)
                    .containsExactlyInAnyOrder("Harry Potter and the Sorcerer's Stone", "Harry Potter and the Chamber of Secrets");
        }

        @Test
        @DisplayName("should find available books (copies > 0)")
        void shouldFindAvailableBooks() {
            Book available = createBook("978-0-43-935806-9", "Harry Potter and the Sorcerer's Stone", "J.K. Rowling", 3, Genre.FICTION);
            Book unavailable = createBook("978-0-26-110325-1", "The Lord of the Rings", "J.R.R. Tolkien", 1, Genre.FICTION);
            unavailable.setAvailableCopies(0);
            bookRepository.save(unavailable);

            List<Book> results = bookRepository.findAvailableBooks();

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Harry Potter and the Sorcerer's Stone");
        }
    }

    @Nested
    @DisplayName("Genre and author queries")
    class FilterTests {

        @Test
        @DisplayName("should find books by genre")
        void shouldFindByGenre() {
            createBook("978-0-55-329241-3", "A Brief History of Time", "Stephen Hawking", 2, Genre.SCIENCE);
            createBook("978-0-39-333230-3", "Cosmos", "Carl Sagan", 3, Genre.SCIENCE);
            createBook("978-0-43-935806-9", "Harry Potter and the Sorcerer's Stone", "J.K. Rowling", 5, Genre.FICTION);
            createBook("978-0-06-093546-9", "Sapiens", "Yuval Noah Harari", 4, Genre.HISTORY);

            List<Book> results = bookRepository.findByGenre(Genre.SCIENCE);

            assertThat(results).hasSize(2);
            assertThat(results).extracting(Book::getTitle)
                    .containsExactlyInAnyOrder("A Brief History of Time", "Cosmos");
        }

        @Test
        @DisplayName("should find books by author (case insensitive, partial match)")
        void shouldFindByAuthor() {
            createBook("978-0-43-935806-9", "Harry Potter and the Sorcerer's Stone", "J.K. Rowling", 3, Genre.FICTION);
            createBook("978-0-43-935807-6", "Harry Potter and the Chamber of Secrets", "J.K. Rowling", 2, Genre.FICTION);
            createBook("978-0-26-110325-1", "The Lord of the Rings", "J.R.R. Tolkien", 5, Genre.FICTION);

            List<Book> results = bookRepository.findByAuthorContainingIgnoreCase("rowling");

            assertThat(results).hasSize(2);
            assertThat(results).extracting(Book::getAuthor)
                    .allMatch(author -> author.toLowerCase().contains("rowling"));
        }

        @Test
        @DisplayName("should search by author name using searchBooks()")
        void shouldSearchByAuthorKeyword() {
            createBook("978-0-43-935806-9", "Harry Potter and the Sorcerer's Stone", "J.K. Rowling", 3, Genre.FICTION);
            createBook("978-0-55-329241-3", "A Brief History of Time", "Stephen Hawking", 2, Genre.SCIENCE);
            createBook("978-0-26-110325-1", "The Lord of the Rings", "J.R.R. Tolkien", 4, Genre.FICTION);

            List<Book> results = bookRepository.searchBooks("Tolkien");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("The Lord of the Rings");
        }

        @Test
        @DisplayName("should return empty list when no books match search")
        void shouldReturnEmpty_WhenNoMatch() {
            createBook("978-0-43-935806-9", "Harry Potter and the Sorcerer's Stone", "J.K. Rowling", 3, Genre.FICTION);
            createBook("978-0-26-110325-1", "The Lord of the Rings", "J.R.R. Tolkien", 5, Genre.FICTION);

            List<Book> results = bookRepository.searchBooks("xyznonexistentkeyword");

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should enforce unique ISBN constraint")
        void shouldEnforceUniqueIsbn() {
            createBook("978-0-43-935806-9", "Harry Potter and the Sorcerer's Stone", "J.K. Rowling", 2, Genre.FICTION);

            Book duplicate = new Book("978-0-43-935806-9", "Harry Potter and the Goblet of Fire", "J.K. Rowling", 3, Genre.FICTION);
            duplicate.setPublishedDate(LocalDate.of(2021, 1, 1));

            assertThrows(DataIntegrityViolationException.class, () -> {
                bookRepository.saveAndFlush(duplicate);
            });
        }

        @Test
        @DisplayName("should handle deleting a book")
        void shouldDeleteBook() {
            Book saved = createBook("978-0-43-935806-9", "Harry Potter and the Sorcerer's Stone", "J.K. Rowling", 2, Genre.FICTION);
            Long savedId = saved.getId();

            assertThat(bookRepository.findById(savedId)).isPresent();

            bookRepository.deleteById(savedId);

            assertThat(bookRepository.findById(savedId)).isEmpty();
        }
    }
}