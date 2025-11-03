package io.pillopl.library.lending.book.new_model

import io.pillopl.library.catalogue.BookType
import io.pillopl.library.commons.aggregates.Version
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId
import io.pillopl.library.lending.patron.model.PatronId
import spock.lang.Specification

import java.time.Instant

import static io.pillopl.library.lending.book.model.BookFixture.anyBookId
import static io.pillopl.library.lending.librarybranch.model.LibraryBranchFixture.anyBranch
import static io.pillopl.library.lending.patron.model.PatronFixture.anyPatronId

class BookStateTest extends Specification {

    def 'should transition from Available to OnHold'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            PatronId patron = anyPatronId()
            LibraryBranchId branch = anyBranch()

        when:
            book.placeOnHold(patron, branch, Instant.now().plusSeconds(3600))

        then:
            book.state instanceof OnHoldState
            book.currentState == "ON_HOLD"
    }

    def 'should transition from Available to CheckedOut'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            PatronId patron = anyPatronId()

        when:
            book.checkout(patron, anyBranch())

        then:
            book.state instanceof CheckedOutState
            book.currentState == "CHECKED_OUT"
    }

    def 'should transition from OnHold to CheckedOut by same patron'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            PatronId patron = anyPatronId()
            book.placeOnHold(patron, anyBranch(), Instant.now().plusSeconds(3600))

        when:
            book.checkout(patron, anyBranch())

        then:
            book.state instanceof CheckedOutState
            book.currentState == "CHECKED_OUT"
    }

    def 'should transition from OnHold to Available via cancelHold'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            book.placeOnHold(anyPatronId(), anyBranch(), Instant.now().plusSeconds(3600))

        when:
            book.cancelHold()

        then:
            book.state instanceof AvailableState
            book.currentState == "AVAILABLE"
    }

    def 'should transition from OnHold to Available via expireHold'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            book.placeOnHold(anyPatronId(), anyBranch(), Instant.now().minusSeconds(3600))

        when:
            book.expireHold()

        then:
            book.state instanceof AvailableState
            book.currentState == "AVAILABLE"
    }

    def 'should transition from CheckedOut to Available'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            book.checkout(anyPatronId(), anyBranch())

        when:
            book.returnBook(anyBranch())

        then:
            book.state instanceof AvailableState
            book.currentState == "AVAILABLE"
    }

    def 'should complete full lifecycle: Available -> OnHold -> CheckedOut -> Available'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            PatronId patron = anyPatronId()

        when:
            book.placeOnHold(patron, anyBranch(), Instant.now().plusSeconds(3600))

        then:
            book.currentState == "ON_HOLD"
            book.version == new Version(1)

        when:
            book.checkout(patron, anyBranch())

        then:
            book.currentState == "CHECKED_OUT"
            book.version == new Version(2)

        when:
            book.returnBook(anyBranch())

        then:
            book.currentState == "AVAILABLE"
            book.version == new Version(3)
    }

    def 'should handle multiple holds and cancellations'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            PatronId patron1 = anyPatronId()
            PatronId patron2 = anyPatronId()

        when:
            book.placeOnHold(patron1, anyBranch(), Instant.now().plusSeconds(3600))
            book.cancelHold()
            book.placeOnHold(patron2, anyBranch(), Instant.now().plusSeconds(3600))

        then:
            book.currentState == "ON_HOLD"
            book.currentPatron == patron2
            book.version == new Version(3)
    }

    def 'should reject invalid transition from OnHold to OnHold'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            book.placeOnHold(anyPatronId(), anyBranch(), Instant.now().plusSeconds(3600))
            BookState state = book.state

        when:
            state.placeOnHold(anyPatronId(), anyBranch(), Instant.now().plusSeconds(3600))

        then:
            thrown(IllegalStateException)
    }

    def 'should reject invalid transition from CheckedOut to OnHold'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            book.checkout(anyPatronId(), anyBranch())
            BookState state = book.state

        when:
            state.placeOnHold(anyPatronId(), anyBranch(), Instant.now().plusSeconds(3600))

        then:
            thrown(IllegalStateException)
    }

    def 'should reject invalid transition from CheckedOut to CheckedOut'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            book.checkout(anyPatronId(), anyBranch())
            BookState state = book.state

        when:
            state.checkout(anyPatronId(), anyBranch())

        then:
            thrown(IllegalStateException)
    }

    def 'should reject invalid transition from Available to return'() {
        given:
            Book book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            BookState state = book.state

        when:
            state.returnBook(anyBranch())

        then:
            thrown(IllegalStateException)
    }
}
