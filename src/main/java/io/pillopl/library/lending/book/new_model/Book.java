package io.pillopl.library.lending.book.new_model;

import io.pillopl.library.catalogue.BookId;
import io.pillopl.library.catalogue.BookType;
import io.pillopl.library.commons.aggregates.Version;
import io.pillopl.library.lending.book.model.BookInformation;
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId;
import io.pillopl.library.lending.patron.model.PatronId;
import lombok.Getter;
import java.time.Instant;

@Getter
public class Book implements io.pillopl.library.lending.book.model.Book {
    private final BookId bookId;
    private final BookType bookType;
    private final BookInformation bookInformation;
    private BookState state;
    private Version version;

    public Book(BookId bookId, BookType bookType, LibraryBranchId branch, Version version) {
        this.bookId = bookId;
        this.bookType = bookType;
        this.bookInformation = new BookInformation(bookId, bookType);
        this.version = version;
        this.state = new AvailableState(this, branch);
    }

    // State transition methods
    public void placeOnHold(PatronId patronId, LibraryBranchId branchId, Instant holdTill) {
        if (state.canBePutOnHold(patronId)) {
            state = state.placeOnHold(patronId, branchId, holdTill);
            version = version.next();
        }
    }

    public void checkout(PatronId patronId, LibraryBranchId branchId) {
        if (state.canBeCheckedOut(patronId)) {
            state = state.checkout(patronId, branchId);
            version = version.next();
        }
    }

    public void returnBook(LibraryBranchId branchId) {
        if (state.canBeReturned()) {
            state = state.returnBook(branchId);
            version = version.next();
        }
    }

    public void cancelHold() {
        state = state.cancelHold();
        version = version.next();
    }

    public void expireHold() {
        state = state.expireHold();
        version = version.next();
    }

    // State information
    public String getCurrentState() {
        return state.getStateName();
    }

    public LibraryBranchId getCurrentBranch() {
        return state.getCurrentBranch();
    }

    public PatronId getCurrentPatron() {
        return state.getCurrentPatron();
    }
}
