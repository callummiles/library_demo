package io.pillopl.library.lending.book.infrastructure

import io.pillopl.library.catalogue.BookId
import io.pillopl.library.lending.LendingTestContext
import io.pillopl.library.lending.book.model.AvailableBook
import io.pillopl.library.lending.book.model.BookOnHold
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId
import io.pillopl.library.lending.patron.model.HoldDuration
import io.pillopl.library.lending.patron.model.PatronEvent
import io.pillopl.library.lending.patron.model.PatronId
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
 * Integration test for finding books that are currently on hold by specific patrons.
 * 
 * This test verifies that the repository can correctly identify and retrieve
 * books that are on hold by a particular patron. This functionality is essential
 * for operations like checking out books (which requires the book to be on hold
 * by the patron) and canceling holds.
 * 
 * The test ensures that the query correctly associates books with the patrons
 * who have placed holds on them, and that books not on hold return empty results.
 */
@SpringBootTest(classes = LendingTestContext.class)
class FindBookOnHoldInDatabaseIT extends Specification {

    BookId bookId = anyBookId()
    LibraryBranchId libraryBranchId = anyBranch()
    PatronId patronId = anyPatronId()

    @Autowired
    BookDatabaseRepository bookEntityRepository

    /**
     * Verifies that the repository can correctly identify books that are on hold
     * by a specific patron. This test ensures that when a book is available,
     * the findBookOnHold query returns empty, but after a patron places a hold
     * on the book, the query correctly returns the book for that patron.
     */
    def 'should find book on hold in database'() {
        given:
            AvailableBook availableBook = circulatingAvailableBookAt(bookId, libraryBranchId)
        when:
            bookEntityRepository.save(availableBook)
        then:
            bookEntityRepository.findBookOnHold(bookId, patronId).isEmpty()
        when:
            BookOnHold bookOnHold = availableBook.handle(placedOnHoldBy(patronId))
        and:
            bookEntityRepository.save(bookOnHold)
        then:
            bookEntityRepository.findBookOnHold(bookId, patronId).isDefined()
    }

    PatronEvent.BookPlacedOnHold placedOnHoldBy(PatronId patronId) {
        return events(bookPlacedOnHoldNow(
                bookId,
                Circulating,
                libraryBranchId,
                patronId,
                HoldDuration.closeEnded(5))).bookPlacedOnHold
    }

}
