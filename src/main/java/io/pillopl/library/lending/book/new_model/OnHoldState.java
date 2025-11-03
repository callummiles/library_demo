package io.pillopl.library.lending.book.new_model;

import io.pillopl.library.lending.librarybranch.model.LibraryBranchId;
import io.pillopl.library.lending.patron.model.PatronId;
import io.pillopl.library.commons.aggregates.Version;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.Instant;

/**
 * Represents a book that is on hold for a specific patron.
 * 
 * <p>Valid transitions from this state:
 * <ul>
 *   <li>To CheckedOutState - when the patron who placed the hold checks out the book</li>
 *   <li>To AvailableState - when the hold is cancelled or expires</li>
 * </ul>
 * 
 * <p>Business rules enforced:
 * <ul>
 *   <li>Only the patron who placed the hold can check out the book</li>
 *   <li>Cannot place another hold while book is already on hold</li>
 *   <li>Cannot return a book that is on hold (not checked out)</li>
 *   <li>Hold can be cancelled to make book available again</li>
 *   <li>Hold automatically expires if current time is after holdTill</li>
 * </ul>
 * 
 * <p>Validation logic:
 * <ul>
 *   <li>Checkout validation checks that patron ID matches the hold patron</li>
 *   <li>Expire validation checks if current time is after hold expiration time</li>
 * </ul>
 */
@RequiredArgsConstructor
@Getter
public class OnHoldState implements BookState {
    private final Book book;
    private final LibraryBranchId holdPlacedAt;
    private final PatronId byPatron;
    private final Instant holdTill;

    @Override
    public BookState placeOnHold(PatronId patronId, LibraryBranchId branchId, Instant holdTill) {
        return invalidTransition("Book is already on hold");
    }

    @Override
    public BookState checkout(PatronId patronId, LibraryBranchId branchId) {
        if (!patronId.equals(byPatron)) {
            return invalidTransition("Only the patron who placed the hold can check out the book");
        }
        return new CheckedOutState(book, branchId, patronId);
    }

    @Override
    public BookState returnBook(LibraryBranchId branchId) {
        return invalidTransition("Cannot return a book that is on hold");
    }

    @Override
    public BookState cancelHold() {
        return new AvailableState(book, holdPlacedAt);
    }

    @Override
    public BookState expireHold() {
        if (Instant.now().isAfter(holdTill)) {
            return new AvailableState(book, holdPlacedAt);
        }
        return this;
    }

    @Override
    public boolean canBeCheckedOut(PatronId patronId) {
        return patronId.equals(byPatron);
    }

    @Override
    public boolean canBePutOnHold(PatronId patronId) {
        return false;
    }

    @Override
    public boolean canBeReturned() {
        return false;
    }

    @Override
    public String getStateName() {
        return "ON_HOLD";
    }

    @Override
    public Version getVersion() {
        return book.getVersion();
    }

    @Override
    public LibraryBranchId getCurrentBranch() {
        return holdPlacedAt;
    }

    @Override
    public PatronId getCurrentPatron() {
        return byPatron;
    }
}
