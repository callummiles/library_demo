package io.pillopl.library.lending.eventspropagation

import io.pillopl.library.common.events.publisher.DomainEventsTestConfig
import io.pillopl.library.lending.LendingTestContext
import io.pillopl.library.lending.book.model.AvailableBook
import io.pillopl.library.lending.book.model.BookFixture
import io.pillopl.library.lending.book.model.BookOnHold
import io.pillopl.library.lending.book.model.BookRepository
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId
import io.pillopl.library.lending.patron.model.HoldDuration
import io.pillopl.library.lending.patron.model.Patron
import io.pillopl.library.lending.patron.model.Patrons
import io.pillopl.library.lending.patron.model.PatronId
import io.vavr.control.Option
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.sql.DataSource

import static io.pillopl.library.lending.librarybranch.model.LibraryBranchFixture.anyBranch
import static io.pillopl.library.lending.patron.model.PatronEvent.BookPlacedOnHold.bookPlacedOnHoldNow
import static io.pillopl.library.lending.patron.model.PatronEvent.BookPlacedOnHoldEvents
import static io.pillopl.library.lending.patron.model.PatronEvent.BookPlacedOnHoldEvents.events
import static io.pillopl.library.lending.patron.model.PatronEvent.PatronCreated
import static io.pillopl.library.lending.patron.model.PatronFixture.anyPatronId
import static io.pillopl.library.lending.patron.model.PatronFixture.regularPatron
import static io.pillopl.library.lending.patron.model.PatronType.Regular

/**
 * Integration test for eventual consistency between aggregates and read models.
 * 
 * This test verifies that when domain events are published asynchronously,
 * all related aggregates and read models eventually reach a consistent state.
 * Unlike strong consistency, this test uses polling conditions to wait for
 * the asynchronous event processing to complete.
 * 
 * Eventual consistency is important for scenarios where immediate consistency
 * is not required and the system can tolerate brief periods where different
 * parts of the system may have slightly different views of the data.
 */
@SpringBootTest(classes = [LendingTestContext.class, DomainEventsTestConfig.class])
class EventualConsistencyBetweenAggregatesAndReadModelsIT extends Specification {

    PatronId patronId = anyPatronId()
    LibraryBranchId libraryBranchId = anyBranch()
    AvailableBook book = BookFixture.circulatingBook()

    @Autowired
    Patrons patronRepo

    @Autowired
    BookRepository bookRepository

    @Autowired
    DataSource datasource

    PollingConditions pollingConditions = new PollingConditions(timeout: 6)

    /**
     * Verifies that domain events eventually synchronize all related aggregates
     * and read models through asynchronous processing. This test uses polling
     * conditions to wait for the eventual consistency to be achieved, ensuring
     * that the book aggregate transitions to BookOnHold state and the daily sheet
     * is updated, even when events are processed asynchronously.
     */
    def 'should synchronize Patron, Book and DailySheet with events'() {
        given:
            bookRepository.save(book)
        and:
            patronRepo.publish(patronCreated())
        when:
            patronRepo.publish(placedOnHold(book))
        then:
            patronShouldBeFoundInDatabaseWithOneBookOnHold(patronId)
        and:
            bookReactedToPlacedOnHoldEvent()
        and:
            dailySheetIsUpdated()
    }

    void bookReactedToPlacedOnHoldEvent() {
        pollingConditions.eventually {
            assert bookRepository.findBy(book.bookId).get() instanceof BookOnHold
        }
    }

    void dailySheetIsUpdated() {
        pollingConditions.eventually {
            assert countOfHoldsInDailySheet() == 1
        }
    }

    private Object countOfHoldsInDailySheet() {
        return new JdbcTemplate(datasource).query("select count(*) from holds_sheet s where s.hold_by_patron_id = ?",
                [patronId.patronId] as Object[],
                new ColumnMapRowMapper()).get(0)
                .get("COUNT(*)")
    }

    BookPlacedOnHoldEvents placedOnHold(AvailableBook book) {
        return events(bookPlacedOnHoldNow(
                book.getBookId(),
                book.type(),
                book.libraryBranch,
                patronId,
                HoldDuration.closeEnded(5)))
    }

    PatronCreated patronCreated() {
        return PatronCreated.now(patronId, Regular)
    }

    void patronShouldBeFoundInDatabaseWithOneBookOnHold(PatronId patronId) {
        Patron patron = loadPersistedPatron(patronId)
        assert patron.numberOfHolds() == 1
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
