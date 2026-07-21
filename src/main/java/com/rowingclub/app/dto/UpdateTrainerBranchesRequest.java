package com.rowingclub.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Bir antrenörün görebileceği branşları günceller. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTrainerBranchesRequest {
    private List<UUID> membershipTypeIds;
}