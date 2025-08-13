package io.pillopl.library.lending.book.infrastructure

import io.pillopl.library.catalogue.BookId
import io.pillopl.library.lending.LendingTestContext
import io.pillopl.library.lending.book.model.AvailableBook
import io.pillopl.library.lending.book.model.Book
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId
import io.pillopl.library.lending.patron.model.PatronId
import io.vavr.control.Option
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import static io.pillopl.library.lending.book.model.BookFixture.anyBookId
import static io.pillopl.library.lending.book.model.BookFixture.circulatingAvailableBookAt
import static io.pillopl.library.lending.librarybranch.model.LibraryBranchFixture.anyBranch
import static io.pillopl.library.lending.patron.model.PatronFixture.anyPatronId

/**
 * Integration test for the BookDatabaseRepository persistence operations.
 * 
 * This test verifies that book aggregates can be properly persisted to and retrieved
 * from the database using the BookDatabaseRepository. It focuses on testing the
 * infrastructure layer's ability to handle book aggregate persistence within the
 * lending bounded context.
 * 
 * The test ensures that the repository correctly maintains book state and can
 * persist different book states (like AvailableBook) to the underlying database
 * infrastructure.
 */
@SpringBootTest(classes = LendingTestContext.class)
class BookDatabaseRepositoryIT extends Specification {

    BookId bookId = anyBookId()
    LibraryBranchId libraryBranchId = anyBranch()
    PatronId patronId = anyPatronId()

    @Autowired
    BookDatabaseRepository bookEntityRepository

    /**
     * Verifies that book aggregates can be successfully persisted to and retrieved
     * from the database. This test ensures the basic persistence functionality
     * works correctly for book aggregates in the lending context, confirming
     * that the repository can save and load books with their correct state.
     */
    def 'persistence in real database should work'() {
        given:
            AvailableBook availableBook = circulatingAvailableBookAt(bookId, libraryBranchId)
        when:
            bookEntityRepository.save(availableBook)
        then:
            bookIsPersistedAs(AvailableBook.class)
    }

    void bookIsPersistedAs(Class<?> clz) {
        Book book = loadPersistedBook(bookId)
        assert book.class == clz
    }

    Book loadPersistedBook(BookId bookId) {
        Option<Book> loaded = bookEntityRepository.findBy(bookId)
        Book book = loaded.getOrElseThrow({
            new IllegalStateException("should have been persisted")
        })
        return book
    }
}
