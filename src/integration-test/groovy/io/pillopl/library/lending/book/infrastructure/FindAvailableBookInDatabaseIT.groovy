package io.pillopl.library.lending.book.infrastructure

import io.pillopl.library.catalogue.BookId
import io.pillopl.library.lending.LendingTestContext
import io.pillopl.library.lending.book.model.AvailableBook
import io.pillopl.library.lending.book.model.BookOnHold
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId
import io.pillopl.library.lending.patron.model.PatronEvent
import io.pillopl.library.lending.patron.model.PatronId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import static io.pillopl.library.catalogue.BookType.Circulating
import static io.pillopl.library.lending.book.model.BookFixture.anyBookId
import static io.pillopl.library.lending.book.model.BookFixture.circulatingAvailableBookAt
import static io.pillopl.library.lending.librarybranch.model.LibraryBranchFixture.anyBranch
import static io.pillopl.library.lending.patron.model.HoldDuration.closeEnded
import static io.pillopl.library.lending.patron.model.PatronEvent.BookPlacedOnHold.bookPlacedOnHoldNow
import static io.pillopl.library.lending.patron.model.PatronEvent.BookPlacedOnHoldEvents.events
import static io.pillopl.library.lending.patron.model.PatronFixture.anyPatronId

/**
 * Integration test for finding available books in the database.
 * 
 * This test verifies that the repository can correctly identify and retrieve
 * books that are in an "available" state (ready for lending). It tests the
 * query functionality that distinguishes between available books and books
 * that are currently on hold or checked out.
 * 
 * This is essential for the lending process, as patrons can only place holds
 * on books that are currently available for lending.
 */
@SpringBootTest(classes = LendingTestContext.class)
class FindAvailableBookInDatabaseIT extends Specification {

    BookId bookId = anyBookId()
    LibraryBranchId libraryBranchId = anyBranch()
    PatronId patronId = anyPatronId()

    @Autowired
    BookDatabaseRepository bookEntityRepository

    /**
     * Verifies that the repository can correctly identify available books and
     * distinguish them from books that are on hold. This test ensures that
     * when a book is available, it can be found by the findAvailableBookBy query,
     * but when the same book is placed on hold, it is no longer returned by
     * the available book query.
     */
    def 'should find available book in database'() {
        given:
            AvailableBook availableBook = circulatingAvailableBookAt(bookId, libraryBranchId)
        when:
            bookEntityRepository.save(availableBook)
        then:
            bookEntityRepository.findAvailableBookBy(bookId).isDefined()
        when:
            BookOnHold bookOnHold = availableBook.handle(placedOnHold())
        and:
            bookEntityRepository.save(bookOnHold)
        then:
            bookEntityRepository.findAvailableBookBy(bookId).isEmpty()
    }


    PatronEvent.BookPlacedOnHold placedOnHold() {
        return events(
                bookPlacedOnHoldNow(
                        bookId, Circulating,
                        libraryBranchId, patronId,
                        closeEnded(5)))
                .bookPlacedOnHold
    }

}
