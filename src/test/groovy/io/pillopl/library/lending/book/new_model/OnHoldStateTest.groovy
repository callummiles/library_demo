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

class OnHoldStateTest extends Specification {

    Book book
    OnHoldState onHoldState
    PatronId holdPatron
    LibraryBranchId holdBranch
    Instant holdTill

    def setup() {
        holdPatron = anyPatronId()
        holdBranch = anyBranch()
        holdTill = Instant.now().plusSeconds(3600)
        book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
        book.placeOnHold(holdPatron, holdBranch, holdTill)
        onHoldState = book.state as OnHoldState
    }

    def 'should not allow placing another hold'() {
        when:
            onHoldState.placeOnHold(anyPatronId(), anyBranch(), Instant.now().plusSeconds(3600))

        then:
            thrown(IllegalStateException)
    }

    def 'should allow checkout by patron who placed the hold'() {
        given:
            LibraryBranchId checkoutBranch = anyBranch()

        when:
            BookState newState = onHoldState.checkout(holdPatron, checkoutBranch)

        then:
            newState instanceof CheckedOutState
            newState.stateName == "CHECKED_OUT"
    }

    def 'should not allow checkout by different patron'() {
        given:
            PatronId differentPatron = anyPatronId()

        when:
            onHoldState.checkout(differentPatron, anyBranch())

        then:
            thrown(IllegalStateException)
    }

    def 'should not allow returning a book on hold'() {
        when:
            onHoldState.returnBook(anyBranch())

        then:
            thrown(IllegalStateException)
    }

    def 'should allow canceling hold'() {
        when:
            BookState newState = onHoldState.cancelHold()

        then:
            newState instanceof AvailableState
            newState.stateName == "AVAILABLE"
    }

    def 'should expire hold when hold period has passed'() {
        given:
            Instant pastHoldTill = Instant.now().minusSeconds(3600)
            book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            book.placeOnHold(holdPatron, holdBranch, pastHoldTill)
            onHoldState = book.state as OnHoldState

        when:
            BookState newState = onHoldState.expireHold()

        then:
            newState instanceof AvailableState
            newState.stateName == "AVAILABLE"
    }

    def 'should not expire hold when hold period has not passed'() {
        given:
            Instant futureHoldTill = Instant.now().plusSeconds(3600)
            book = new Book(anyBookId(), BookType.Circulating, anyBranch(), Version.zero())
            book.placeOnHold(holdPatron, holdBranch, futureHoldTill)
            onHoldState = book.state as OnHoldState

        when:
            BookState newState = onHoldState.expireHold()

        then:
            newState == onHoldState
            newState.stateName == "ON_HOLD"
    }

    def 'should allow checkout only by patron who placed the hold'() {
        expect:
            onHoldState.canBeCheckedOut(holdPatron) == true
            onHoldState.canBeCheckedOut(anyPatronId()) == false
    }

    def 'should not allow placing another hold'() {
        expect:
            onHoldState.canBePutOnHold(anyPatronId()) == false
    }

    def 'should not be returnable'() {
        expect:
            onHoldState.canBeReturned() == false
    }

    def 'should have correct state name'() {
        expect:
            onHoldState.stateName == "ON_HOLD"
    }

    def 'should return correct current branch'() {
        expect:
            onHoldState.currentBranch == holdBranch
    }

    def 'should return correct current patron'() {
        expect:
            onHoldState.currentPatron == holdPatron
    }

    def 'should have correct hold expiration time'() {
        expect:
            onHoldState.holdTill == holdTill
    }
}
