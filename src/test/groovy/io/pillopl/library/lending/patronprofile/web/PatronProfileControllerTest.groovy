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
import io.pillopl.library.lending.patronprofile.model.Checkout
import io.pillopl.library.lending.patronprofile.model.CheckoutsView
import io.pillopl.library.lending.patronprofile.model.Hold
import io.pillopl.library.lending.patronprofile.model.HoldsView
import io.pillopl.library.lending.patronprofile.model.PatronProfile
import io.pillopl.library.lending.patronprofile.model.PatronProfiles
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

    def patronId = UUID.randomUUID()
    def bookId = UUID.randomUUID()
    def libraryBranchId = UUID.randomUUID()
    def anotherBookId = UUID.randomUUID()

    def createTestHold() {
        return new Hold(new BookId(bookId), Instant.now().plusDays(7))
    }

    def createTestCheckout() {
        return new Checkout(new BookId(bookId), Instant.now().plusDays(14))
    }

    def createTestPatronProfile() {
        return new PatronProfile(
            new HoldsView(List.of(createTestHold())),
            new CheckoutsView(List.of(createTestCheckout()))
        )
    }

    def createEmptyPatronProfile() {
        return new PatronProfile(
            new HoldsView(List.empty()),
            new CheckoutsView(List.empty())
        )
    }

    def 'should return patron profile with HATEOAS links'() {
        when:
            def result = mockMvc.perform(get("/profiles/{patronId}", patronId))

        then:
            result.andExpect(status().isOk())
                  .andExpect(jsonPath('$.patronId').value(patronId.toString()))
                  .andExpect(jsonPath('$._links.holds.href').exists())
                  .andExpect(jsonPath('$._links.checkouts.href').exists())
                  .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return patron holds collection with links'() {
        given:
            def patronProfile = createTestPatronProfile()
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))

        then:
            result.andExpect(status().isOk())
                  .andExpect(jsonPath('$._embedded').exists())
                  .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return empty holds collection when patron has no holds'() {
        given:
            def patronProfile = createEmptyPatronProfile()
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))

        then:
            result.andExpect(status().isOk())
                  .andExpect(jsonPath('$._embedded').exists())
                  .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return individual hold when found'() {
        given:
            def patronProfile = createTestPatronProfile()
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            result.andExpect(status().isOk())
                  .andExpect(jsonPath('$.bookId').value(bookId.toString()))
                  .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return 404 when hold not found'() {
        given:
            def patronProfile = createEmptyPatronProfile()
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, anotherBookId))

        then:
            result.andExpect(status().isNotFound())
    }

    def 'should return patron checkouts collection with links'() {
        given:
            def patronProfile = createTestPatronProfile()
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))

        then:
            result.andExpect(status().isOk())
                  .andExpect(jsonPath('$._embedded').exists())
                  .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return empty checkouts collection when patron has no checkouts'() {
        given:
            def patronProfile = createEmptyPatronProfile()
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))

        then:
            result.andExpect(status().isOk())
                  .andExpect(jsonPath('$._embedded').exists())
                  .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return individual checkout when found'() {
        given:
            def patronProfile = createTestPatronProfile()
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", patronId, bookId))

        then:
            result.andExpect(status().isOk())
                  .andExpect(jsonPath('$.bookId').value(bookId.toString()))
                  .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return 404 when checkout not found'() {
        given:
            def patronProfile = createEmptyPatronProfile()
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", patronId, anotherBookId))

        then:
            result.andExpect(status().isNotFound())
    }

    def 'should place hold successfully'() {
        given:
            def request = new PlaceHoldRequest(bookId, libraryBranchId, 7)
            placingOnHold.placeOnHold(_ as PlaceOnHoldCommand) >> Try.success(Result.Success)

        when:
            def result = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
            result.andExpect(status().isOk())
    }

    def 'should place hold with null numberOfDays'() {
        given:
            def request = new PlaceHoldRequest(bookId, libraryBranchId, null)
            placingOnHold.placeOnHold(_ as PlaceOnHoldCommand) >> Try.success(Result.Success)

        when:
            def result = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
            result.andExpect(status().isOk())
    }

    def 'should return 500 when place hold fails'() {
        given:
            def request = new PlaceHoldRequest(bookId, libraryBranchId, 7)
            placingOnHold.placeOnHold(_ as PlaceOnHoldCommand) >> Try.failure(new RuntimeException("Service error"))

        when:
            def result = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
            result.andExpect(status().isInternalServerError())
    }

    def 'should return 400 when place hold request is malformed'() {
        when:
            def result = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"invalid": "json"}'))

        then:
            result.andExpect(status().isBadRequest())
    }

    def 'should cancel hold successfully'() {
        given:
            cancelingHold.cancelHold(_ as CancelHoldCommand) >> Try.success(Result.Success)

        when:
            def result = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            result.andExpect(status().isNoContent())
    }

    def 'should return 404 when canceling non-existent hold'() {
        given:
            cancelingHold.cancelHold(_ as CancelHoldCommand) >> Try.failure(new IllegalArgumentException("Hold not found"))

        when:
            def result = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            result.andExpect(status().isNotFound())
    }

    def 'should return 500 when cancel hold fails with unexpected error'() {
        given:
            cancelingHold.cancelHold(_ as CancelHoldCommand) >> Try.failure(new RuntimeException("Unexpected error"))

        when:
            def result = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            result.andExpect(status().isInternalServerError())
    }

    def 'should verify correct command parameters for place hold'() {
        given:
            def request = new PlaceHoldRequest(bookId, libraryBranchId, 14)
            placingOnHold.placeOnHold(_ as PlaceOnHoldCommand) >> Try.success(Result.Success)

        when:
            mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
            1 * placingOnHold.placeOnHold({ PlaceOnHoldCommand cmd ->
                cmd.patronId.patronId == patronId &&
                cmd.bookId.bookId == bookId &&
                cmd.libraryId.libraryBranchId == libraryBranchId &&
                cmd.noOfDays == Option.of(14)
            })
    }

    def 'should verify correct command parameters for cancel hold'() {
        given:
            cancelingHold.cancelHold(_ as CancelHoldCommand) >> Try.success(Result.Success)

        when:
            mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            1 * cancelingHold.cancelHold({ CancelHoldCommand cmd ->
                cmd.patronId.patronId == patronId &&
                cmd.bookId.bookId == bookId
            })
    }

    def 'should verify HATEOAS affordances in hold response'() {
        given:
            def patronProfile = createTestPatronProfile()
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            result.andExpect(status().isOk())
                  .andExpect(jsonPath('$._links.self.href').exists())
    }
}
