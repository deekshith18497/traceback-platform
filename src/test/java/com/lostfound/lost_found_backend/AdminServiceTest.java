
package com.lostfound.lost_found_backend;

import com.lostfound.entity.LostItem;
import com.lostfound.entity.Match;
import com.lostfound.entity.User;
import com.lostfound.repository.FoundItemRepository;
import com.lostfound.repository.LostItemRepository;
import com.lostfound.repository.MatchRepository;
import com.lostfound.repository.UserRepository;
import com.lostfound.service.AdminService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LostItemRepository lostItemRepository;

    @Mock
    private FoundItemRepository foundItemRepository;

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void dashboardCountsUsersItemsAndMatches() {
        User user = new User();
        user.setEnabled(true);

        Match pending = new Match();
        pending.setStatus("PENDING");

        Match confirmed = new Match();
        confirmed.setStatus("CONFIRMED");

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(lostItemRepository.count()).thenReturn(4L);
        when(foundItemRepository.count()).thenReturn(3L);
        when(matchRepository.findAll())
                .thenReturn(List.of(pending, confirmed));

        var result = adminService.getDashboard();

        assertEquals(1L, result.getTotalUsers());
        assertEquals(1L, result.getEnabledUsers());
        assertEquals(4L, result.getTotalLostItems());
        assertEquals(3L, result.getTotalFoundItems());
        assertEquals(2L, result.getTotalMatches());
        assertEquals(1L, result.getPendingMatches());
        assertEquals(1L, result.getConfirmedMatches());
    }

    @Test
    void missingUserCannotBeDisabled() {
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> adminService.setUserEnabled(999L, false));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void adminAccountCannotBeDisabled() {
        User admin = new User();
        admin.setRole(User.Role.ADMIN);
        admin.setEnabled(true);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        assertThrows(
                IllegalArgumentException.class,
                () -> adminService.setUserEnabled(1L, false));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void invalidModerationStatusIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> adminService.setLostItemStatus(1L, "DELETED"));

        verify(lostItemRepository, never()).findById(anyLong());
    }

    @Test
    void lostItemCanBeClosedByAdmin() {
        LostItem item = new LostItem();
        item.setId(1L);
        item.setStatus("ACTIVE");

        when(lostItemRepository.findById(1L))
                .thenReturn(Optional.of(item));
        when(lostItemRepository.save(any(LostItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LostItem result =
                adminService.setLostItemStatus(1L, "CLOSED");

        assertEquals("CLOSED", result.getStatus());
        verify(lostItemRepository).save(item);
    }
}
