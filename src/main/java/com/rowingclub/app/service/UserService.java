package com.rowingclub.app.service;

import com.rowingclub.app.common.exception.ResourceNotFoundException;
import com.rowingclub.app.dto.ResetPasswordRequest;
import com.rowingclub.app.dto.UpdateProfileRequest;
import com.rowingclub.app.dto.UpdateTrainerBranchesRequest;
import com.rowingclub.app.dto.UpdateTrainerPermissionsRequest;
import com.rowingclub.app.dto.UserResponse;
import com.rowingclub.app.entity.MembershipType;
import com.rowingclub.app.entity.TrainerBranch;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.repository.MembershipTypeRepository;
import com.rowingclub.app.repository.TrainerBranchRepository;
import com.rowingclub.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TrainerBranchRepository trainerBranchRepository;
    private final MembershipTypeRepository membershipTypeRepository;

    public UserResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public void updateFcmToken(UUID userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setFcmToken(token);
        userRepository.save(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getUsersByType(String userTypeName) {
        return userRepository.findAllByUserTypeName(userTypeName)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse toggleUserActive(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public void resetPassword(UUID userId, ResetPasswordRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /** Antrenör yetkilerini günceller. Yoklama açıkken liste görme de zorunlu açıktır. */
    @Transactional
    public UserResponse updateTrainerPermissions(
            UUID userId, UpdateTrainerPermissionsRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        boolean attendance = Boolean.TRUE.equals(request.getCanManageAttendance());
        // Yoklama alabilen, listeyi de görebilmeli (tutarlılık).
        boolean roster = attendance || Boolean.TRUE.equals(request.getCanViewRoster());

        user.setCanViewRoster(roster);
        user.setCanManageAttendance(attendance);
        user.setCanViewAthletes(Boolean.TRUE.equals(request.getCanViewAthletes()));
        user.setCanManageDailyBookings(Boolean.TRUE.equals(request.getCanManageDailyBookings()));
        userRepository.save(user);
        return toResponse(user);
    }
    /** Antrenörün görebileceği branşları günceller (boş liste = kısıtlama yok, hepsini görür). */
    @Transactional
    public UserResponse updateTrainerBranches(UUID userId, UpdateTrainerBranchesRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        trainerBranchRepository.deleteByTrainerId(userId);

        List<UUID> ids = request.getMembershipTypeIds() == null
                ? List.of() : request.getMembershipTypeIds();

        for (UUID membershipTypeId : ids) {
            MembershipType type = membershipTypeRepository.findById(membershipTypeId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "MembershipType", "id", membershipTypeId));
            trainerBranchRepository.save(TrainerBranch.builder()
                    .trainer(user)
                    .membershipType(type)
                    .build());
        }

        return toResponse(user);
    }

    // --- Mapper ---

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .userType(user.getUserType().getName())
                .isActive(user.getIsActive())
                .canViewRoster(user.getCanViewRoster())
                .canManageAttendance(user.getCanManageAttendance())
                .canViewAthletes(user.getCanViewAthletes())
                .canManageDailyBookings(user.getCanManageDailyBookings())
                .assignedBranchIds(
                        trainerBranchRepository.findMembershipTypeIdsByTrainerId(user.getId()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}