package io.pillopl.library.lending.patronprofile.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.pillopl.library.catalogue.BookId
import io.pillopl.library.commons.commands.Result
import io.pillopl.library.lending.patron.application.hold.CancelingHold
import io.pillopl.library.lending.patron.application.hold.PlacingOnHold
import io.pillopl.library.lending.patron.model.PatronId
import io.pillopl.library.lending.patronprofile.model.*
import io.vavr.collection.List
import io.vavr.control.Try
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification
import spock.mock.DetachedMockFactory

import java.time.Instant

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(PatronProfileController)
@ContextConfiguration(classes = [TestConfig])
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

    def patronId = UUID.randomUUID()
    def bookId = UUID.randomUUID()
    def libraryBranchId = UUID.randomUUID()

    @TestConfiguration
    static class TestConfig {
        def mockFactory = new DetachedMockFactory()

        @Bean
        PatronProfiles patronProfiles() {
            return mockFactory.Stub(PatronProfiles)
        }

        @Bean
        PlacingOnHold placingOnHold() {
            return mockFactory.Stub(PlacingOnHold)
        }

        @Bean
        CancelingHold cancelingHold() {
            return mockFactory.Stub(CancelingHold)
        }
    }

    def 'should return patron profile with HATEOAS links'() {
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

    def 'should return holds collection with HATEOAS links'() {
        given:
            def hold = new Hold(new BookId(bookId), Instant.now().plusDays(7))
            def patronProfile = new PatronProfile(
                new HoldsView(List.of(hold)),
                new CheckoutsView(List.empty())
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath('$._embedded').exists())
                   .andExpect(jsonPath('$._links.self.href').exists())
                   .andExpect(jsonPath('$._embedded.holdList[0].bookId').value(bookId.toString()))
                   .andExpect(jsonPath('$._embedded.holdList[0]._links.self.href').exists())
    }

    def 'should return empty holds collection when patron has no holds'() {
        given:
            def patronProfile = new PatronProfile(
                new HoldsView(List.empty()),
                new CheckoutsView(List.empty())
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return individual hold when found'() {
        given:
            def hold = new Hold(new BookId(bookId), Instant.now().plusDays(7))
            def patronProfile = new PatronProfile(
                new HoldsView(List.of(hold)),
                new CheckoutsView(List.empty())
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(jsonPath('$.bookId').value(bookId.toString()))
                   .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return 404 when hold not found'() {
        given:
            def patronProfile = new PatronProfile(
                new HoldsView(List.empty()),
                new CheckoutsView(List.empty())
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNotFound())
    }

    def 'should successfully place hold'() {
        given:
            def request = new PlaceHoldRequest(bookId, libraryBranchId, 7)
            placingOnHold.placeOnHold(_) >> Try.success(Result.Success)

        when:
            def response = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
            response.andExpect(status().isOk())
    }

    def 'should return 500 when place hold fails'() {
        given:
            def request = new PlaceHoldRequest(bookId, libraryBranchId, 7)
            placingOnHold.placeOnHold(_) >> Try.failure(new RuntimeException("Service error"))

        when:
            def response = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
            response.andExpect(status().isInternalServerError())
    }

    def 'should handle invalid JSON in place hold request'() {
        when:
            def response = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))

        then:
            response.andExpect(status().isBadRequest())
    }

    def 'should successfully cancel hold'() {
        given:
            cancelingHold.cancelHold(_) >> Try.success(Result.Success)

        when:
            def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNoContent())
    }

    def 'should return 404 when canceling non-existent hold'() {
        given:
            cancelingHold.cancelHold(_) >> Try.failure(new IllegalArgumentException("Hold not found"))

        when:
            def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNotFound())
    }

    def 'should return 500 when cancel hold fails with other exception'() {
        given:
            cancelingHold.cancelHold(_) >> Try.failure(new RuntimeException("Service error"))

        when:
            def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isInternalServerError())
    }

    def 'should return checkouts collection'() {
        given:
            def checkout = new Checkout(new BookId(bookId), Instant.now().plusDays(14))
            def patronProfile = new PatronProfile(
                new HoldsView(List.empty()),
                new CheckoutsView(List.of(checkout))
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath('$._embedded.checkoutList[0].bookId').value(bookId.toString()))
                   .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return empty checkouts collection when patron has no checkouts'() {
        given:
            def patronProfile = new PatronProfile(
                new HoldsView(List.empty()),
                new CheckoutsView(List.empty())
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return individual checkout when found'() {
        given:
            def checkout = new Checkout(new BookId(bookId), Instant.now().plusDays(14))
            def patronProfile = new PatronProfile(
                new HoldsView(List.empty()),
                new CheckoutsView(List.of(checkout))
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath('$.bookId').value(bookId.toString()))
                   .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should return 404 when checkout not found'() {
        given:
            def patronProfile = new PatronProfile(
                new HoldsView(List.empty()),
                new CheckoutsView(List.empty())
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNotFound())
    }

    def 'should handle holds with affordances for cancellation'() {
        given:
            def hold = new Hold(new BookId(bookId), Instant.now().plusDays(7))
            def patronProfile = new PatronProfile(
                new HoldsView(List.of(hold)),
                new CheckoutsView(List.empty())
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(jsonPath('$._links.self.href').exists())
    }

    def 'should handle multiple holds in collection'() {
        given:
            def hold1 = new Hold(new BookId(bookId), Instant.now().plusDays(7))
            def hold2 = new Hold(new BookId(UUID.randomUUID()), Instant.now().plusDays(5))
            def patronProfile = new PatronProfile(
                new HoldsView(List.of(hold1, hold2)),
                new CheckoutsView(List.empty())
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(jsonPath('$._embedded.holdList').isArray())
                   .andExpect(jsonPath('$._embedded.holdList.length()').value(2))
    }

    def 'should handle multiple checkouts in collection'() {
        given:
            def checkout1 = new Checkout(new BookId(bookId), Instant.now().plusDays(14))
            def checkout2 = new Checkout(new BookId(UUID.randomUUID()), Instant.now().plusDays(10))
            def patronProfile = new PatronProfile(
                new HoldsView(List.empty()),
                new CheckoutsView(List.of(checkout1, checkout2))
            )
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(jsonPath('$._embedded.checkoutList').isArray())
                   .andExpect(jsonPath('$._embedded.checkoutList.length()').value(2))
    }
}
