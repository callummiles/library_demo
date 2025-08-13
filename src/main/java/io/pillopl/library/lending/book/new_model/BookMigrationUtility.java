package io.pillopl.library.lending.book.new_model;

import io.pillopl.library.lending.book.model.AvailableBook;
import io.pillopl.library.lending.book.model.BookOnHold;
import io.pillopl.library.lending.book.model.CheckedOutBook;
import java.lang.reflect.Field;

public class BookMigrationUtility {
    
    public static Book migrate(AvailableBook availableBook) {
        Book book = new Book(
            availableBook.getBookId(),
            availableBook.getBookInformation().getBookType(),
            availableBook.getLibraryBranch(),
            availableBook.getVersion()
        );
        return book;
    }
    
    public static Book migrate(BookOnHold bookOnHold) {
        Book book = new Book(
            bookOnHold.getBookId(),
            bookOnHold.getBookInformation().getBookType(),
            bookOnHold.getHoldPlacedAt(),
            bookOnHold.getVersion()
        );
        
        OnHoldState onHoldState = new OnHoldState(
            book,
            bookOnHold.getHoldPlacedAt(),
            bookOnHold.getByPatron(),
            bookOnHold.getHoldTill()
        );
        
        setBookState(book, onHoldState);
        return book;
    }
    
    public static Book migrate(CheckedOutBook checkedOutBook) {
        Book book = new Book(
            checkedOutBook.getBookId(),
            checkedOutBook.getBookInformation().getBookType(),
            checkedOutBook.getCheckedOutAt(),
            checkedOutBook.getVersion()
        );
        
        CheckedOutState checkedOutState = new CheckedOutState(
            book,
            checkedOutBook.getCheckedOutAt(),
            checkedOutBook.getByPatron()
        );
        
        setBookState(book, checkedOutState);
        return book;
    }
    
    private static void setBookState(Book book, BookState newState) {
        try {
            Field stateField = Book.class.getDeclaredField("state");
            stateField.setAccessible(true);
            stateField.set(book, newState);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set book state during migration", e);
        }
    }
}
