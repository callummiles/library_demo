package io.pillopl.library.lending.book.new_model

import io.pillopl.library.catalogue.BookId
import io.pillopl.library.catalogue.BookType
import io.pillopl.library.commons.aggregates.Version
import io.pillopl.library.lending.book.model.AvailableBook
import io.pillopl.library.lending.book.model.BookOnHold
import io.pillopl.library.lending.book.model.CheckedOutBook
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId
import io.pillopl.library.lending.patron.model.PatronId
import spock.lang.Specification

import java.time.Instant

class BookMigrationUtilityTest extends Specification {

    def bookId = new BookId(UUID.randomUUID())
    def bookType = BookType.Circulating
    def branchId = new LibraryBranchId(UUID.randomUUID())
    def patronId = new PatronId(UUID.randomUUID())
    def version = Version.zero()
    def holdTill = Instant.now().plusDays(7)

    def "should migrate AvailableBook to Book with AvailableState"() {
        given:
        def availableBook = new AvailableBook(bookId, bookType, branchId, version)

        when:
        def migratedBook = BookMigrationUtility.migrate(availableBook)

        then:
        migratedBook.bookId == bookId
        migratedBook.bookType == bookType
        migratedBook.version == version
        migratedBook.currentState == "AVAILABLE"
        migratedBook.currentBranch == branchId
        migratedBook.currentPatron == null
    }

    def "should migrate BookOnHold to Book with OnHoldState"() {
        given:
        def bookOnHold = new BookOnHold(bookId, bookType, branchId, patronId, holdTill, version)

        when:
        def migratedBook = BookMigrationUtility.migrate(bookOnHold)

        then:
        migratedBook.bookId == bookId
        migratedBook.bookType == bookType
        migratedBook.version == version
        migratedBook.currentState == "ON_HOLD"
        migratedBook.currentBranch == branchId
        migratedBook.currentPatron == patronId
    }

    def "should migrate CheckedOutBook to Book with CheckedOutState"() {
        given:
        def checkedOutBook = new CheckedOutBook(bookId, bookType, branchId, patronId, version)

        when:
        def migratedBook = BookMigrationUtility.migrate(checkedOutBook)

        then:
        migratedBook.bookId == bookId
        migratedBook.bookType == bookType
        migratedBook.version == version
        migratedBook.currentState == "CHECKED_OUT"
        migratedBook.currentBranch == branchId
        migratedBook.currentPatron == patronId
    }

    def "should preserve version numbers during migration"() {
        given:
        def higherVersion = version.next().next()
        def availableBook = new AvailableBook(bookId, bookType, branchId, higherVersion)

        when:
        def migratedBook = BookMigrationUtility.migrate(availableBook)

        then:
        migratedBook.version == higherVersion
    }

    def "should handle restricted books correctly"() {
        given:
        def restrictedBookType = BookType.Restricted
        def availableBook = new AvailableBook(bookId, restrictedBookType, branchId, version)

        when:
        def migratedBook = BookMigrationUtility.migrate(availableBook)

        then:
        migratedBook.bookType == restrictedBookType
        migratedBook.currentState == "AVAILABLE"
    }
}
