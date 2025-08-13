package io.pillopl.library.lending.book.infrastructure

import io.pillopl.library.catalogue.BookId
import io.pillopl.library.commons.aggregates.AggregateRootIsStale
import io.pillopl.library.commons.aggregates.Version
import io.pillopl.library.lending.LendingTestContext
import io.pillopl.library.lending.book.model.AvailableBook
import io.pillopl.library.lending.book.model.Book
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId
import io.pillopl.library.lending.patron.model.HoldDuration
import io.pillopl.library.lending.patron.model.PatronEvent
import io.pillopl.library.lending.patron.model.PatronId
import io.vavr.control.Option
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import static io.pillopl.library.catalogue.BookType.Circulating
import static io.pillopl.library.lending.book.model.BookFixture.anyBookId
import static io.pillopl.library.lending.book.model.BookFixture.circulatingAvailableBookAt
import static io.pillopl.library.lending.librarybranch.model.LibraryBranchFixture.anyBranch
import static io.pillopl.library.lending.patron.model.PatronEvent.BookPlacedOnHold.bookPlacedOnHoldNow
import static io.pillopl.library.lending.patron.model.PatronEvent.BookPlacedOnHoldEvents.events
import static io.pillopl.library.lending.patron.model.PatronFixture.anyPatronId

/**
 * Integration test for optimistic locking behavior in book aggregate persistence.
 * 
 * This test verifies that the book repository correctly implements optimistic locking
 * to prevent concurrent modification issues. When multiple processes attempt to modify
 * the same book aggregate simultaneously, the repository should detect version conflicts
 * and throw an AggregateRootIsStale exception to maintain data consistency.
 * 
 * This is crucial for maintaining aggregate integrity in a concurrent environment
 * where multiple patrons might attempt to place holds on the same book simultaneously.
 */
@SpringBootTest(classes = LendingTestContext.class)
class OptimisticLockingBookAggregateIT extends Specification {

    BookId bookId = anyBookId()
    LibraryBranchId libraryBranchId = anyBranch()
    PatronId patronId = anyPatronId()

    @Autowired
    BookDatabaseRepository bookEntityRepository

    /**
     * Verifies that optimistic locking correctly prevents concurrent modifications
     * to the same book aggregate. This test simulates a scenario where a book is
     * loaded, modified by another process, and then an attempt is made to save
     * the stale version. The repository should detect the version conflict and
     * throw an AggregateRootIsStale exception to maintain data consistency.
     */
    def 'persistence in real database should work'() {
        given:
            AvailableBook availableBook = circulatingAvailableBookAt(bookId, libraryBranchId)
        and:
            bookEntityRepository.save(availableBook)
        and:
            Book loaded = loadPersistedBook(bookId)
        and:
            someoneModifiedBookInTheMeantime(availableBook)
        when:
            bookEntityRepository.save(loaded)
        then:
            thrown(AggregateRootIsStale)
            loadPersistedBook(bookId).version == new Version(1)
    }

    void someoneModifiedBookInTheMeantime(AvailableBook availableBook) {
        bookEntityRepository.save(availableBook.handle(placedOnHoldBy(anyPatronId())))
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

    PatronEvent.BookPlacedOnHold placedOnHoldBy(PatronId patronId) {
        return events(bookPlacedOnHoldNow(
                bookId,
                Circulating,
                libraryBranchId,
                patronId,
                HoldDuration.closeEnded(5)))
                .bookPlacedOnHold
    }
}
