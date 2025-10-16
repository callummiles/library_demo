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
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification
import spock.mock.DetachedMockFactory

import java.time.Instant

import static io.pillopl.library.lending.book.model.BookFixture.anyBookId
import static io.pillopl.library.lending.librarybranch.model.LibraryBranchFixture.anyBranch
import static io.pillopl.library.lending.patron.model.PatronFixture.anyPatronId
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(PatronProfileController)
class PatronProfileControllerTest extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    PatronProfiles patronProfiles

    @Autowired
    PlacingOnHold placingOnHold

    @Autowired
    CancelingHold cancelingHold

    @TestConfiguration
    static class MockConfig {
        DetachedMockFactory detachedMockFactory = new DetachedMockFactory()

        @Bean
        PatronProfiles patronProfiles() {
            return detachedMockFactory.Mock(PatronProfiles)
        }

        @Bean
        PlacingOnHold placingOnHold() {
            return detachedMockFactory.Mock(PlacingOnHold)
        }

        @Bean
        CancelingHold cancelingHold() {
            return detachedMockFactory.Mock(CancelingHold)
        }
    }

    def "should return patron profile with HATEOAS links"() {
        given:
            def patronId = UUID.randomUUID()
        when:
            def result = mockMvc.perform(get("/profiles/{patronId}", patronId))
        then:
            result.andExpect(status().isOk())
                  .andExpect(jsonPath('$.patronId').value(patronId.toString()))
                  .andExpect(jsonPath('$._links.holds.href').exists())
                  .andExpect(jsonPath('$._links.checkouts.href').exists())
                  .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return all holds for a patron"() {
        given:
            def patronId = anyPatronId()
            def bookId1 = anyBookId()
            def bookId2 = anyBookId()
            def till = Instant.now()

            def hold1 = new Hold(bookId1, till)
            def hold2 = new Hold(bookId2, till)
            def holdsView = new HoldsView(io.vavr.collection.List.of(hold1, hold2))
            def checkoutsView = new CheckoutsView(io.vavr.collection.List.empty())
            def patronProfile = new PatronProfile(holdsView, checkoutsView)
        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId.getPatronId()))
        then:
            1 * patronProfiles.fetchFor(patronId) >> patronProfile
            result.andExpect(status().isOk())
                  .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return empty collection when patron has no holds"() {
        given:
            def patronId = anyPatronId()
            def holdsView = new HoldsView(io.vavr.collection.List.empty())
            def checkoutsView = new CheckoutsView(io.vavr.collection.List.empty())
            def patronProfile = new PatronProfile(holdsView, checkoutsView)
        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId.getPatronId()))
        then:
            1 * patronProfiles.fetchFor(patronId) >> patronProfile
            result.andExpect(status().isOk())
                  .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return specific hold when it exists"() {
        given:
            def patronId = anyPatronId()
            def bookId = anyBookId()
            def till = Instant.now()

            def hold = new Hold(bookId, till)
            def holdsView = new HoldsView(io.vavr.collection.List.of(hold))
            def checkoutsView = new CheckoutsView(io.vavr.collection.List.empty())
            def patronProfile = new PatronProfile(holdsView, checkoutsView)
        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}",
                patronId.getPatronId(), bookId.getBookId()))
        then:
            1 * patronProfiles.fetchFor(patronId) >> patronProfile
            result.andExpect(status().isOk())
                  .andExpect(jsonPath('$.bookId').value(bookId.getBookId().toString()))
                  .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return 404 when hold does not exist"() {
        given:
            def patronId = anyPatronId()
            def bookId = anyBookId()
            def holdsView = new HoldsView(io.vavr.collection.List.empty())
            def checkoutsView = new CheckoutsView(io.vavr.collection.List.empty())
            def patronProfile = new PatronProfile(holdsView, checkoutsView)
        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}",
                patronId.getPatronId(), bookId.getBookId()))
        then:
            1 * patronProfiles.fetchFor(patronId) >> patronProfile
            result.andExpect(status().isNotFound())
    }

    def "should return all checkouts for a patron"() {
        given:
            def patronId = anyPatronId()
            def bookId1 = anyBookId()
            def bookId2 = anyBookId()
            def till = Instant.now()

            def checkout1 = new Checkout(bookId1, till)
            def checkout2 = new Checkout(bookId2, till)
            def holdsView = new HoldsView(io.vavr.collection.List.empty())
            def checkoutsView = new CheckoutsView(io.vavr.collection.List.of(checkout1, checkout2))
            def patronProfile = new PatronProfile(holdsView, checkoutsView)
        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId.getPatronId()))
        then:
            1 * patronProfiles.fetchFor(patronId) >> patronProfile
            result.andExpect(status().isOk())
                  .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return empty collection when patron has no checkouts"() {
        given:
            def patronId = anyPatronId()
            def holdsView = new HoldsView(io.vavr.collection.List.empty())
            def checkoutsView = new CheckoutsView(io.vavr.collection.List.empty())
            def patronProfile = new PatronProfile(holdsView, checkoutsView)
        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId.getPatronId()))
        then:
            1 * patronProfiles.fetchFor(patronId) >> patronProfile
            result.andExpect(status().isOk())
                  .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return specific checkout when it exists"() {
        given:
            def patronId = anyPatronId()
            def bookId = anyBookId()
            def till = Instant.now()

            def checkout = new Checkout(bookId, till)
            def holdsView = new HoldsView(io.vavr.collection.List.empty())
            def checkoutsView = new CheckoutsView(io.vavr.collection.List.of(checkout))
            def patronProfile = new PatronProfile(holdsView, checkoutsView)
        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}",
                patronId.getPatronId(), bookId.getBookId()))
        then:
            1 * patronProfiles.fetchFor(patronId) >> patronProfile
            result.andExpect(status().isOk())
                  .andExpect(jsonPath('$.bookId').value(bookId.getBookId().toString()))
                  .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return 404 when checkout does not exist"() {
        given:
            def patronId = anyPatronId()
            def bookId = anyBookId()
            def holdsView = new HoldsView(io.vavr.collection.List.empty())
            def checkoutsView = new CheckoutsView(io.vavr.collection.List.empty())
            def patronProfile = new PatronProfile(holdsView, checkoutsView)
        when:
            def result = mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}",
                patronId.getPatronId(), bookId.getBookId()))
        then:
            1 * patronProfiles.fetchFor(patronId) >> patronProfile
            result.andExpect(status().isNotFound())
    }

    def "should successfully place a hold"() {
        given:
            def patronId = UUID.randomUUID()
            def bookId = anyBookId()
            def libraryBranchId = anyBranch()
            def request = [
                bookId: bookId.getBookId(),
                libraryBranchId: libraryBranchId.getLibraryBranchId(),
                numberOfDays: 5
            ]
        when:
            def result = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        then:
            1 * placingOnHold.placeOnHold(_) >> Try.success(Result.Success)
            result.andExpect(status().isOk())
    }

    def "should return 500 when placing hold fails"() {
        given:
            def patronId = UUID.randomUUID()
            def bookId = anyBookId()
            def libraryBranchId = anyBranch()
            def request = [
                bookId: bookId.getBookId(),
                libraryBranchId: libraryBranchId.getLibraryBranchId(),
                numberOfDays: 5
            ]
        when:
            def result = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        then:
            1 * placingOnHold.placeOnHold(_) >> Try.failure(new RuntimeException("Service error"))
            result.andExpect(status().isInternalServerError())
    }

    def "should successfully cancel a hold"() {
        given:
            def patronId = UUID.randomUUID()
            def bookId = UUID.randomUUID()
        when:
            def result = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))
        then:
            1 * cancelingHold.cancelHold(_) >> Try.success(Result.Success)
            result.andExpect(status().isNoContent())
    }

    def "should return 404 when canceling hold that does not exist (IllegalArgumentException)"() {
        given:
            def patronId = UUID.randomUUID()
            def bookId = UUID.randomUUID()
        when:
            def result = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))
        then:
            1 * cancelingHold.cancelHold(_) >> Try.failure(new IllegalArgumentException("Hold not found"))
            result.andExpect(status().isNotFound())
    }

    def "should return 500 when canceling hold fails with other exception"() {
        given:
            def patronId = UUID.randomUUID()
            def bookId = UUID.randomUUID()
        when:
            def result = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))
        then:
            1 * cancelingHold.cancelHold(_) >> Try.failure(new RuntimeException("Service error"))
            result.andExpect(status().isInternalServerError())
    }
}
