package io.pillopl.library.lending.dailysheet.infrastructure

import io.pillopl.library.catalogue.BookId
import io.pillopl.library.catalogue.BookType
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
 * Integration test for daily sheet holds tracking and expiration detection.
 * 
 * This test verifies that the daily sheet read model correctly tracks holds
 * and identifies which holds are expired or about to expire. The daily sheet
 * is a crucial operational tool for library staff to manage expired holds
 * and maintain efficient library operations.
 * 
 * The test covers various scenarios including expired holds, future holds,
 * open-ended holds, and the effects of hold cancellations, expirations,
 * and checkouts on the daily sheet reporting.
 */
@SpringBootTest(classes = LendingTestContext.class)
class FindingHoldsInDailySheetDatabaseIT extends Specification {

    PatronId patronId = anyPatronId()
    PatronType regular = PatronType.Regular
    LibraryBranchId libraryBranchId = anyBranch()
    BookId bookId = anyBookId()
    BookType type = Restricted

    static final Instant TIME_OF_EXPIRE_CHECK = now()

    @Autowired
    DataSource dataSource

    SheetsReadModel readModel

    def setup() {
        readModel = new SheetsReadModel(new JdbcTemplate(dataSource), fixed(TIME_OF_EXPIRE_CHECK, systemDefault()))
    }

    /**
     * Verifies that the daily sheet correctly identifies expired holds.
     * This test places holds with different expiration dates and ensures
     * that only holds that have expired (expiration date in the past)
     * are included in the expired holds count, while future holds are excluded.
     */
    def 'should find expired holds'() {
        given:
            int currentNoOfExpiredHolds = readModel.queryForHoldsToExpireSheet().count()
        when:
            readModel.handle(placedOnHold(aCloseEndedHoldTillYesterday()))
        and:
            readModel.handle(placedOnHold(aCloseEndedHoldTillTomorrow()))
        then:
            readModel.queryForHoldsToExpireSheet().count() == currentNoOfExpiredHolds + 1
    }

    /**
     * Verifies that processing the same BookPlacedOnHold event multiple times
     * does not create duplicate entries in the daily sheet. This ensures
     * idempotent event processing, which is crucial for reliable event-driven
     * systems where events might be replayed or processed multiple times.
     */
    def 'handling placed on hold should de idempotent'() {
        given:
            int currentNoOfExpiredHolds = readModel.queryForHoldsToExpireSheet().count()
        and:
            PatronEvent.BookPlacedOnHold event = placedOnHold(aCloseEndedHoldTillYesterday())
        when:
            2.times { readModel.handle(event) }
        then:
            readModel.queryForHoldsToExpireSheet().count() == currentNoOfExpiredHolds + 1
    }

    /**
     * Verifies that open-ended holds (holds without expiration dates) are
     * never included in the expired holds report. Open-ended holds are
     * typically used for researcher patrons and should not appear in
     * daily operational reports for expired holds.
     */
    def 'should never find open-ended holds'() {
        given:
            int currentNoOfExpiredHolds = readModel.queryForHoldsToExpireSheet().count()
        when:
            readModel.handle(placedOnHold(anOpenEndedHold()))
        then:
            readModel.queryForHoldsToExpireSheet().count() == currentNoOfExpiredHolds
    }

    /**
     * Verifies that canceled holds are removed from the expired holds report.
     * When a hold is canceled, it should no longer appear in daily operational
     * reports, ensuring that library staff only see active holds that require
     * attention.
     */
    def 'should never find canceled holds'() {
        given:
            int currentNoOfExpiredHolds = readModel.queryForHoldsToExpireSheet().count()
        when:
            readModel.handle(placedOnHold(aCloseEndedHoldTillYesterday()))
        and:
            readModel.handle(holdCanceled())
        then:
            readModel.queryForHoldsToExpireSheet().count() == currentNoOfExpiredHolds
    }

    /**
     * Verifies that holds that have already been processed as expired are
     * removed from the expired holds report. Once a hold has been marked
     * as expired through the hold expiration process, it should no longer
     * appear in daily operational reports.
     */
    def 'should never find already expired holds'() {
        given:
            int currentNoOfExpiredHolds = readModel.queryForHoldsToExpireSheet().count()
        when:
            readModel.handle(placedOnHold(anOpenEndedHold()))
        and:
            readModel.handle(holdExpired())
        then:
            readModel.queryForHoldsToExpireSheet().count() == currentNoOfExpiredHolds
    }

    /**
     * Verifies that holds that have been fulfilled through book checkout
     * are removed from the expired holds report. Once a patron checks out
     * a book they had on hold, the hold should no longer appear in daily
     * operational reports since it has been successfully completed.
     */
    def 'should never find already checkedOut holds'() {
        given:
            int currentNoOfExpiredHolds = readModel.queryForHoldsToExpireSheet().count()
        when:
            readModel.handle(placedOnHold(aCloseEndedHoldTillYesterday()))
        and:
            readModel.handle(bookCheckedOut())
        then:
            readModel.queryForHoldsToExpireSheet().count() == currentNoOfExpiredHolds
    }


    Instant aCloseEndedHoldTillTomorrow() {
        return TIME_OF_EXPIRE_CHECK.plus(Duration.ofDays(1))
    }

    Instant aCloseEndedHoldTillYesterday() {
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
                type,
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

	PatronEvent.BookCheckedOut bookCheckedOut() {
        return new PatronEvent.BookCheckedOut(
                now(),
                patronId.getPatronId(),
                bookId.getBookId(),
                type,
                libraryBranchId.getLibraryBranchId(),
                now())
    }


}
