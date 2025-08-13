package io.pillopl.library.lending.patron.infrastructure


import io.pillopl.library.lending.LendingTestContext
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId
import io.pillopl.library.lending.patron.model.HoldDuration
import io.pillopl.library.lending.patron.model.Patron
import io.pillopl.library.lending.patron.model.Patrons
import io.pillopl.library.lending.patron.model.PatronId
import io.pillopl.library.lending.patron.model.PatronType
import io.vavr.control.Option
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import static io.pillopl.library.catalogue.BookType.Circulating
import static io.pillopl.library.lending.book.model.BookFixture.anyBookId
import static io.pillopl.library.lending.librarybranch.model.LibraryBranchFixture.anyBranch
import static io.pillopl.library.lending.patron.model.PatronEvent.BookPlacedOnHold.bookPlacedOnHoldNow
import static io.pillopl.library.lending.patron.model.PatronEvent.BookPlacedOnHoldEvents
import static io.pillopl.library.lending.patron.model.PatronEvent.BookPlacedOnHoldEvents.events
import static io.pillopl.library.lending.patron.model.PatronEvent.PatronCreated
import static io.pillopl.library.lending.patron.model.PatronFixture.anyPatronId
import static io.pillopl.library.lending.patron.model.PatronFixture.regularPatron
import static io.pillopl.library.lending.patron.model.PatronType.Regular

/**
 * Integration test for patron aggregate persistence operations.
 * 
 * This test verifies that patron aggregates can be properly persisted to and
 * retrieved from the database through domain events. It tests the event-driven
 * persistence model where patron state changes are captured as events and
 * the patron repository can reconstruct patron aggregates from these events.
 * 
 * The test ensures that patron creation and hold placement events are correctly
 * processed and that the resulting patron aggregate reflects the expected state
 * when retrieved from the repository.
 */
@SpringBootTest(classes = LendingTestContext.class)
class PatronDatabaseRepositoryIT extends Specification {

    PatronId patronId = anyPatronId()
    PatronType regular = Regular
    LibraryBranchId libraryBranchId = anyBranch()

    @Autowired
    Patrons patronRepo

    /**
     * Verifies that patron aggregates can be persisted and retrieved through
     * domain events. This test publishes patron creation and book hold events,
     * then verifies that the patron can be retrieved from the repository with
     * the correct state, including the number of holds. This ensures the
     * event-driven persistence model works correctly for patron aggregates.
     */
    def 'persistence in real database should work'() {
        when:
            patronRepo.publish(patronCreated())
        then:
            patronShouldBeFoundInDatabaseWithZeroBooksOnHold(patronId)
        when:
            patronRepo.publish(placedOnHold())
        then:
            patronShouldBeFoundInDatabaseWithOneBookOnHold(patronId)
    }

    BookPlacedOnHoldEvents placedOnHold() {
        return events(bookPlacedOnHoldNow(
                anyBookId(),
                Circulating,
                libraryBranchId,
                patronId,
                HoldDuration.closeEnded(5)))
    }

    PatronCreated patronCreated() {
        return PatronCreated.now(patronId, Regular)
    }

    void patronShouldBeFoundInDatabaseWithOneBookOnHold(PatronId patronId) {
        Patron patron = loadPersistedPatron(patronId)
        assert patron.numberOfHolds() == 1
        assertPatronInformation(patron, patronId)
    }

    void patronShouldBeFoundInDatabaseWithZeroBooksOnHold(PatronId patronId) {
        Patron patron = loadPersistedPatron(patronId)
        assert patron.numberOfHolds() == 0
        assertPatronInformation(patron, patronId)

    }

    void assertPatronInformation(Patron patron, PatronId patronId) {
        assert patron.equals(regularPatron(patronId))
    }

    Patron loadPersistedPatron(PatronId patronId) {
        Option<Patron> loaded = patronRepo.findBy(patronId)
        Patron patron = loaded.getOrElseThrow({
            new IllegalStateException("should have been persisted")
        })
        return patron
    }
}
