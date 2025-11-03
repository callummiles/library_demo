package io.pillopl.library.lending.book.new_model;

import io.pillopl.library.lending.librarybranch.model.LibraryBranchId;
import io.pillopl.library.lending.patron.model.PatronId;
import io.pillopl.library.commons.aggregates.Version;
import lombok.RequiredArgsConstructor;
import java.time.Instant;

/**
 * Represents a book that is available for checkout or hold.
 * 
 * <p>Valid transitions from this state:
 * <ul>
 *   <li>To OnHoldState - when a patron places a hold</li>
 *   <li>To CheckedOutState - when a patron checks out the book directly</li>
 * </ul>
 * 
 * <p>Business rules enforced:
 * <ul>
 *   <li>Any patron can place a hold on an available book</li>
 *   <li>Any patron can check out an available book</li>
 *   <li>Cannot return an available book (not checked out)</li>
 *   <li>Cannot cancel hold on an available book (no hold exists)</li>
 * </ul>
 */
@RequiredArgsConstructor
public class AvailableState implements BookState {
    private final Book book;
    private final LibraryBranchId branch;

    @Override
    public BookState placeOnHold(PatronId patronId, LibraryBranchId branchId, Instant holdTill) {
        return new OnHoldState(book, branchId, patronId, holdTill);
    }

    @Override
    public BookState checkout(PatronId patronId, LibraryBranchId branchId) {
        return new CheckedOutState(book, branchId, patronId);
    }

    @Override
    public BookState returnBook(LibraryBranchId branchId) {
        return invalidTransition("Cannot return an available book");
    }

    @Override
    public BookState cancelHold() {
        return invalidTransition("Cannot cancel hold on an available book");
    }

    @Override
    public BookState expireHold() {
        return invalidTransition("Cannot expire hold on an available book");
    }

    @Override
    public boolean canBeCheckedOut(PatronId patronId) {
        return true;
    }

    @Override
    public boolean canBePutOnHold(PatronId patronId) {
        return true;
    }

    @Override
    public boolean canBeReturned() {
        return false;
    }

    @Override
    public String getStateName() {
        return "AVAILABLE";
    }

    @Override
    public Version getVersion() {
        return book.getVersion();
    }

    @Override
    public LibraryBranchId getCurrentBranch() {
        return branch;
    }

    @Override
    public PatronId getCurrentPatron() {
        return null;
    }
}
