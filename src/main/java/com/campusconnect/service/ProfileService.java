package com.campusconnect.service;

import com.campusconnect.dto.Dtos.*;
import com.campusconnect.entity.ConnectionRequest;
import com.campusconnect.entity.Profile;
import com.campusconnect.entity.User;
import com.campusconnect.exception.ApiException;
import com.campusconnect.repository.BlockRepository;
import com.campusconnect.repository.ProfileRepository;
import com.campusconnect.repository.UserRepository;
import com.campusconnect.util.PhoneUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private static final int MAX_PHOTOS = 4, MAX_INTERESTS = 8;

    private final ProfileRepository profiles;
    private final UserRepository users;
    private final BlockRepository blocks;
    private final ProfileMapper mapper;
    private final FileStorageService files;

    @Transactional(readOnly = true)
    public MyProfileDto getMine(Long userId) {
        return mapper.toMine(user(userId), profile(userId));
    }

    @Transactional
    public MyProfileDto update(Long userId, ProfileUpdateRequest r) {
        User u = user(userId);
        Profile p = profile(userId);

        if (r.birthDate() != null) {
            if (r.birthDate().isAfter(LocalDate.now().minusYears(18))) throw ApiException.bad("You must be at least 18 to use Campus Connect");
            if (r.birthDate().isBefore(LocalDate.now().minusYears(70))) throw ApiException.bad("Please enter a valid date of birth");
        }

        if (r.whatsappNumber() != null && !r.whatsappNumber().isBlank()) {
            String phone = PhoneUtil.normalize(r.whatsappNumber());
            if (!phone.equals(u.getWhatsappNumber())) {
                users.findByWhatsappNumber(phone).ifPresent(o -> { throw ApiException.conflict("This WhatsApp number is already registered."); });
                u.setWhatsappNumber(phone);
            }
        }

        LinkedHashSet<String> interests = r.interests() == null ? new LinkedHashSet<>() :
                r.interests().stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toCollection(LinkedHashSet::new));
        if (interests.size() > MAX_INTERESTS) throw ApiException.bad("Pick at most " + MAX_INTERESTS + " interests");

        p.setFullName(r.fullName().trim());
        p.setBirthDate(r.birthDate());
        p.setGender(r.gender());
        p.setLookingFor(r.lookingFor());
        p.setUniversity(blankToNull(r.university()));
        p.setDepartment(blankToNull(r.department()));
        p.setYearOfStudy(r.yearOfStudy());
        p.setBio(blankToNull(r.bio()));
        p.getInterests().clear();
        p.getInterests().addAll(interests);
        return mapper.toMine(u, p);
    }

    /** Marks onboarding as finished once everything required is present. */
    @Transactional
    public MeDto complete(Long userId) {
        User u = user(userId);
        Profile p = profile(userId);
        List<String> missing = new ArrayList<>();
        if (u.getWhatsappNumber() == null) missing.add("WhatsApp number");
        if (p.getBirthDate() == null || p.getGender() == null || p.getUniversity() == null || p.getDepartment() == null
                || p.getYearOfStudy() == null) missing.add("basic details");
        if (p.getLookingFor() == null) missing.add("what you're looking for");
        if (p.getInterests().size() < 3) missing.add("at least 3 interests");
        if (p.getPhotos().isEmpty()) missing.add("at least one photo");
        if (!missing.isEmpty()) throw ApiException.bad("Almost there! Still missing: " + String.join(", ", missing));
        u.setProfileComplete(true);
        return mapper.toMe(u, p);
    }

    @Transactional
    public MyProfileDto addPhoto(Long userId, MultipartFile file) {
        Profile p = profile(userId);
        if (p.getPhotos().size() >= MAX_PHOTOS) throw ApiException.bad("You can upload up to " + MAX_PHOTOS + " photos");
        p.getPhotos().add(files.storeImage(file));
        return mapper.toMine(user(userId), p);
    }

    @Transactional
    public MyProfileDto removePhoto(Long userId, int index) {
        Profile p = profile(userId);
        if (index < 0 || index >= p.getPhotos().size()) throw ApiException.notFound("Photo not found");
        if (user(userId).isProfileComplete() && p.getPhotos().size() == 1)
            throw ApiException.bad("Keep at least one photo on your profile");
        files.deleteQuietly(p.getPhotos().remove(index));
        return mapper.toMine(user(userId), p);
    }

    @Transactional
    public MyProfileDto makePrimary(Long userId, int index) {
        Profile p = profile(userId);
        if (index < 0 || index >= p.getPhotos().size()) throw ApiException.notFound("Photo not found");
        String url = p.getPhotos().remove(index);
        p.getPhotos().add(0, url);
        return mapper.toMine(user(userId), p);
    }

    @Transactional
    public MeDto setVisibility(Long userId, boolean visible) {
        Profile p = profile(userId);
        p.setVisible(visible);
        return mapper.toMe(user(userId), p);
    }

    @Transactional
    public void deactivate(Long userId) {
        User u = user(userId);
        u.setActive(false);
        profile(userId).setVisible(false);
    }

    @Transactional(readOnly = true)
    public ProfileDto view(Long me, Long targetUserId) {
        Profile target = profiles.findByUserId(targetUserId)
                .filter(t -> t.getUser().isActive() && t.getUser().isProfileComplete())
                .orElseThrow(() -> ApiException.notFound("This profile isn't available"));
        boolean self = me.equals(targetUserId);
        if (!self && blocks.blockedEitherWay(me, targetUserId)) throw ApiException.notFound("This profile isn't available");
        ConnectionRequest rel = self ? null : mapper.relations(me).get(targetUserId);
        if (!self && !target.isVisible() && rel == null) throw ApiException.notFound("This profile isn't available");
        return mapper.toDto(target, profile(me), rel);
    }

    private static String blankToNull(String v) { return v == null || v.isBlank() ? null : v.trim(); }

    private User user(Long id) { return users.findById(id).orElseThrow(() -> ApiException.notFound("Account not found")); }
    private Profile profile(Long id) { return profiles.findByUserId(id).orElseThrow(() -> ApiException.notFound("Profile not found")); }
}
