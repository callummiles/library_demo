package io.pillopl.library.lending.patronprofile.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.pillopl.library.catalogue.BookId
import io.pillopl.library.commons.commands.Result
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId
import io.pillopl.library.lending.patron.application.hold.CancelingHold
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
import java.util.UUID

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when
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
    Instant holdTill = Instant.parse("2023-12-31T23:59:59Z")

    def "should return patron profile with HATEOAS links"() {
        when:
        def response = mockMvc.perform(get("/profiles/{patronId}", patronId))

        then:
        response.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath('$.patronId').value(patronId.toString()))
                .andExpect(jsonPath('$._links.self.href').exists())
                .andExpect(jsonPath('$._links.holds.href').exists())
                .andExpect(jsonPath('$._links.checkouts.href').exists())
    }

    def "should return empty holds collection when patron has no holds"() {
        given:
        PatronProfile patronProfile = createPatronProfileWithNoHolds()
        when(patronProfiles.fetchFor(new PatronId(patronId))).thenReturn(patronProfile)

        when:
        def response = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))

        then:
        response.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath('$._embedded').doesNotExist())
                .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return holds collection with HATEOAS links when patron has holds"() {
        given:
        PatronProfile patronProfile = createPatronProfileWithHolds()
        when(patronProfiles.fetchFor(new PatronId(patronId))).thenReturn(patronProfile)

        when:
        def response = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))

        then:
        response.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath('$._embedded.holdList').isArray())
                .andExpect(jsonPath('$._embedded.holdList[0].bookId').value(bookId.toString()))
                .andExpect(jsonPath('$._embedded.holdList[0].till').exists())
                .andExpect(jsonPath('$._embedded.holdList[0]._links.self.href').exists())
                .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return specific hold when found"() {
        given:
        PatronProfile patronProfile = createPatronProfileWithHolds()
        when(patronProfiles.fetchFor(new PatronId(patronId))).thenReturn(patronProfile)

        when:
        def response = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
        response.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath('$.bookId').value(bookId.toString()))
                .andExpect(jsonPath('$.till').exists())
                .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return 404 when hold not found"() {
        given:
        PatronProfile patronProfile = createPatronProfileWithNoHolds()
        when(patronProfiles.fetchFor(new PatronId(patronId))).thenReturn(patronProfile)

        when:
        def response = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
        response.andExpect(status().isNotFound())
    }

    def "should successfully place hold and return 200"() {
        given:
        PlaceHoldRequest request = new PlaceHoldRequest(bookId, libraryBranchId, 7)
        when(placingOnHold.placeOnHold(any())).thenReturn(Try.success(Result.Success))

        when:
        def response = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        response.andExpect(status().isOk())
    }

    def "should return 500 when place hold fails"() {
        given:
        PlaceHoldRequest request = new PlaceHoldRequest(bookId, libraryBranchId, 7)
        when(placingOnHold.placeOnHold(any())).thenReturn(Try.failure(new RuntimeException("Service error")))

        when:
        def response = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        response.andExpect(status().isInternalServerError())
    }

    def "should successfully cancel hold and return 204"() {
        given:
        when(cancelingHold.cancelHold(any())).thenReturn(Try.success(Result.Success))

        when:
        def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
        response.andExpect(status().isNoContent())
    }

    def "should return 404 when cancel hold throws IllegalArgumentException"() {
        given:
        when(cancelingHold.cancelHold(any())).thenReturn(Try.failure(new IllegalArgumentException("Hold not found")))

        when:
        def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
        response.andExpect(status().isNotFound())
    }

    def "should return 500 when cancel hold throws generic exception"() {
        given:
        when(cancelingHold.cancelHold(any())).thenReturn(Try.failure(new RuntimeException("Service error")))

        when:
        def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
        response.andExpect(status().isInternalServerError())
    }

    def "should return empty checkouts collection when patron has no checkouts"() {
        given:
        PatronProfile patronProfile = createPatronProfileWithNoCheckouts()
        when(patronProfiles.fetchFor(new PatronId(patronId))).thenReturn(patronProfile)

        when:
        def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))

        then:
        response.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath('$._embedded').doesNotExist())
                .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return checkouts collection with HATEOAS links when patron has checkouts"() {
        given:
        PatronProfile patronProfile = createPatronProfileWithCheckouts()
        when(patronProfiles.fetchFor(new PatronId(patronId))).thenReturn(patronProfile)

        when:
        def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))

        then:
        response.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath('$._embedded.checkoutList').isArray())
                .andExpect(jsonPath('$._embedded.checkoutList[0].bookId').value(bookId.toString()))
                .andExpect(jsonPath('$._embedded.checkoutList[0].till').exists())
                .andExpect(jsonPath('$._embedded.checkoutList[0]._links.self.href').exists())
                .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return specific checkout when found"() {
        given:
        PatronProfile patronProfile = createPatronProfileWithCheckouts()
        when(patronProfiles.fetchFor(new PatronId(patronId))).thenReturn(patronProfile)

        when:
        def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", patronId, bookId))

        then:
        response.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath('$.bookId').value(bookId.toString()))
                .andExpect(jsonPath('$.till').exists())
                .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return 404 when checkout not found"() {
        given:
        PatronProfile patronProfile = createPatronProfileWithNoCheckouts()
        when(patronProfiles.fetchFor(new PatronId(patronId))).thenReturn(patronProfile)

        when:
        def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", patronId, bookId))

        then:
        response.andExpect(status().isNotFound())
    }

    def "should handle malformed JSON in place hold request"() {
        when:
        def response = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))

        then:
        response.andExpect(status().isBadRequest())
    }

    def "should handle invalid UUID in path variables"() {
        when:
        def response = mockMvc.perform(get("/profiles/{patronId}", "invalid-uuid"))

        then:
        response.andExpect(status().isBadRequest())
    }

    private PatronProfile createPatronProfileWithNoHolds() {
        HoldsView holdsView = new HoldsView(List.empty())
        CheckoutsView checkoutsView = new CheckoutsView(List.empty())
        return new PatronProfile(holdsView, checkoutsView)
    }

    private PatronProfile createPatronProfileWithHolds() {
        Hold hold = new Hold(new BookId(bookId), holdTill)
        HoldsView holdsView = new HoldsView(List.of(hold))
        CheckoutsView checkoutsView = new CheckoutsView(List.empty())
        return new PatronProfile(holdsView, checkoutsView)
    }

    private PatronProfile createPatronProfileWithNoCheckouts() {
        HoldsView holdsView = new HoldsView(List.empty())
        CheckoutsView checkoutsView = new CheckoutsView(List.empty())
        return new PatronProfile(holdsView, checkoutsView)
    }

    private PatronProfile createPatronProfileWithCheckouts() {
        Checkout checkout = new Checkout(new BookId(bookId), holdTill)
        HoldsView holdsView = new HoldsView(List.empty())
        CheckoutsView checkoutsView = new CheckoutsView(List.of(checkout))
        return new PatronProfile(holdsView, checkoutsView)
    }
}
