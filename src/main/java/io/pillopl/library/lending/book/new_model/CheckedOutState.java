package io.pillopl.library.lending.book.new_model;

import io.pillopl.library.lending.librarybranch.model.LibraryBranchId;
import io.pillopl.library.lending.patron.model.PatronId;
import io.pillopl.library.commons.aggregates.Version;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.Instant;

/**
 * Represents a book that is checked out to a patron.
 * 
 * <p>Valid transitions from this state:
 * <ul>
 *   <li>To AvailableState - when the book is returned</li>
 * </ul>
 * 
 * <p>Business rules enforced:
 * <ul>
 *   <li>Cannot place a hold on a checked out book</li>
 *   <li>Cannot check out a book that is already checked out</li>
 *   <li>Can only be returned to make it available again</li>
 *   <li>Cannot cancel hold (no hold exists when checked out)</li>
 *   <li>Cannot expire hold (no hold exists when checked out)</li>
 * </ul>
 */
@RequiredArgsConstructor
@Getter
public class CheckedOutState implements BookState {
    private final Book book;
    private final LibraryBranchId checkedOutAt;
    private final PatronId byPatron;

    @Override
    public BookState placeOnHold(PatronId patronId, LibraryBranchId branchId, Instant holdTill) {
        return invalidTransition("Cannot place a hold on a checked out book");
    }

    @Override
    public BookState checkout(PatronId patronId, LibraryBranchId branchId) {
        return invalidTransition("Book is already checked out");
    }

    @Override
    public BookState returnBook(LibraryBranchId branchId) {
        return new AvailableState(book, branchId);
    }

    @Override
    public BookState cancelHold() {
        return invalidTransition("Cannot cancel hold on a checked out book");
    }

    @Override
    public BookState expireHold() {
        return invalidTransition("Cannot expire hold on a checked out book");
    }

    @Override
    public boolean canBeCheckedOut(PatronId patronId) {
        return false;
    }

    @Override
    public boolean canBePutOnHold(PatronId patronId) {
        return false;
    }

    @Override
    public boolean canBeReturned() {
        return true;
    }

    @Override
    public String getStateName() {
        return "CHECKED_OUT";
    }

    @Override
    public Version getVersion() {
        return book.getVersion();
    }

    @Override
    public LibraryBranchId getCurrentBranch() {
        return checkedOutAt;
    }

    @Override
    public PatronId getCurrentPatron() {
        return byPatron;
    }
}
