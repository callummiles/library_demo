package io.pillopl.library.lending.book.new_model

import io.pillopl.library.catalogue.BookType
import io.pillopl.library.commons.aggregates.Version
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId
import io.pillopl.library.lending.patron.model.PatronId
import spock.lang.Specification

import static io.pillopl.library.lending.book.model.BookFixture.anyBookId
import static io.pillopl.library.lending.librarybranch.model.LibraryBranchFixture.anyBranch
import static io.pillopl.library.lending.patron.model.PatronFixture.anyPatronId

class CheckedOutStateTest extends Specification {

    Book book
    CheckedOutState checkedOutState
    PatronId checkoutPatron
    LibraryBranchId checkoutBranch

    def setup() {
        checkoutPatron = anyPatronId()
        checkoutBranch = anyBranch()
        book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
        book.checkout(checkoutPatron, checkoutBranch)
        checkedOutState = book.state as CheckedOutState
    }

    def 'should not allow placing hold on checked out book'() {
        when:
            checkedOutState.placeOnHold(anyPatronId(), anyBranch(), java.time.Instant.now().plusSeconds(3600))

        then:
            thrown(IllegalStateException)
    }

    def 'should not allow checking out already checked out book'() {
        when:
            checkedOutState.checkout(anyPatronId(), anyBranch())

        then:
            thrown(IllegalStateException)
    }

    def 'should allow returning book'() {
        given:
            LibraryBranchId returnBranch = anyBranch()

        when:
            BookState newState = checkedOutState.returnBook(returnBranch)

        then:
            newState instanceof AvailableState
            newState.stateName == "AVAILABLE"
    }

    def 'should not allow canceling hold on checked out book'() {
        when:
            checkedOutState.cancelHold()

        then:
            thrown(IllegalStateException)
    }

    def 'should not allow expiring hold on checked out book'() {
        when:
            checkedOutState.expireHold()

        then:
            thrown(IllegalStateException)
    }

    def 'should not allow checking out'() {
        expect:
            checkedOutState.canBeCheckedOut(anyPatronId()) == false
    }

    def 'should not allow placing hold'() {
        expect:
            checkedOutState.canBePutOnHold(anyPatronId()) == false
    }

    def 'should be returnable'() {
        expect:
            checkedOutState.canBeReturned() == true
    }

    def 'should have correct state name'() {
        expect:
            checkedOutState.stateName == "CHECKED_OUT"
    }

    def 'should return correct current branch'() {
        expect:
            checkedOutState.currentBranch == checkoutBranch
    }

    def 'should return correct current patron'() {
        expect:
            checkedOutState.currentPatron == checkoutPatron
    }
}
