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
import io.vavr.control.Try
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification
import spock.mock.DetachedMockFactory

import java.time.Instant

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(PatronProfileController.class)
class PatronProfileControllerTest extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    PatronProfiles patronProfiles

    @Autowired
    PlacingOnHold placingOnHold

    @Autowired
    CancelingHold cancelingHold

    @Autowired
    ObjectMapper objectMapper

    UUID patronId = UUID.randomUUID()
    UUID bookId = UUID.randomUUID()
    UUID libraryBranchId = UUID.randomUUID()
    Instant now = Instant.now()

    def 'should return patron profile with HATEOAS links'() {
        when:
            def result = mockMvc.perform(get("/profiles/{patronId}", patronId))

        then:
            result.andExpect(status().isOk())
                .andExpect(jsonPath('$.patronId').value(patronId.toString()))
                .andExpect(jsonPath('$._links.self.href').exists())
                .andExpect(jsonPath('$._links.holds.href').exists())
                .andExpect(jsonPath('$._links.checkouts.href').exists())
    }

    def 'should return empty holds collection when patron has no holds'() {
        given:
            PatronProfile profile = new PatronProfile(
                new HoldsView(List.empty()),
                new CheckoutsView(List.empty())
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> profile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))

        then:
            result.andExpect(status().isOk())
                .andExpect(jsonPath('$._embedded').doesNotExist())
                .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return holds collection with HATEOAS links'() {
        given:
            Hold hold1 = new Hold(new BookId(bookId), now)
            Hold hold2 = new Hold(new BookId(UUID.randomUUID()), now.plusSeconds(3600))
            PatronProfile profile = new PatronProfile(
                new HoldsView(List.of(hold1, hold2)),
                new CheckoutsView(List.empty())
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> profile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))

        then:
            result.andExpect(status().isOk())
                .andExpect(jsonPath('$._embedded.holdList').isArray())
                .andExpect(jsonPath('$._embedded.holdList[0].bookId').value(bookId.toString()))
                .andExpect(jsonPath('$._embedded.holdList[0]._links.self.href').exists())
                .andExpect(jsonPath('$._embedded.holdList[1]._links.self.href').exists())
                .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return single hold when it exists'() {
        given:
            Hold hold = new Hold(new BookId(bookId), now)
            PatronProfile profile = new PatronProfile(
                new HoldsView(List.of(hold)),
                new CheckoutsView(List.empty())
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> profile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            result.andExpect(status().isOk())
                .andExpect(jsonPath('$.bookId').value(bookId.toString()))
                .andExpect(jsonPath('$.till').exists())
                .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return 404 when hold does not exist'() {
        given:
            PatronProfile profile = new PatronProfile(
                new HoldsView(List.empty()),
                new CheckoutsView(List.empty())
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> profile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            result.andExpect(status().isNotFound())
    }

    def 'should return empty checkouts collection when patron has no checkouts'() {
        given:
            PatronProfile profile = new PatronProfile(
                new HoldsView(List.empty()),
                new CheckoutsView(List.empty())
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> profile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))

        then:
            result.andExpect(status().isOk())
                .andExpect(jsonPath('$._embedded').doesNotExist())
                .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return checkouts collection with HATEOAS links'() {
        given:
            Checkout checkout1 = new Checkout(new BookId(bookId), now)
            Checkout checkout2 = new Checkout(new BookId(UUID.randomUUID()), now.plusSeconds(7200))
            PatronProfile profile = new PatronProfile(
                new HoldsView(List.empty()),
                new CheckoutsView(List.of(checkout1, checkout2))
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> profile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))

        then:
            result.andExpect(status().isOk())
                .andExpect(jsonPath('$._embedded.checkoutList').isArray())
                .andExpect(jsonPath('$._embedded.checkoutList[0].bookId').value(bookId.toString()))
                .andExpect(jsonPath('$._embedded.checkoutList[0]._links.self.href').exists())
                .andExpect(jsonPath('$._embedded.checkoutList[1]._links.self.href').exists())
                .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return single checkout when it exists'() {
        given:
            Checkout checkout = new Checkout(new BookId(bookId), now)
            PatronProfile profile = new PatronProfile(
                new HoldsView(List.empty()),
                new CheckoutsView(List.of(checkout))
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> profile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", patronId, bookId))

        then:
            result.andExpect(status().isOk())
                .andExpect(jsonPath('$.bookId').value(bookId.toString()))
                .andExpect(jsonPath('$.till').exists())
                .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return 404 when checkout does not exist'() {
        given:
            PatronProfile profile = new PatronProfile(
                new HoldsView(List.empty()),
                new CheckoutsView(List.empty())
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> profile

        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", patronId, bookId))

        then:
            result.andExpect(status().isNotFound())
    }

    def 'should successfully place hold on book'() {
        given:
            placingOnHold.placeOnHold(_ as PlaceOnHoldCommand) >> Try.success(Result.Success)
            def requestBody = [
                bookId: bookId.toString(),
                libraryBranchId: libraryBranchId.toString(),
                numberOfDays: 5
            ]

        when:
            def result = mockMvc.perform(
                post("/profiles/{patronId}/holds", patronId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )

        then:
            result.andExpect(status().isOk())
    }

    def 'should return 500 when placing hold fails'() {
        given:
            placingOnHold.placeOnHold(_ as PlaceOnHoldCommand) >> Try.failure(new RuntimeException("Service failure"))
            def requestBody = [
                bookId: bookId.toString(),
                libraryBranchId: libraryBranchId.toString(),
                numberOfDays: 5
            ]

        when:
            def result = mockMvc.perform(
                post("/profiles/{patronId}/holds", patronId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )

        then:
            result.andExpect(status().isInternalServerError())
    }

    def 'should successfully cancel hold'() {
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

    def 'should return 500 when canceling hold fails with unexpected error'() {
        given:
            cancelingHold.cancelHold(_ as CancelHoldCommand) >> Try.failure(new RuntimeException("Unexpected error"))

        when:
            def result = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            result.andExpect(status().isInternalServerError())
    }

    def 'should map PlaceHoldRequest correctly with all fields'() {
        given:
            PlaceOnHoldCommand capturedCommand = null
            placingOnHold.placeOnHold(_ as PlaceOnHoldCommand) >> { PlaceOnHoldCommand cmd ->
                capturedCommand = cmd
                return Try.success(Result.Success)
            }
            def requestBody = [
                bookId: bookId.toString(),
                libraryBranchId: libraryBranchId.toString(),
                numberOfDays: 7
            ]

        when:
            mockMvc.perform(
                post("/profiles/{patronId}/holds", patronId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )

        then:
            capturedCommand != null
            capturedCommand.patronId.patronId == patronId
            capturedCommand.bookId.bookId == bookId
            capturedCommand.libraryBranchId.libraryBranchId == libraryBranchId
            capturedCommand.holdDuration.get() == 7
    }

    def 'should handle PlaceHoldRequest with null numberOfDays'() {
        given:
            placingOnHold.placeOnHold(_ as PlaceOnHoldCommand) >> Try.success(Result.Success)
            def requestBody = [
                bookId: bookId.toString(),
                libraryBranchId: libraryBranchId.toString(),
                numberOfDays: null
            ]

        when:
            def result = mockMvc.perform(
                post("/profiles/{patronId}/holds", patronId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )

        then:
            result.andExpect(status().isOk())
    }

    @TestConfiguration
    static class MockConfig {
        DetachedMockFactory detachedMockFactory = new DetachedMockFactory()

        @Bean
        PatronProfiles patronProfiles() {
            return detachedMockFactory.Stub(PatronProfiles)
        }

        @Bean
        PlacingOnHold placingOnHold() {
            return detachedMockFactory.Stub(PlacingOnHold)
        }

        @Bean
        CancelingHold cancelingHold() {
            return detachedMockFactory.Stub(CancelingHold)
        }
    }
}
