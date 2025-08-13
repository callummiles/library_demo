package io.pillopl.library.lending.dailysheet.infrastructure

import io.pillopl.library.catalogue.BookId
import io.pillopl.library.lending.LendingTestContext
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId
import io.pillopl.library.lending.patron.model.PatronEvent
import io.pillopl.library.lending.patron.model.PatronId
import io.pillopl.library.lending.patron.model.PatronType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import spock.lang.Specification

import javax.sql.DataSource
import java.time.Duration
import java.time.Instant

import static io.pillopl.library.catalogue.BookType.Restricted
import static io.pillopl.library.lending.book.model.BookFixture.anyBookId
import static io.pillopl.library.lending.librarybranch.model.LibraryBranchFixture.anyBranch
import static io.pillopl.library.lending.patron.model.PatronFixture.anyPatronId
import static java.time.Clock.fixed
import static java.time.Instant.now
import static java.time.ZoneId.systemDefault

/**
 * Integration test for daily sheet overdue checkout tracking and detection.
 * 
 * This test verifies that the daily sheet read model correctly tracks checkouts
 * and identifies which checkouts are overdue. The daily sheet is an essential
 * operational tool for library staff to manage overdue books and follow up
 * with patrons who have not returned books on time.
 * 
 * The test covers scenarios including overdue checkouts, future due dates,
 * and the effects of book returns on the overdue checkout reporting.
 */
@SpringBootTest(classes = LendingTestContext.class)
class FindingOverdueCheckoutsInDailySheetDatabaseIT extends Specification {

    PatronId patronId = anyPatronId()
    PatronType regular = PatronType.Regular
    LibraryBranchId libraryBranchId = anyBranch()
    BookId bookId = anyBookId()

    static final Instant TIME_OF_EXPIRE_CHECK = now()

    @Autowired
    DataSource dataSource

    SheetsReadModel readModel

    def setup() {
        readModel = new SheetsReadModel(new JdbcTemplate(dataSource), fixed(TIME_OF_EXPIRE_CHECK, systemDefault()))
    }

    /**
     * Verifies that the daily sheet correctly identifies overdue checkouts.
     * This test creates checkouts with different due dates and ensures that
     * only checkouts that are overdue (due date in the past) are included
     * in the overdue checkouts count, while future due dates are excluded.
     */
    def 'should find overdue checkouts'() {
        given:
            int currentNoOfOverdueCheckouts = readModel.queryForCheckoutsToOverdue().count()
        when:
            readModel.handle(bookCheckedOut(tillYesterday()))
        and:
            readModel.handle(bookCheckedOut(tillTomorrow()))
        then:
            readModel.queryForCheckoutsToOverdue().count() == currentNoOfOverdueCheckouts + 1
    }

    /**
     * Verifies that processing the same BookCheckedOut event multiple times
     * does not create duplicate entries in the daily sheet. This ensures
     * idempotent event processing, which is crucial for reliable event-driven
     * systems where events might be replayed or processed multiple times.
     */
    def 'handling bookCheckedOut should de idempotent'() {
        given:
            int currentNoOfOverdueCheckouts = readModel.queryForCheckoutsToOverdue().count()
        and:
		PatronEvent.BookCheckedOut event = bookCheckedOut(tillYesterday())
        when:
            2.times { readModel.handle(event) }
        then:
            readModel.queryForCheckoutsToOverdue().count() == currentNoOfOverdueCheckouts + 1
    }

    /**
     * Verifies that returned books are removed from the overdue checkouts report.
     * When a book is returned, it should no longer appear in daily operational
     * reports for overdue checkouts, ensuring that library staff only see
     * books that are still checked out and overdue.
     */
    def 'should never find returned books'() {
        given:
            int currentNoOfOverdueCheckouts = readModel.queryForCheckoutsToOverdue().count()
        and:
            readModel.handle(bookCheckedOut(tillTomorrow()))
        when:
            readModel.handle(bookReturned())
        then:
            readModel.queryForCheckoutsToOverdue().count() == currentNoOfOverdueCheckouts
    }


    Instant tillTomorrow() {
        return TIME_OF_EXPIRE_CHECK.plus(Duration.ofDays(1))
    }

    Instant tillYesterday() {
        return TIME_OF_EXPIRE_CHECK.minus(Duration.ofDays(1))
    }

    Instant anOpenEndedHold() {
        return null
    }

    PatronEvent.BookPlacedOnHold placedOnHold(Instant till) {
        return new PatronEvent.BookPlacedOnHold(
                now(),
                patronId.getPatronId(),
                bookId.getBookId(),
                Restricted,
                libraryBranchId.getLibraryBranchId(),
                TIME_OF_EXPIRE_CHECK.minusSeconds(60000),
                till)
    }

    PatronEvent.BookHoldCanceled holdCanceled() {
        return new PatronEvent.BookHoldCanceled(
                now(),
                patronId.getPatronId(),
                bookId.getBookId(),
                libraryBranchId.getLibraryBranchId())
    }

    PatronEvent.BookHoldExpired holdExpired() {
        return new PatronEvent.BookHoldExpired(
                now(),
                patronId.getPatronId(),
                bookId.getBookId(),
                libraryBranchId.getLibraryBranchId())
    }

	PatronEvent.BookCheckedOut bookCheckedOut(Instant till) {
        return new PatronEvent.BookCheckedOut(
                now(),
                patronId.getPatronId(),
                bookId.getBookId(),
                Restricted,
                libraryBranchId.getLibraryBranchId(),
                till)
    }

    PatronEvent.BookReturned bookReturned() {
        return new PatronEvent.BookReturned(
                now(),
                patronId.getPatronId(),
                bookId.getBookId(),
                Restricted,
                libraryBranchId.getLibraryBranchId())
    }

}
