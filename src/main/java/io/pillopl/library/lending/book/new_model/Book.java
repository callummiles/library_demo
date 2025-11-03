package io.pillopl.library.lending.book.new_model;

import io.pillopl.library.catalogue.BookId;
import io.pillopl.library.catalogue.BookType;
import io.pillopl.library.commons.aggregates.Version;
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId;
import io.pillopl.library.lending.patron.model.PatronId;
import lombok.Getter;
import java.time.Instant;

/**
 * Aggregate root for a book in the lending system using the State pattern.
 * 
 * <p>This implementation delegates all state-specific behavior to the current BookState implementation.
 * The Book maintains a reference to its current state and transitions between states by replacing
 * the state object. Version is incremented after each successful state transition.
 * 
 * <p>This represents an alternative architecture to the immutable type-based approach in the model/ package,
 * using mutable State pattern with bidirectional references between Book and BookState.
 * 
 * <p>State transitions are validated before execution using the canXXX methods provided by the current state.
 * If validation passes, the state transition occurs and version is incremented via version.next().
 */
@Getter
public class Book {
    private final BookId bookId;
    private final BookType bookType;
    private BookState state;
    private Version version;

    /**
     * Creates a new Book in the Available state.
     * 
     * @param bookId unique identifier for the book
     * @param bookType type of book (Circulating or Restricted)
     * @param branch library branch where book initially resides
     * @param version initial version (typically Version.zero())
     */
    public Book(BookId bookId, BookType bookType, LibraryBranchId branch, Version version) {
        this.bookId = bookId;
        this.bookType = bookType;
        this.version = version;
        this.state = new AvailableState(this, branch);
    }

    /**
     * Places this book on hold for a patron if validation passes.
     * Increments version after successful state transition.
     * 
     * @param patronId patron placing the hold
     * @param branchId library branch where hold is placed
     * @param holdTill expiration time for the hold
     */
    public void placeOnHold(PatronId patronId, LibraryBranchId branchId, Instant holdTill) {
        if (state.canBePutOnHold(patronId)) {
            state = state.placeOnHold(patronId, branchId, holdTill);
            version = version.next();
        }
    }

    /**
     * Checks out this book to a patron if validation passes.
     * Increments version after successful state transition.
     * 
     * @param patronId patron checking out the book
     * @param branchId library branch where checkout occurs
     */
    public void checkout(PatronId patronId, LibraryBranchId branchId) {
        if (state.canBeCheckedOut(patronId)) {
            state = state.checkout(patronId, branchId);
            version = version.next();
        }
    }

    /**
     * Returns this book to the library if validation passes.
     * Increments version after successful state transition.
     * 
     * @param branchId library branch where book is returned
     */
    public void returnBook(LibraryBranchId branchId) {
        if (state.canBeReturned()) {
            state = state.returnBook(branchId);
            version = version.next();
        }
    }

    /**
     * Cancels the current hold on this book.
     * Increments version after successful state transition.
     */
    public void cancelHold() {
        state = state.cancelHold();
        version = version.next();
    }

    /**
     * Expires the current hold on this book if hold period has passed.
     * Increments version after successful state transition.
     */
    public void expireHold() {
        state = state.expireHold();
        version = version.next();
    }

    /**
     * Gets the name of the current state.
     * 
     * @return state name (e.g., "AVAILABLE", "ON_HOLD", "CHECKED_OUT")
     */
    public String getCurrentState() {
        return state.getStateName();
    }

    /**
     * Gets the library branch where the book currently resides.
     * 
     * @return current branch ID
     */
    public LibraryBranchId getCurrentBranch() {
        return state.getCurrentBranch();
    }

    /**
     * Gets the patron currently associated with this book (if any).
     * 
     * @return patron ID if book is on hold or checked out, null if available
     */
    public PatronId getCurrentPatron() {
        return state.getCurrentPatron();
    }
}
