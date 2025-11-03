package io.pillopl.library.lending.book.new_model

import io.pillopl.library.catalogue.BookId
import io.pillopl.library.catalogue.BookType
import io.pillopl.library.commons.aggregates.Version
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId
import io.pillopl.library.lending.patron.model.PatronId
import spock.lang.Specification

import java.time.Instant

import static io.pillopl.library.lending.book.model.BookFixture.anyBookId
import static io.pillopl.library.lending.librarybranch.model.LibraryBranchFixture.anyBranch
import static io.pillopl.library.lending.patron.model.PatronFixture.anyPatronId

class BookTest extends Specification {

    def 'should create book in available state'() {
        given:
            BookId bookId = anyBookId()
            LibraryBranchId branch = anyBranch()

        when:
            Book book = new Book(bookId, BookType.Circulating, branch, Version.zero())

        then:
            book.bookId == bookId
            book.bookType == BookType.Circulating
            book.version == Version.zero()
            book.currentState == "AVAILABLE"
            book.currentBranch == branch
            book.currentPatron == null
    }

    def 'should place book on hold and increment version'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            PatronId patron = anyPatronId()
            LibraryBranchId branch = anyBranch()
            Instant holdTill = Instant.now().plusSeconds(3600)

        when:
            book.placeOnHold(patron, branch, holdTill)

        then:
            book.currentState == "ON_HOLD"
            book.currentPatron == patron
            book.currentBranch == branch
            book.version == new Version(1)
    }

    def 'should checkout book from available state and increment version'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            PatronId patron = anyPatronId()
            LibraryBranchId branch = anyBranch()

        when:
            book.checkout(patron, branch)

        then:
            book.currentState == "CHECKED_OUT"
            book.currentPatron == patron
            book.currentBranch == branch
            book.version == new Version(1)
    }

    def 'should checkout book from on hold state by same patron and increment version'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            PatronId patron = anyPatronId()
            LibraryBranchId holdBranch = anyBranch()
            LibraryBranchId checkoutBranch = anyBranch()
            book.placeOnHold(patron, holdBranch, Instant.now().plusSeconds(3600))

        when:
            book.checkout(patron, checkoutBranch)

        then:
            book.currentState == "CHECKED_OUT"
            book.currentPatron == patron
            book.currentBranch == checkoutBranch
            book.version == new Version(2)
    }

    def 'should not checkout book from on hold state by different patron'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            PatronId holdPatron = anyPatronId()
            PatronId differentPatron = anyPatronId()
            book.placeOnHold(holdPatron, anyBranch(), Instant.now().plusSeconds(3600))

        when:
            book.checkout(differentPatron, anyBranch())

        then:
            book.currentState == "ON_HOLD"
            book.currentPatron == holdPatron
            book.version == new Version(1)
    }

    def 'should return book and increment version'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            PatronId patron = anyPatronId()
            LibraryBranchId checkoutBranch = anyBranch()
            LibraryBranchId returnBranch = anyBranch()
            book.checkout(patron, checkoutBranch)

        when:
            book.returnBook(returnBranch)

        then:
            book.currentState == "AVAILABLE"
            book.currentPatron == null
            book.currentBranch == returnBranch
            book.version == new Version(2)
    }

    def 'should cancel hold and increment version'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            PatronId patron = anyPatronId()
            LibraryBranchId branch = anyBranch()
            book.placeOnHold(patron, branch, Instant.now().plusSeconds(3600))

        when:
            book.cancelHold()

        then:
            book.currentState == "AVAILABLE"
            book.currentPatron == null
            book.version == new Version(2)
    }

    def 'should expire hold and increment version when hold period passed'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            PatronId patron = anyPatronId()
            LibraryBranchId branch = anyBranch()
            Instant holdTill = Instant.now().minusSeconds(3600)
            book.placeOnHold(patron, branch, holdTill)

        when:
            book.expireHold()

        then:
            book.currentState == "AVAILABLE"
            book.currentPatron == null
            book.version == new Version(2)
    }

    def 'should not change state when expiring hold that has not expired yet'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            PatronId patron = anyPatronId()
            LibraryBranchId branch = anyBranch()
            Instant holdTill = Instant.now().plusSeconds(3600)
            book.placeOnHold(patron, branch, holdTill)

        when:
            book.expireHold()

        then:
            book.currentState == "ON_HOLD"
            book.currentPatron == patron
            book.version == new Version(2)
    }

    def 'should increment version multiple times across transitions'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            PatronId patron = anyPatronId()

        when:
            book.placeOnHold(patron, anyBranch(), Instant.now().plusSeconds(3600))
            book.checkout(patron, anyBranch())
            book.returnBook(anyBranch())

        then:
            book.version == new Version(3)
            book.currentState == "AVAILABLE"
    }
}
