package com.rowingclub.app.service;

import com.rowingclub.app.common.MemberLevel;
import com.rowingclub.app.dto.BranchProgressResponse;
import com.rowingclub.app.entity.Membership;
import com.rowingclub.app.repository.EnrollmentRepository;
import com.rowingclub.app.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final MembershipRepository membershipRepository;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Üyenin tüm branşları için seviye listesi.
     * Branşlar üyenin üyeliklerinden (sahip olduğu paketlerin tiplerinden) gelir;
     * üyeliği olmayıp katılımı olan bir branş varsa o da eklenir.
     */
    @Transactional(readOnly = true)
    public List<BranchProgressResponse> getProgress(UUID userId) {
        // Üyenin sahip olduğu branşlar
        Map<UUID, String> branches = new LinkedHashMap<>();
        for (Object[] row : membershipRepository.findMemberBranches(userId)) {
            branches.put((UUID) row[0], (String) row[1]);
        }

        // Branş bazlı katılınan ders sayıları
        Map<UUID, Integer> attended = new HashMap<>();
        for (Object[] row : enrollmentRepository.countAttendedByBranch(userId)) {
            UUID typeId = (UUID) row[0];
            branches.putIfAbsent(typeId, (String) row[1]);
            attended.put(typeId, ((Long) row[2]).intValue());
        }

        List<BranchProgressResponse> result = new ArrayList<>();
        // Aktif eğitim paketleri: branş -> paketteki toplam ders (sessionsIncluded)
        Map<UUID, Integer> trainingTotals = activeTrainingTotalsByBranch(userId);
        for (Map.Entry<UUID, String> branch : branches.entrySet()) {
            UUID branchId = branch.getKey();
            Integer trainingTotal = trainingTotals.get(branchId);
            if (trainingTotal != null) {
                int done = (int) enrollmentRepository
                        .countAttendedTrainingByBranch(userId, branchId);
                result.add(buildTraining(
                        branchId, branch.getValue(), done, trainingTotal));
            } else {
                int count = attended.getOrDefault(branchId, 0);
                result.add(build(branchId, branch.getValue(), count));
            }
        }
        result.sort(Comparator.comparing(BranchProgressResponse::getMembershipTypeName));
        return result;
    }

    /** Tek bir branş için seviye (örn. seans detayında kayıtlı üyenin o branştaki seviyesi). */
    @Transactional(readOnly = true)
    public BranchProgressResponse getBranchProgress(UUID userId, UUID membershipTypeId, String branchName) {
        Integer trainingTotal = activeTrainingTotalsByBranch(userId).get(membershipTypeId);
        if (trainingTotal != null) {
            int done = (int) enrollmentRepository
                    .countAttendedTrainingByBranch(userId, membershipTypeId);
            return buildTraining(membershipTypeId, branchName, done, trainingTotal);
        }
        int count = (int) enrollmentRepository.countAttendedByUserAndBranch(userId, membershipTypeId);
        return build(membershipTypeId, branchName, count);
    }

    /**
     * Üyenin AKTİF eğitim paketleri için branş -> toplam ders haritası.
     * Eğitim paketi bitene (status ACTIVE olmaktan çıkana) kadar bu branşta
     * normal seviye yerine "Eğitim N" gösterilir.
     */
    private Map<UUID, Integer> activeTrainingTotalsByBranch(UUID userId) {
        Map<UUID, Integer> map = new HashMap<>();
        for (Membership m : membershipRepository
                .findAllByUserIdAndStatus(userId, Membership.MembershipStatus.ACTIVE)) {
            if (Boolean.TRUE.equals(m.getPlan().getIsTraining())) {
                Integer inc = m.getPlan().getSessionsIncluded();
                int total = inc != null ? inc : 0;
                for (var pt : m.getPlan().getPlanTypes()) {
                    map.put(pt.getMembershipType().getId(), total);
                }
            }
        }
        return map;
    }

    /** Aktif eğitim paketindeyken: "Eğitim N" (N = eğitimde katılınan ders). */
    private BranchProgressResponse buildTraining(UUID typeId, String name, int done, int total) {
        String label = done > 0 ? "Eğitim " + done : "Eğitim";
        return BranchProgressResponse.builder()
                .membershipTypeId(typeId)
                .membershipTypeName(name)
                .attendedCount(done)
                .levelNumber(done)
                .levelName(label)
                .maxLevel(total)
                .nextLevelName(null)
                .neededForNext(Math.max(0, total - done))
                .currentThreshold(0)
                .nextThreshold(total > 0 ? total : null)
                .isTraining(true)
                .build();
    }

    private BranchProgressResponse build(UUID typeId, String name, int attended) {
        MemberLevel level = MemberLevel.of(attended);
        MemberLevel next = level.next();
        return BranchProgressResponse.builder()
                .membershipTypeId(typeId)
                .membershipTypeName(name)
                .attendedCount(attended)
                .levelNumber(level.number())
                .levelName(level.label())
                .maxLevel(MemberLevel.maxNumber())
                .nextLevelName(next != null ? next.label() : null)
                .neededForNext(next != null ? Math.max(0, next.minSessions() - attended) : null)
                .currentThreshold(level.minSessions())
                .nextThreshold(next != null ? next.minSessions() : null)
                .isTraining(false)
                .build();
    }
}