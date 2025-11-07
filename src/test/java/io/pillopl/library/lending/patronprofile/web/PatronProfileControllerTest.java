package io.pillopl.library.lending.patronprofile.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pillopl.library.catalogue.BookId;
import io.pillopl.library.commons.commands.Result;
import io.pillopl.library.lending.patron.application.hold.CancelHoldCommand;
import io.pillopl.library.lending.patron.application.hold.CancelingHold;
import io.pillopl.library.lending.patron.application.hold.PlaceOnHoldCommand;
import io.pillopl.library.lending.patron.application.hold.PlacingOnHold;
import io.pillopl.library.lending.patron.model.PatronId;
import io.pillopl.library.lending.patronprofile.model.*;
import io.vavr.control.Try;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(PatronProfileController.class)
public class PatronProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PatronProfiles patronProfiles;

    @MockBean
    private PlacingOnHold placingOnHold;

    @MockBean
    private CancelingHold cancelingHold;

    @Test
    public void shouldReturnPatronProfileWithHateoasLinks() throws Exception {
        UUID patronId = UUID.randomUUID();

        mockMvc.perform(get("/profiles/{patronId}", patronId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patronId").value(patronId.toString()))
                .andExpect(jsonPath("$._links.holds.href").exists())
                .andExpect(jsonPath("$._links.checkouts.href").exists())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    public void shouldReturnCollectionOfHolds() throws Exception {
        UUID patronId = UUID.randomUUID();
        UUID bookId1 = UUID.randomUUID();
        UUID bookId2 = UUID.randomUUID();
        Instant now = Instant.now();

        Hold hold1 = new Hold(new BookId(bookId1), now);
        Hold hold2 = new Hold(new BookId(bookId2), now.plusSeconds(3600));
        HoldsView holdsView = new HoldsView(io.vavr.collection.List.of(hold1, hold2));
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.empty());
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.holdList", hasSize(2)))
                .andExpect(jsonPath("$._embedded.holdList[0].bookId").value(bookId1.toString()))
                .andExpect(jsonPath("$._embedded.holdList[1].bookId").value(bookId2.toString()))
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(patronProfiles).fetchFor(any(PatronId.class));
    }

    @Test
    public void shouldReturnEmptyCollectionWhenNoHolds() throws Exception {
        UUID patronId = UUID.randomUUID();

        HoldsView holdsView = new HoldsView(io.vavr.collection.List.empty());
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.empty());
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/holds/", patronId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    public void shouldReturnSingleHoldWhenFound() throws Exception {
        UUID patronId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Instant now = Instant.now();

        Hold hold = new Hold(new BookId(bookId), now);
        HoldsView holdsView = new HoldsView(io.vavr.collection.List.of(hold));
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.empty());
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(bookId.toString()))
                .andExpect(jsonPath("$.till").exists())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    public void shouldReturn404WhenHoldNotFound() throws Exception {
        UUID patronId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        HoldsView holdsView = new HoldsView(io.vavr.collection.List.empty());
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.empty());
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", patronId, bookId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnCollectionOfCheckouts() throws Exception {
        UUID patronId = UUID.randomUUID();
        UUID bookId1 = UUID.randomUUID();
        UUID bookId2 = UUID.randomUUID();
        Instant now = Instant.now();

        Checkout checkout1 = new Checkout(new BookId(bookId1), now);
        Checkout checkout2 = new Checkout(new BookId(bookId2), now.plusSeconds(7200));
        HoldsView holdsView = new HoldsView(io.vavr.collection.List.empty());
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.of(checkout1, checkout2));
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.checkoutList", hasSize(2)))
                .andExpect(jsonPath("$._embedded.checkoutList[0].bookId").value(bookId1.toString()))
                .andExpect(jsonPath("$._embedded.checkoutList[1].bookId").value(bookId2.toString()))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    public void shouldReturnEmptyCollectionWhenNoCheckouts() throws Exception {
        UUID patronId = UUID.randomUUID();

        HoldsView holdsView = new HoldsView(io.vavr.collection.List.empty());
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.empty());
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/checkouts/", patronId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    public void shouldReturnSingleCheckoutWhenFound() throws Exception {
        UUID patronId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Instant now = Instant.now();

        Checkout checkout = new Checkout(new BookId(bookId), now);
        HoldsView holdsView = new HoldsView(io.vavr.collection.List.empty());
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.of(checkout));
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", patronId, bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(bookId.toString()))
                .andExpect(jsonPath("$.till").exists())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    public void shouldReturn404WhenCheckoutNotFound() throws Exception {
        UUID patronId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        HoldsView holdsView = new HoldsView(io.vavr.collection.List.empty());
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.empty());
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", patronId, bookId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldPlaceHoldSuccessfully() throws Exception {
        UUID patronId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID libraryBranchId = UUID.randomUUID();
        Integer numberOfDays = 5;

        PlaceHoldRequest request = new PlaceHoldRequest(bookId, libraryBranchId, numberOfDays);

        when(placingOnHold.placeOnHold(any(PlaceOnHoldCommand.class)))
                .thenReturn(Try.success(Result.Success));

        mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(placingOnHold).placeOnHold(any(PlaceOnHoldCommand.class));
    }

    @Test
    public void shouldReturn500WhenPlaceHoldFails() throws Exception {
        UUID patronId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID libraryBranchId = UUID.randomUUID();
        Integer numberOfDays = 5;

        PlaceHoldRequest request = new PlaceHoldRequest(bookId, libraryBranchId, numberOfDays);

        when(placingOnHold.placeOnHold(any(PlaceOnHoldCommand.class)))
                .thenReturn(Try.failure(new RuntimeException("Service failure")));

        mockMvc.perform(post("/profiles/{patronId}/holds", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void shouldCancelHoldSuccessfully() throws Exception {
        UUID patronId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        when(cancelingHold.cancelHold(any(CancelHoldCommand.class)))
                .thenReturn(Try.success(Result.Success));

        mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))
                .andExpect(status().isNoContent());

        verify(cancelingHold).cancelHold(any(CancelHoldCommand.class));
    }

    @Test
    public void shouldReturn404WhenCancelHoldNotFound() throws Exception {
        UUID patronId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        when(cancelingHold.cancelHold(any(CancelHoldCommand.class)))
                .thenReturn(Try.failure(new IllegalArgumentException("Hold not found")));

        mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturn500WhenCancelHoldFailsWithOtherError() throws Exception {
        UUID patronId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        when(cancelingHold.cancelHold(any(CancelHoldCommand.class)))
                .thenReturn(Try.failure(new RuntimeException("Service failure")));

        mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", patronId, bookId))
                .andExpect(status().isInternalServerError());
    }
}
