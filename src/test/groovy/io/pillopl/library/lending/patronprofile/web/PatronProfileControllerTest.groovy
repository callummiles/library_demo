package io.pillopl.library.lending.patronprofile.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.pillopl.library.catalogue.BookId
import io.pillopl.library.commons.commands.Result
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId
import io.pillopl.library.lending.patron.application.hold.CancelingHold
import io.pillopl.library.lending.patron.application.hold.PlacingOnHold
import io.pillopl.library.lending.patron.model.PatronId
import io.pillopl.library.lending.patronprofile.model.CheckoutsView
import io.pillopl.library.lending.patronprofile.model.HoldsView
import io.pillopl.library.lending.patronprofile.model.PatronProfile
import io.pillopl.library.lending.patronprofile.model.PatronProfiles
import io.vavr.collection.List
import io.vavr.control.Option
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

@WebMvcTest(PatronProfileController)
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

    static UUID patronId = UUID.fromString("12345678-1234-1234-1234-123456789012")
    static UUID bookId = UUID.fromString("87654321-4321-4321-4321-210987654321")
    static UUID libraryBranchId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    static Instant holdTill = Instant.parse("2025-12-31T23:59:59Z")

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

    def "should return holds collection when patron has holds"() {
        given:
            def hold = createMockHold()
            def holdsView = createMockHoldsView([hold])
            def patronProfile = createMockPatronProfile(holdsView, createEmptyCheckoutsView())
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath('$._embedded').exists())
                   .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return empty holds collection when patron has no holds"() {
        given:
            def patronProfile = createMockPatronProfile(createEmptyHoldsView(), createEmptyCheckoutsView())
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return specific hold when it exists"() {
        given:
            def hold = createMockHold()
            def patronProfile = createMockPatronProfile(createEmptyHoldsView(), createEmptyCheckoutsView())
            patronProfile.findHold(new BookId(bookId)) >> Option.of(hold)
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath('$.bookId').value(bookId.toString()))
                   .andExpect(jsonPath('$.till').exists())
                   .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return 404 when hold does not exist"() {
        given:
            def patronProfile = createMockPatronProfile(createEmptyHoldsView(), createEmptyCheckoutsView())
            patronProfile.findHold(new BookId(bookId)) >> Option.none()
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNotFound())
    }

    def "should return checkouts collection when patron has checkouts"() {
        given:
            def checkout = createMockCheckout()
            def checkoutsView = createMockCheckoutsView([checkout])
            def patronProfile = createMockPatronProfile(createEmptyHoldsView(), checkoutsView)
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath('$._embedded').exists())
                   .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return empty checkouts collection when patron has no checkouts"() {
        given:
            def patronProfile = createMockPatronProfile(createEmptyHoldsView(), createEmptyCheckoutsView())
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return specific checkout when it exists"() {
        given:
            def checkout = createMockCheckout()
            def patronProfile = createMockPatronProfile(createEmptyHoldsView(), createEmptyCheckoutsView())
            patronProfile.findCheckout(new BookId(bookId)) >> Option.of(checkout)
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath('$.bookId').value(bookId.toString()))
                   .andExpect(jsonPath('$.till').exists())
                   .andExpect(jsonPath('$._links.self.href').exists())
    }

    def "should return 404 when checkout does not exist"() {
        given:
            def patronProfile = createMockPatronProfile(createEmptyHoldsView(), createEmptyCheckoutsView())
            patronProfile.findCheckout(new BookId(bookId)) >> Option.none()
            patronProfiles.fetchFor(new PatronId(patronId)) >> patronProfile

        when:
            def response = mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNotFound())
    }

    def "should successfully place hold with valid request"() {
        given:
            def request = new PlaceHoldRequest(bookId, libraryBranchId, 7)
            def requestJson = objectMapper.writeValueAsString(request)
            placingOnHold.placeOnHold(_) >> Try.success(Result.Success)

        when:
            def response = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))

        then:
            response.andExpect(status().isOk())
    }

    def "should return 500 when place hold fails"() {
        given:
            def request = new PlaceHoldRequest(bookId, libraryBranchId, 7)
            def requestJson = objectMapper.writeValueAsString(request)
            placingOnHold.placeOnHold(_) >> Try.failure(new RuntimeException("Service failure"))

        when:
            def response = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))

        then:
            response.andExpect(status().isInternalServerError())
    }

    def "should return 400 for malformed place hold request"() {
        given:
            def malformedJson = '{"bookId": "invalid-uuid"}'

        when:
            def response = mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformedJson))

        then:
            response.andExpect(status().isBadRequest())
    }

    def "should successfully cancel hold"() {
        given:
            cancelingHold.cancelHold(_) >> Try.success(Result.Success)

        when:
            def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNoContent())
    }

    def "should return 404 when canceling non-existent hold"() {
        given:
            cancelingHold.cancelHold(_) >> Try.failure(new IllegalArgumentException("Hold not found"))

        when:
            def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isNotFound())
    }

    def "should return 500 when cancel hold fails with non-IllegalArgumentException"() {
        given:
            cancelingHold.cancelHold(_) >> Try.failure(new RuntimeException("Service failure"))

        when:
            def response = mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))

        then:
            response.andExpect(status().isInternalServerError())
    }

    private io.pillopl.library.lending.patronprofile.model.Hold createMockHold() {
        def mockHold = Stub(io.pillopl.library.lending.patronprofile.model.Hold)
        mockHold.getBook() >> new BookId(bookId)
        mockHold.getTill() >> holdTill
        return mockHold
    }

    private io.pillopl.library.lending.patronprofile.model.Checkout createMockCheckout() {
        def mockCheckout = Stub(io.pillopl.library.lending.patronprofile.model.Checkout)
        mockCheckout.getBook() >> new BookId(bookId)
        mockCheckout.getTill() >> holdTill
        return mockCheckout
    }

    private HoldsView createMockHoldsView(List<io.pillopl.library.lending.patronprofile.model.Hold> holds) {
        def mockHoldsView = Stub(HoldsView)
        mockHoldsView.getCurrentHolds() >> List.ofAll(holds)
        return mockHoldsView
    }

    private HoldsView createEmptyHoldsView() {
        def mockHoldsView = Stub(HoldsView)
        mockHoldsView.getCurrentHolds() >> List.empty()
        return mockHoldsView
    }

    private CheckoutsView createMockCheckoutsView(List<io.pillopl.library.lending.patronprofile.model.Checkout> checkouts) {
        def mockCheckoutsView = Stub(CheckoutsView)
        mockCheckoutsView.getCurrentCheckouts() >> List.ofAll(checkouts)
        return mockCheckoutsView
    }

    private CheckoutsView createEmptyCheckoutsView() {
        def mockCheckoutsView = Stub(CheckoutsView)
        mockCheckoutsView.getCurrentCheckouts() >> List.empty()
        return mockCheckoutsView
    }

    private PatronProfile createMockPatronProfile(HoldsView holdsView, CheckoutsView checkoutsView) {
        def mockPatronProfile = Stub(PatronProfile)
        mockPatronProfile.getHoldsView() >> holdsView
        mockPatronProfile.getCurrentCheckouts() >> checkoutsView
        return mockPatronProfile
    }

    @TestConfiguration
    static class MockConfig {
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
}
