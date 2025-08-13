package io.pillopl.library.catalogue

import io.vavr.control.Option
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import static io.pillopl.library.catalogue.BookFixture.DDD
import static io.pillopl.library.catalogue.BookFixture.NON_PRESENT_ISBN
import static io.pillopl.library.catalogue.BookInstance.instanceOf
import static io.pillopl.library.catalogue.BookType.Restricted

/**
 * Integration test for the catalogue database operations.
 * 
 * This test verifies that the CatalogueDatabase can properly persist and retrieve
 * books and book instances from the actual database. It tests the core CRUD operations
 * of the catalogue bounded context, ensuring that books can be saved, loaded, and
 * that non-existent books return empty results as expected.
 * 
 * The catalogue context is responsible for managing the library's book inventory,
 * separate from the lending operations handled by the lending bounded context.
 */
@SpringBootTest(classes = CatalogueConfiguration.class)
class CatalogueDatabaseIT extends Specification {

    @Autowired
    CatalogueDatabase catalogueDatabase

    /**
     * Verifies that a book can be successfully saved to and retrieved from the database.
     * This test ensures the basic persistence functionality works correctly for the
     * catalogue's book management operations.
     */
    def 'should be able to save and load new book'() {
        given:
            Book book = DDD
        when:
            catalogueDatabase.saveNew(book)
        and:
            Option<Book> ddd = catalogueDatabase.findBy(book.bookIsbn)
        then:
            ddd.isDefined()
            ddd.get() == book
    }

    /**
     * Verifies that attempting to load a book with a non-existent ISBN returns
     * an empty Option, ensuring proper handling of missing data scenarios.
     */
    def 'should not load not present book'() {
        when:
            Option<Book> ddd = catalogueDatabase.findBy(NON_PRESENT_ISBN)
        then:
            ddd.isEmpty()
    }

    /**
     * Verifies that book instances (specific copies of books with particular types
     * like Restricted) can be successfully saved to the database without throwing
     * exceptions. This tests the persistence of book instances which represent
     * individual copies of books in the library's inventory.
     */
    def 'should save book instance'() {
        when:
            catalogueDatabase.saveNew(instanceOf(DDD, Restricted))
        then:
            noExceptionThrown()
    }


}
