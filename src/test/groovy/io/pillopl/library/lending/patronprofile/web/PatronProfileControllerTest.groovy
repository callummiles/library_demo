package io.pillopl.library.lending.patronprofile.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.pillopl.library.catalogue.BookId
import io.pillopl.library.commons.commands.Result
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId
import io.pillopl.library.lending.patron.application.hold.CancelHoldCommand
import io.pillopl.library.lending.patron.application.hold.CancelingHold
import io.pillopl.library.lending.patron.application.hold.PlaceOnHoldCommand
import io.pillopl.library.lending.patron.application.hold.PlacingOnHold
import io.pillopl.library.lending.patron.model.PatronId
import io.pillopl.library.lending.patronprofile.model.*
import io.vavr.collection.List
import io.vavr.control.Option
import io.vavr.control.Try
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.time.Instant

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(PatronProfileController)
class PatronProfileControllerTest extends Specification {

    @Autowired
    MockMvc mockMvc

    @MockBean
    PatronProfiles patronProfiles

    @MockBean
    PlacingOnHold placingOnHold

    @MockBean
    CancelingHold cancelingHold

    @Autowired
    ObjectMapper objectMapper

    UUID patronId = UUID.randomUUID()
    UUID bookId = UUID.randomUUID()
    UUID libraryBranchId = UUID.randomUUID()
    Instant holdTill = Instant.now().plusDays(7)

    def "should return patron profile with HATEOAS links"() {
        when:
            def response = mockMvc.perform(get("/profiles/{patronId}", patronId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(jsonPath('$.patronId').value(patronId.toString()))
                   .andExpect(jsonPath('$._links.self.href').exists())
                   .andExpect(jsonPath('$._links.holds.href').exists())
                   .andExpect(jsonPath('$._links.checkouts.href').exists())
    }

    def "should return holds collection with HATEOAS links and affordances"() {
        given:
            def hold = new Hold(new BookId(bookId), holdTill)
            def holdsView = new HoldsView(List.of(hold))
            def patronProfile = new PatronProfile(holdsView, new CheckoutsView(List.empty()))
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(jsonPath('$._embedded').exists())
                   .andExpect(jsonPath('$._links.self.href').exists())
                   .andExpect(jsonPath('$._embedded.holdList[0].bookId').value(bookId.toString()))
                   .andExpect(jsonPath('$._embedded.holdList[0].till').exists())
                   .andExpect(jsonPath('$._embedded.holdList[0]._links.self.href').exists())
    }

    def "should return empty holds collection"() {
        given:
            def holdsView = new HoldsView(List.empty())
            def patronProfile = new PatronProfile(holdsView, new CheckoutsView(List.empty()))
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return specific hold when found"() {
        given:
            def hold = new Hold(new BookId(bookId), holdTill)
            def holdsView = new HoldsView(List.of(hold))
            def patronProfile = new PatronProfile(holdsView, new CheckoutsView(List.empty()))
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(jsonPath('$.bookId').value(bookId.toString()))
                   .andExpect(jsonPath('$.till').exists())
                   .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return 404 when hold not found"() {
        given:
            def holdsView = new HoldsView(List.empty())
            def patronProfile = new PatronProfile(holdsView, new CheckoutsView(List.empty()))
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNotFound())
    }

    def "should return checkouts collection with HATEOAS links"() {
        given:
            def checkout = new Checkout(new BookId(bookId), holdTill)
            def checkoutsView = new CheckoutsView(List.of(checkout))
            def patronProfile = new PatronProfile(new HoldsView(List.empty()), checkoutsView)
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(jsonPath('$._embedded').exists())
                   .andExpect(jsonPath('$._links.self.href').exists())
                   .andExpect(jsonPath('$._embedded.checkoutList[0].bookId').value(bookId.toString()))
                   .andExpect(jsonPath('$._embedded.checkoutList[0].till').exists())
                   .andExpect(jsonPath('$._embedded.checkoutList[0]._links.self.href').exists())
    }

    def "should return empty checkouts collection"() {
        given:
            def checkoutsView = new CheckoutsView(List.empty())
            def patronProfile = new PatronProfile(new HoldsView(List.empty()), checkoutsView)
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return specific checkout when found"() {
        given:
            def checkout = new Checkout(new BookId(bookId), holdTill)
            def checkoutsView = new CheckoutsView(List.of(checkout))
            def patronProfile = new PatronProfile(new HoldsView(List.empty()), checkoutsView)
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(jsonPath('$.bookId').value(bookId.toString()))
                   .andExpect(jsonPath('$.till').exists())
                   .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return 404 when checkout not found"() {
        given:
            def checkoutsView = new CheckoutsView(List.empty())
            def patronProfile = new PatronProfile(new HoldsView(List.empty()), checkoutsView)
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNotFound())
    }

    def "should successfully place hold when service succeeds"() {
        given:
            def request = new PlaceHoldRequest(bookId, libraryBranchId, 7)
            def requestJson = objectMapper.writeValueAsString(request)
            placingOnHold.placeOnHold(_ as PlaceOnHoldCommand) >> Try.success(Result.Success)

        when:
            def response = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))

        then:
            response.andExpect(status().isOk())
    }

    def "should return 500 when place hold service returns rejection"() {
        given:
            def request = new PlaceHoldRequest(bookId, libraryBranchId, 7)
            def requestJson = objectMapper.writeValueAsString(request)
            placingOnHold.placeOnHold(_ as PlaceOnHoldCommand) >> Try.success(Result.Rejection)

        when:
            def response = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))

        then:
            response.andExpect(status().isInternalServerError())
    }

    def "should return 500 when place hold service fails"() {
        given:
            def request = new PlaceHoldRequest(bookId, libraryBranchId, 7)
            def requestJson = objectMapper.writeValueAsString(request)
            placingOnHold.placeOnHold(_ as PlaceOnHoldCommand) >> Try.failure(new RuntimeException("Service error"))

        when:
            def response = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))

        then:
            response.andExpect(status().isInternalServerError())
    }

    def "should successfully cancel hold when service succeeds"() {
        given:
            cancelingHold.cancelHold(_ as CancelHoldCommand) >> Try.success(Result.Success)

        when:
            def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNoContent())
    }

    def "should return 404 when cancel hold throws IllegalArgumentException"() {
        given:
            cancelingHold.cancelHold(_ as CancelHoldCommand) >> Try.failure(new IllegalArgumentException("Hold not found"))

        when:
            def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNotFound())
    }

    def "should return 500 when cancel hold service returns rejection"() {
        given:
            cancelingHold.cancelHold(_ as CancelHoldCommand) >> Try.success(Result.Rejection)

        when:
            def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isInternalServerError())
    }

    def "should return 500 when cancel hold service fails with other exception"() {
        given:
            cancelingHold.cancelHold(_ as CancelHoldCommand) >> Try.failure(new RuntimeException("Service error"))

        when:
            def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isInternalServerError())
    }

    def "should properly deserialize PlaceHoldRequest JSON"() {
        given:
            def request = new PlaceHoldRequest(bookId, libraryBranchId, 7)
            def requestJson = objectMapper.writeValueAsString(request)
            placingOnHold.placeOnHold(_ as PlaceOnHoldCommand) >> Try.success(Result.Success)

        when:
            def response = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))

        then:
            response.andExpect(status().isOk())
            1 * placingOnHold.placeOnHold({ PlaceOnHoldCommand cmd ->
                cmd.patronId.patronId == patronId &&
                cmd.bookId.bookId == bookId &&
                cmd.libraryBranchId.libraryBranchId == libraryBranchId &&
                cmd.holdDuration.get() == 7
            })
    }

    def "should properly construct CancelHoldCommand"() {
        given:
            cancelingHold.cancelHold(_ as CancelHoldCommand) >> Try.success(Result.Success)

        when:
            def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNoContent())
            1 * cancelingHold.cancelHold({ CancelHoldCommand cmd ->
                cmd.patronId.patronId == patronId &&
                cmd.bookId.bookId == bookId
            })
    }
}
