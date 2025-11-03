package io.pillopl.library.lending.book.new_model;

import io.pillopl.library.lending.librarybranch.model.LibraryBranchId;
import io.pillopl.library.lending.patron.model.PatronId;
import io.pillopl.library.commons.aggregates.Version;
import java.time.Instant;

/**
 * Represents the state of a book in the lending system.
 * Implements the State pattern for managing book state transitions and business rules.
 * 
 * <p>This interface defines the contract for all book states (Available, OnHold, CheckedOut).
 * Each state implementation handles valid transitions and enforces business rules specific to that state.
 * Invalid state transitions throw IllegalStateException via the default invalidTransition() method.
 * 
 * <p>State Transitions:
 * <ul>
 *   <li>Available → OnHold (via placeOnHold)</li>
 *   <li>Available → CheckedOut (via checkout)</li>
 *   <li>OnHold → CheckedOut (via checkout, only by same patron)</li>
 *   <li>OnHold → Available (via cancelHold or expireHold)</li>
 *   <li>CheckedOut → Available (via returnBook)</li>
 * </ul>
 */
public interface BookState {
    /**
     * Places this book on hold for the specified patron.
     * 
     * @param patronId the patron placing the hold
     * @param branchId the library branch where hold is placed
     * @param holdTill when the hold expires
     * @return new state after transition
     * @throws IllegalStateException if hold cannot be placed in current state
     */
    BookState placeOnHold(PatronId patronId, LibraryBranchId branchId, Instant holdTill);
    
    /**
     * Checks out this book to the specified patron.
     * 
     * @param patronId the patron checking out the book
     * @param branchId the library branch where checkout occurs
     * @return new state after transition
     * @throws IllegalStateException if book cannot be checked out in current state
     */
    BookState checkout(PatronId patronId, LibraryBranchId branchId);
    
    /**
     * Returns this book to the library.
     * 
     * @param branchId the library branch where book is returned
     * @return new state after transition
     * @throws IllegalStateException if book cannot be returned in current state
     */
    BookState returnBook(LibraryBranchId branchId);
    
    /**
     * Cancels an existing hold on this book.
     * 
     * @return new state after transition
     * @throws IllegalStateException if there is no hold to cancel
     */
    BookState cancelHold();
    
    /**
     * Expires an existing hold on this book if the hold period has passed.
     * 
     * @return new state after transition (or same state if hold has not expired yet)
     * @throws IllegalStateException if there is no hold to expire
     */
    BookState expireHold();
    
    /**
     * Checks if this book can be checked out by the specified patron.
     * 
     * @param patronId the patron attempting to check out the book
     * @return true if book can be checked out by this patron, false otherwise
     */
    boolean canBeCheckedOut(PatronId patronId);
    
    /**
     * Checks if this book can be put on hold by the specified patron.
     * 
     * @param patronId the patron attempting to place a hold
     * @return true if book can be put on hold, false otherwise
     */
    boolean canBePutOnHold(PatronId patronId);
    
    /**
     * Checks if this book can be returned.
     * 
     * @return true if book can be returned, false otherwise
     */
    boolean canBeReturned();
    
    /**
     * Gets the name of the current state.
     * 
     * @return state name (e.g., "AVAILABLE", "ON_HOLD", "CHECKED_OUT")
     */
    String getStateName();
    
    /**
     * Gets the version of the book aggregate.
     * 
     * @return current version
     */
    Version getVersion();
    
    /**
     * Gets the library branch where the book currently resides.
     * 
     * @return current branch ID
     */
    LibraryBranchId getCurrentBranch();
    
    /**
     * Gets the patron currently associated with this book (if any).
     * 
     * @return patron ID if book is on hold or checked out, null if available
     */
    PatronId getCurrentPatron();
    
    /**
     * Default implementation for invalid state transitions.
     * Throws IllegalStateException with the provided message.
     * 
     * @param message description of the invalid transition
     * @return never returns (always throws exception)
     * @throws IllegalStateException always
     */
    default BookState invalidTransition(String message) {
        throw new IllegalStateException("Invalid state transition: " + message);
    }
}
