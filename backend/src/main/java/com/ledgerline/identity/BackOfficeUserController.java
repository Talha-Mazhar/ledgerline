package com.ledgerline.identity;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The back office's view of users. Every {@code /back-office} path is for staff only (see {@link
 * SecurityConfiguration}); changing a role is for admins only, which {@code @PreAuthorize} checks
 * on the method itself.
 */
@RestController
@RequestMapping("/api/v1/back-office/users")
@Validated
class BackOfficeUserController {

    record UserListResponse(List<StaffViewOfUser> items) {}

    record StaffViewOfUser(UUID id, String email, String displayName, Role role) {
        static StaffViewOfUser from(UserSummary user) {
            return new StaffViewOfUser(user.id(), user.email(), user.displayName(), user.role());
        }
    }

    record ChangeRoleRequest(Role role) {
        ChangeRoleRequest {
            Objects.requireNonNull(role, "role is required");
        }
    }

    private final UserAccounts accounts;

    BackOfficeUserController(UserAccounts accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    UserListResponse listUsers(@RequestParam(defaultValue = "") String query) {
        return new UserListResponse(accounts.search(query, 50).stream()
                .map(StaffViewOfUser::from)
                .toList());
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    StaffViewOfUser changeRole(
            @PathVariable UUID userId,
            @RequestBody ChangeRoleRequest body,
            @AuthenticationPrincipal SignedInUser admin) {
        return StaffViewOfUser.from(accounts.changeRole(admin.id(), userId, body.role()));
    }
}
