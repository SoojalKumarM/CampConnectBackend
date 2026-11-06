package com.campusconnect.dto;

import com.campusconnect.entity.Gender;
import com.campusconnect.entity.LookingFor;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class Dtos {
    private Dtos() {}

    // ---- auth ----
    public record RegisterRequest(
            @NotBlank(message = "Full name is required") @Size(max = 80) String fullName,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String email,
            @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String password,
            @NotBlank(message = "WhatsApp number is required") String whatsappNumber) {}

    public record LoginRequest(@NotBlank(message = "Email is required") String email,
                               @NotBlank(message = "Password is required") String password) {}

    public record GoogleLoginRequest(@NotBlank(message = "Missing Google credential") String credential) {}

    public record MeDto(Long id, String email, String fullName, String photoUrl, boolean hasWhatsapp,
                        boolean verifiedStudent, boolean profileComplete, boolean visible) {}

    public record AuthResponse(String token, MeDto user) {}

    // ---- profile ----
    public record ProfileUpdateRequest(
            @NotBlank(message = "Full name is required") @Size(max = 80) String fullName,
            LocalDate birthDate,
            Gender gender,
            LookingFor lookingFor,
            @Size(max = 120) String university,
            @Size(max = 120) String department,
            @Min(1) @Max(8) Integer yearOfStudy,
            @Size(max = 500, message = "Bio can be at most 500 characters") String bio,
            Set<@Size(max = 40) String> interests,
            String whatsappNumber) {}

    /** What others see. whatsappNumber/whatsappLink are ONLY filled when relation == CONNECTED. */
    public record ProfileDto(Long userId, String fullName, Integer age, Gender gender, LookingFor lookingFor,
                             String university, String department, Integer yearOfStudy, String bio,
                             List<String> interests, List<String> photos, boolean verifiedStudent,
                             Integer compatibility, List<String> sharedInterests,
                             String relation, Long requestId, String whatsappNumber, String whatsappLink) {}

    /** The owner's own view (editable fields). */
    public record MyProfileDto(Long userId, String email, String fullName, LocalDate birthDate, Gender gender,
                               LookingFor lookingFor, String university, String department, Integer yearOfStudy,
                               String bio, List<String> interests, List<String> photos, String whatsappNumber,
                               boolean verifiedStudent, boolean profileComplete, boolean visible) {}

    public record VisibilityRequest(boolean visible) {}

    // ---- connections ----
    public record SendRequestBody(@Size(max = 200, message = "Message can be at most 200 characters") String message) {}

    public record ConnectionDto(Long id, String status, String direction, String message,
                                Instant createdAt, Instant respondedAt, ProfileDto other) {}

    public record SummaryDto(long receivedPending, long sentPending, long connections, long unreadNotifications) {}

    // ---- notifications / safety ----
    public record NotificationDto(Long id, String type, String title, String body, String link,
                                  boolean read, Instant createdAt) {}

    public record ReportRequest(@NotNull Long userId, @NotBlank(message = "Please pick a reason") String reason,
                                @Size(max = 1000) String details) {}

    public record PageDto<T>(List<T> content, int page, int totalPages, long totalElements) {}
}
