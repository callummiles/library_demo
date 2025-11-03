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

class AvailableStateTest extends Specification {

    Book book
    AvailableState availableState
    LibraryBranchId branch

    def setup() {
        branch = anyBranch()
        book = new Book(anyBookId(), BookType.Circulating, branch, Version.zero())
        availableState = book.state as AvailableState
    }

    def 'should allow placing book on hold'() {
        given:
            PatronId patron = anyPatronId()
            Instant holdTill = Instant.now().plusSeconds(3600)

        when:
            BookState newState = availableState.placeOnHold(patron, anyBranch(), holdTill)

        then:
            newState instanceof OnHoldState
            newState.stateName == "ON_HOLD"
    }

    def 'should allow checking out book'() {
        given:
            PatronId patron = anyPatronId()

        when:
            BookState newState = availableState.checkout(patron, anyBranch())

        then:
            newState instanceof CheckedOutState
            newState.stateName == "CHECKED_OUT"
    }

    def 'should not allow returning an available book'() {
        when:
            availableState.returnBook(anyBranch())

        then:
            thrown(IllegalStateException)
    }

    def 'should not allow canceling hold on available book'() {
        when:
            availableState.cancelHold()

        then:
            thrown(IllegalStateException)
    }

    def 'should not allow expiring hold on available book'() {
        when:
            availableState.expireHold()

        then:
            thrown(IllegalStateException)
    }

    def 'should allow any patron to check out'() {
        given:
            PatronId patron = anyPatronId()

        expect:
            availableState.canBeCheckedOut(patron) == true
    }

    def 'should allow any patron to place hold'() {
        given:
            PatronId patron = anyPatronId()

        expect:
            availableState.canBePutOnHold(patron) == true
    }

    def 'should not be returnable'() {
        expect:
            availableState.canBeReturned() == false
    }

    def 'should have correct state name'() {
        expect:
            availableState.stateName == "AVAILABLE"
    }

    def 'should return correct current branch'() {
        expect:
            availableState.currentBranch == branch
    }

    def 'should return null for current patron'() {
        expect:
            availableState.currentPatron == null
    }

    def 'should return book version'() {
        expect:
            availableState.version == Version.zero()
    }
}
