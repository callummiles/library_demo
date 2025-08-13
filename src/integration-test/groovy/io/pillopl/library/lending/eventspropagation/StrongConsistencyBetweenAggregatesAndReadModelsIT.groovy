package io.pillopl.library.lending.eventspropagation

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
 * Integration test for strong consistency between aggregates and read models.
 * 
 * This test verifies that when domain events are published, all related aggregates
 * and read models are updated synchronously within the same transaction. It ensures
 * that when a patron places a hold on a book, the patron aggregate, book aggregate,
 * and daily sheet read model are all updated consistently and immediately.
 * 
 * Strong consistency is crucial for maintaining data integrity across different
 * parts of the system, ensuring that all views of the data remain synchronized
 * after each operation.
 */
@SpringBootTest(classes = LendingTestContext.class)
class StrongConsistencyBetweenAggregatesAndReadModelsIT extends Specification {

    PatronId patronId = anyPatronId()
    LibraryBranchId libraryBranchId = anyBranch()
    AvailableBook book = BookFixture.circulatingBook()

    @Autowired
    Patrons patronRepo

    @Autowired
    BookRepository bookRepository

    @Autowired
    DataSource datasource

    /**
     * Verifies that domain events properly synchronize all related aggregates and
     * read models in a strongly consistent manner. When a patron places a hold on
     * a book, this test ensures that the patron aggregate is updated with the hold,
     * the book aggregate transitions to BookOnHold state, and the daily sheet
     * read model is updated with the new hold information, all within the same
     * transaction boundary.
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

    boolean bookReactedToPlacedOnHoldEvent() {
        return bookRepository.findBy(book.bookId).get() instanceof BookOnHold
    }

    boolean dailySheetIsUpdated() {
        return new JdbcTemplate(datasource).query("select count(*) from holds_sheet s where s.hold_by_patron_id = ?",
                [patronId.patronId] as Object[],
                new ColumnMapRowMapper()).get(0)
                .get("COUNT(*)") == 1
    }

    BookPlacedOnHoldEvents placedOnHold(AvailableBook book) {
        return events(bookPlacedOnHoldNow(
                book.bookId,
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
