package com.rowingclub.app.repository;

import com.rowingclub.app.entity.TrainerBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TrainerBranchRepository extends JpaRepository<TrainerBranch, UUID> {

    List<TrainerBranch> findByTrainerId(UUID trainerId);

    void deleteByTrainerId(UUID trainerId);

    @Query("SELECT tb.membershipType.id FROM TrainerBranch tb WHERE tb.trainer.id = :trainerId")
    List<UUID> findMembershipTypeIdsByTrainerId(@Param("trainerId") UUID trainerId);
}