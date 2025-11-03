package io.pillopl.library.lending.patronprofile.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.pillopl.library.catalogue.BookId
import io.pillopl.library.commons.commands.Result
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId
import io.pillopl.library.lending.patron.application.hold.CancelingHold
import io.pillopl.library.lending.patron.application.hold.PlacingOnHold
import io.pillopl.library.lending.patron.model.PatronId
import io.pillopl.library.lending.patronprofile.model.*
import io.vavr.control.Try
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.time.Instant

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.when
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(PatronProfileController.class)
@Import(WebConfiguration.class)
class PatronProfileControllerTest extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @MockBean
    PatronProfiles patronProfiles

    @MockBean
    PlacingOnHold placingOnHold

    @MockBean
    CancelingHold cancelingHold

    UUID patronId = UUID.randomUUID()
    UUID bookId = UUID.randomUUID()
    UUID anotherBookId = UUID.randomUUID()
    UUID libraryBranchId = UUID.randomUUID()
    Instant now = Instant.now()

    def 'should return patron profile with HATEOAS links'() {
        when:
            def response = mockMvc.perform(get("/profiles/{patronId}", patronId))

        then:
            response.andExpect(status().isOk())
                    .andExpect(jsonPath('$.patronId').value(patronId.toString()))
                    .andExpect(jsonPath('$._links.holds.href').exists())
                    .andExpect(jsonPath('$._links.checkouts.href').exists())
                    .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return collection of holds with HATEOAS links'() {
        given:
            def hold1 = createHold(bookId, now)
            def hold2 = createHold(anotherBookId, now.plusSeconds(3600))
            def patronProfile = patronProfileWithHolds([hold1, hold2])
            when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile)

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))

        then:
            response.andExpect(status().isOk())
                    .andExpect(jsonPath('$._embedded').exists())
                    .andExpect(jsonPath('$._links.self.href').exists())
                    .andExpect(jsonPath('$._embedded.holdList[0].bookId').value(bookId.toString()))
                    .andExpect(jsonPath('$._embedded.holdList[0]._links.self.href').exists())
                    .andExpect(jsonPath('$._embedded.holdList[1].bookId').value(anotherBookId.toString()))
    }

    def 'should return empty collection when patron has no holds'() {
        given:
            def patronProfile = patronProfileWithHolds([])
            when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile)

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))

        then:
            response.andExpect(status().isOk())
                    .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return specific hold when it exists'() {
        given:
            def hold = createHold(bookId, now)
            def patronProfile = patronProfileWithHolds([hold])
            when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile)

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isOk())
                    .andExpect(jsonPath('$.bookId').value(bookId.toString()))
                    .andExpect(jsonPath('$.till').exists())
                    .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return 404 when hold does not exist'() {
        given:
            def patronProfile = patronProfileWithHolds([])
            when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile)

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNotFound())
    }

    def 'should return collection of checkouts with HATEOAS links'() {
        given:
            def checkout1 = createCheckout(bookId, now)
            def checkout2 = createCheckout(anotherBookId, now.plusSeconds(7200))
            def patronProfile = patronProfileWithCheckouts([checkout1, checkout2])
            when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile)

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))

        then:
            response.andExpect(status().isOk())
                    .andExpect(jsonPath('$._embedded').exists())
                    .andExpect(jsonPath('$._links.self.href').exists())
                    .andExpect(jsonPath('$._embedded.checkoutList[0].bookId').value(bookId.toString()))
                    .andExpect(jsonPath('$._embedded.checkoutList[0]._links.self.href').exists())
                    .andExpect(jsonPath('$._embedded.checkoutList[1].bookId').value(anotherBookId.toString()))
    }

    def 'should return empty collection when patron has no checkouts'() {
        given:
            def patronProfile = patronProfileWithCheckouts([])
            when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile)

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))

        then:
            response.andExpect(status().isOk())
                    .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return specific checkout when it exists'() {
        given:
            def checkout = createCheckout(bookId, now)
            def patronProfile = patronProfileWithCheckouts([checkout])
            when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile)

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isOk())
                    .andExpect(jsonPath('$.bookId').value(bookId.toString()))
                    .andExpect(jsonPath('$.till').exists())
                    .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return 404 when checkout does not exist'() {
        given:
            def patronProfile = patronProfileWithCheckouts([])
            when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile)

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNotFound())
    }

    def 'should successfully place hold and return 200'() {
        given:
            def requestBody = [
                bookId: bookId.toString(),
                libraryBranchId: libraryBranchId.toString(),
                numberOfDays: 7
            ]
            when(placingOnHold.placeOnHold(any())).thenReturn(Try.success(Result.Success))

        when:
            def response = mockMvc.perform(
                post("/profiles/{patronId}/holds", patronId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )

        then:
            response.andExpect(status().isOk())
    }

    def 'should return 500 when placing hold fails'() {
        given:
            def requestBody = [
                bookId: bookId.toString(),
                libraryBranchId: libraryBranchId.toString(),
                numberOfDays: 7
            ]
            when(placingOnHold.placeOnHold(any())).thenReturn(Try.failure(new RuntimeException("Database error")))

        when:
            def response = mockMvc.perform(
                post("/profiles/{patronId}/holds", patronId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )

        then:
            response.andExpect(status().isInternalServerError())
    }

    def 'should properly deserialize place hold request'() {
        given:
            def requestBody = [
                bookId: bookId.toString(),
                libraryBranchId: libraryBranchId.toString(),
                numberOfDays: 14
            ]
            when(placingOnHold.placeOnHold(any())).thenReturn(Try.success(Result.Success))

        when:
            mockMvc.perform(
                post("/profiles/{patronId}/holds", patronId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )

        then:
            noExceptionThrown()
    }

    def 'should successfully cancel hold and return 204'() {
        given:
            when(cancelingHold.cancelHold(any())).thenReturn(Try.success(Result.Success))

        when:
            def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNoContent())
    }

    def 'should return 404 when canceling hold with IllegalArgumentException'() {
        given:
            when(cancelingHold.cancelHold(any())).thenReturn(Try.failure(new IllegalArgumentException("Book not found")))

        when:
            def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNotFound())
    }

    def 'should return 500 when canceling hold fails with other exception'() {
        given:
            when(cancelingHold.cancelHold(any())).thenReturn(Try.failure(new RuntimeException("Unexpected error")))

        when:
            def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isInternalServerError())
    }

    private PatronProfile patronProfileWithHolds(List<io.pillopl.library.lending.patronprofile.model.Hold> holds) {
        def holdsView = new HoldsView(io.vavr.collection.List.ofAll(holds))
        def checkoutsView = new CheckoutsView(io.vavr.collection.List.empty())
        return new PatronProfile(holdsView, checkoutsView)
    }

    private PatronProfile patronProfileWithCheckouts(List<io.pillopl.library.lending.patronprofile.model.Checkout> checkouts) {
        def holdsView = new HoldsView(io.vavr.collection.List.empty())
        def checkoutsView = new CheckoutsView(io.vavr.collection.List.ofAll(checkouts))
        return new PatronProfile(holdsView, checkoutsView)
    }

    private io.pillopl.library.lending.patronprofile.model.Hold createHold(UUID bookId, Instant till) {
        return new io.pillopl.library.lending.patronprofile.model.Hold(new BookId(bookId), till)
    }

    private io.pillopl.library.lending.patronprofile.model.Checkout createCheckout(UUID bookId, Instant till) {
        return new io.pillopl.library.lending.patronprofile.model.Checkout(new BookId(bookId), till)
    }
}
