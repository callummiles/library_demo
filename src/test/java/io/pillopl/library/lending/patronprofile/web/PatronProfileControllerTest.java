package io.pillopl.library.lending.patronprofile.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pillopl.library.catalogue.BookId;
import io.pillopl.library.commons.commands.Result;
import io.pillopl.library.lending.librarybranch.model.LibraryBranchId;
import io.pillopl.library.lending.patron.application.hold.CancelHoldCommand;
import io.pillopl.library.lending.patron.application.hold.CancelingHold;
import io.pillopl.library.lending.patron.application.hold.PlaceOnHoldCommand;
import io.pillopl.library.lending.patron.application.hold.PlacingOnHold;
import io.pillopl.library.lending.patron.model.PatronId;
import io.pillopl.library.lending.patronprofile.model.*;
import io.vavr.control.Try;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.context.annotation.Import;

@RunWith(SpringRunner.class)
@WebMvcTest(PatronProfileController.class)
@Import(PatronProfileController.class)
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

    private static final UUID PATRON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOOK_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID LIBRARY_BRANCH_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final Instant HOLD_TILL = Instant.parse("2025-12-31T23:59:59Z");

    @Test
    public void shouldReturnPatronProfile() throws Exception {
        mockMvc.perform(get("/profiles/{patronId}", PATRON_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patronId").value(PATRON_ID.toString()))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.holds.href").exists())
                .andExpect(jsonPath("$._links.checkouts.href").exists());
    }

    @Test
    public void shouldReturnHoldsCollection() throws Exception {
        io.pillopl.library.lending.patronprofile.model.Hold hold1 = new io.pillopl.library.lending.patronprofile.model.Hold(new BookId(BOOK_ID), HOLD_TILL);
        io.pillopl.library.lending.patronprofile.model.Hold hold2 = new io.pillopl.library.lending.patronprofile.model.Hold(new BookId(UUID.randomUUID()), HOLD_TILL.plusSeconds(3600));
        HoldsView holdsView = new HoldsView(io.vavr.collection.List.of(hold1, hold2));
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.empty());
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/holds/", PATRON_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.holdList").isArray())
                .andExpect(jsonPath("$._embedded.holdList.length()").value(2))
                .andExpect(jsonPath("$._embedded.holdList[0].bookId").value(BOOK_ID.toString()))
                .andExpect(jsonPath("$._embedded.holdList[0].till").exists())
                .andExpect(jsonPath("$._embedded.holdList[0]._links.self.href").exists())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    public void shouldReturnEmptyHoldsCollection() throws Exception {
        HoldsView holdsView = new HoldsView(io.vavr.collection.List.empty());
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.empty());
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/holds/", PATRON_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    public void shouldReturnSingleHold() throws Exception {
        io.pillopl.library.lending.patronprofile.model.Hold hold = new io.pillopl.library.lending.patronprofile.model.Hold(new BookId(BOOK_ID), HOLD_TILL);
        HoldsView holdsView = new HoldsView(io.vavr.collection.List.of(hold));
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.empty());
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", PATRON_ID, BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(BOOK_ID.toString()))
                .andExpect(jsonPath("$.till").value(HOLD_TILL.toString()))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    public void shouldReturn404WhenHoldNotFound() throws Exception {
        HoldsView holdsView = new HoldsView(io.vavr.collection.List.empty());
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.empty());
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", PATRON_ID, BOOK_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnCheckoutsCollection() throws Exception {
        io.pillopl.library.lending.patronprofile.model.Checkout checkout1 = new io.pillopl.library.lending.patronprofile.model.Checkout(new BookId(BOOK_ID), HOLD_TILL);
        io.pillopl.library.lending.patronprofile.model.Checkout checkout2 = new io.pillopl.library.lending.patronprofile.model.Checkout(new BookId(UUID.randomUUID()), HOLD_TILL.plusSeconds(3600));
        HoldsView holdsView = new HoldsView(io.vavr.collection.List.empty());
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.of(checkout1, checkout2));
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/checkouts/", PATRON_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.checkoutList").isArray())
                .andExpect(jsonPath("$._embedded.checkoutList.length()").value(2))
                .andExpect(jsonPath("$._embedded.checkoutList[0].bookId").value(BOOK_ID.toString()))
                .andExpect(jsonPath("$._embedded.checkoutList[0].till").exists())
                .andExpect(jsonPath("$._embedded.checkoutList[0]._links.self.href").exists())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    public void shouldReturnEmptyCheckoutsCollection() throws Exception {
        HoldsView holdsView = new HoldsView(io.vavr.collection.List.empty());
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.empty());
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/checkouts/", PATRON_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    public void shouldReturnSingleCheckout() throws Exception {
        io.pillopl.library.lending.patronprofile.model.Checkout checkout = new io.pillopl.library.lending.patronprofile.model.Checkout(new BookId(BOOK_ID), HOLD_TILL);
        HoldsView holdsView = new HoldsView(io.vavr.collection.List.empty());
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.of(checkout));
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", PATRON_ID, BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(BOOK_ID.toString()))
                .andExpect(jsonPath("$.till").value(HOLD_TILL.toString()))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    public void shouldReturn404WhenCheckoutNotFound() throws Exception {
        HoldsView holdsView = new HoldsView(io.vavr.collection.List.empty());
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.empty());
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/checkouts/{bookId}", PATRON_ID, BOOK_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldPlaceHoldSuccessfully() throws Exception {
        when(placingOnHold.placeOnHold(any(PlaceOnHoldCommand.class)))
                .thenReturn(Try.success(Result.Success));

        String requestJson = "{\"bookId\":\"" + BOOK_ID + "\",\"libraryBranchId\":\"" + LIBRARY_BRANCH_ID + "\",\"numberOfDays\":10}";

        mockMvc.perform(post("/profiles/{patronId}/holds", PATRON_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        ArgumentCaptor<PlaceOnHoldCommand> commandCaptor = ArgumentCaptor.forClass(PlaceOnHoldCommand.class);
        verify(placingOnHold).placeOnHold(commandCaptor.capture());
        PlaceOnHoldCommand capturedCommand = commandCaptor.getValue();
        assertThat(capturedCommand.getPatronId().getPatronId()).isEqualTo(PATRON_ID);
        assertThat(capturedCommand.getBookId().getBookId()).isEqualTo(BOOK_ID);
        assertThat(capturedCommand.getLibraryId().getLibraryBranchId()).isEqualTo(LIBRARY_BRANCH_ID);
        assertThat(capturedCommand.getNoOfDays().get()).isEqualTo(10);
    }

    @Test
    public void shouldReturn500WhenPlaceHoldFails() throws Exception {
        when(placingOnHold.placeOnHold(any(PlaceOnHoldCommand.class)))
                .thenReturn(Try.failure(new RuntimeException("Failed to place hold")));

        String requestJson = "{\"bookId\":\"" + BOOK_ID + "\",\"libraryBranchId\":\"" + LIBRARY_BRANCH_ID + "\",\"numberOfDays\":10}";

        mockMvc.perform(post("/profiles/{patronId}/holds", PATRON_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void shouldCancelHoldSuccessfully() throws Exception {
        when(cancelingHold.cancelHold(any(CancelHoldCommand.class)))
                .thenReturn(Try.success(Result.Success));

        mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", PATRON_ID, BOOK_ID))
                .andExpect(status().isNoContent());

        ArgumentCaptor<CancelHoldCommand> commandCaptor = ArgumentCaptor.forClass(CancelHoldCommand.class);
        verify(cancelingHold).cancelHold(commandCaptor.capture());
        CancelHoldCommand capturedCommand = commandCaptor.getValue();
        assertThat(capturedCommand.getPatronId().getPatronId()).isEqualTo(PATRON_ID);
        assertThat(capturedCommand.getBookId().getBookId()).isEqualTo(BOOK_ID);
    }

    @Test
    public void shouldReturn404WhenCancelHoldNotFound() throws Exception {
        when(cancelingHold.cancelHold(any(CancelHoldCommand.class)))
                .thenReturn(Try.failure(new IllegalArgumentException("Hold not found")));

        mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", PATRON_ID, BOOK_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturn500WhenCancelHoldFailsWithOtherError() throws Exception {
        when(cancelingHold.cancelHold(any(CancelHoldCommand.class)))
                .thenReturn(Try.failure(new RuntimeException("Unexpected error")));

        mockMvc.perform(delete("/profiles/{patronId}/holds/{bookId}", PATRON_ID, BOOK_ID))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void shouldDeserializePlaceHoldRequestCorrectly() throws Exception {
        when(placingOnHold.placeOnHold(any(PlaceOnHoldCommand.class)))
                .thenReturn(Try.success(Result.Success));

        String requestJson = "{\"bookId\":\"" + BOOK_ID + "\",\"libraryBranchId\":\"" + LIBRARY_BRANCH_ID + "\",\"numberOfDays\":5}";

        mockMvc.perform(post("/profiles/{patronId}/holds", PATRON_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        ArgumentCaptor<PlaceOnHoldCommand> commandCaptor = ArgumentCaptor.forClass(PlaceOnHoldCommand.class);
        verify(placingOnHold).placeOnHold(commandCaptor.capture());
        PlaceOnHoldCommand capturedCommand = commandCaptor.getValue();
        assertThat(capturedCommand.getNoOfDays().get()).isEqualTo(5);
    }

    @Test
    public void shouldIncludeCancelHoldAffordanceInHoldResponse() throws Exception {
        io.pillopl.library.lending.patronprofile.model.Hold hold = new io.pillopl.library.lending.patronprofile.model.Hold(new BookId(BOOK_ID), HOLD_TILL);
        HoldsView holdsView = new HoldsView(io.vavr.collection.List.of(hold));
        CheckoutsView checkoutsView = new CheckoutsView(io.vavr.collection.List.empty());
        PatronProfile patronProfile = new PatronProfile(holdsView, checkoutsView);

        when(patronProfiles.fetchFor(any(PatronId.class))).thenReturn(patronProfile);

        mockMvc.perform(get("/profiles/{patronId}/holds/{bookId}", PATRON_ID, BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists());
    }
}
