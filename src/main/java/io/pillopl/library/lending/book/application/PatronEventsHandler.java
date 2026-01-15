package io.pillopl.library.lending.book.application;

import io.pillopl.library.catalogue.BookId;
import io.pillopl.library.commons.events.DomainEvents;
import io.pillopl.library.lending.book.new_model.*;
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId;
import io.pillopl.library.lending.patron.model.PatronEvent.*;
import io.pillopl.library.lending.patron.model.PatronId;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
public class PatronEventsHandler {

    private final BookRepository bookRepository;
    private final DomainEvents domainEvents;

    @EventListener
    void handle(BookPlacedOnHold bookPlacedOnHold) {
        bookRepository.findBy(new BookId(bookPlacedOnHold.getBookId()))
                .peek(book -> handleBookPlacedOnHold(book, bookPlacedOnHold))
                .peek(this::saveBook);
    }

    @EventListener
    void handle(BookCheckedOut bookCheckedOut) {
        bookRepository.findBy(new BookId(bookCheckedOut.getBookId()))
                .peek(book -> handleBookCheckedOut(book, bookCheckedOut))
                .peek(this::saveBook);
    }

    @EventListener
    void handle(BookHoldExpired holdExpired) {
        bookRepository.findBy(new BookId(holdExpired.getBookId()))
                .peek(book -> handleBookHoldExpired(book, holdExpired))
                .peek(this::saveBook);
    }

    @EventListener
    void handle(BookHoldCanceled holdCanceled) {
        bookRepository.findBy(new BookId(holdCanceled.getBookId()))
                .peek(book -> handleBookHoldCanceled(book, holdCanceled))
                .peek(this::saveBook);
    }

    @EventListener
    void handle(BookReturned bookReturned) {
        bookRepository.findBy(new BookId(bookReturned.getBookId()))
                .peek(book -> handleBookReturned(book, bookReturned))
                .peek(this::saveBook);
    }


    private void handleBookPlacedOnHold(Book book, BookPlacedOnHold bookPlacedOnHold) {
        PatronId requestingPatron = new PatronId(bookPlacedOnHold.getPatronId());
        
        // Check for duplicate hold
        if ("ON_HOLD".equals(book.getCurrentState())) {
            PatronId currentPatron = book.getCurrentPatron();
            if (currentPatron != null && !currentPatron.equals(requestingPatron)) {
                domainEvents.publish(
                        new BookDuplicateHoldFound(
                                Instant.now(),
                                currentPatron.getPatronId(),
                                bookPlacedOnHold.getPatronId(),
                                bookPlacedOnHold.getLibraryBranchId(),
                                bookPlacedOnHold.getBookId()));
            }
            return;
        }
        
        // Place hold if book is available
        book.placeOnHold(
                requestingPatron,
                new LibraryBranchId(bookPlacedOnHold.getLibraryBranchId()),
                bookPlacedOnHold.getHoldTill());
    }


    private void handleBookHoldExpired(Book book, BookHoldExpired holdExpired) {
        book.expireHold();
    }

    private void handleBookHoldCanceled(Book book, BookHoldCanceled holdCanceled) {
        book.cancelHold();
    }

    private void handleBookCheckedOut(Book book, BookCheckedOut bookCheckedOut) {
        book.checkout(
                new PatronId(bookCheckedOut.getPatronId()),
                new LibraryBranchId(bookCheckedOut.getLibraryBranchId()));
    }

    private void handleBookReturned(Book book, BookReturned bookReturned) {
        book.returnBook(new LibraryBranchId(bookReturned.getLibraryBranchId()));
    }

    private void saveBook(Book book) {
        bookRepository.save(book);
    }

}
