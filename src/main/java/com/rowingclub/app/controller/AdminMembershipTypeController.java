package com.rowingclub.app.controller;

import com.rowingclub.app.common.ApiResponse;
import com.rowingclub.app.dto.CreateMembershipTypeRequest;
import com.rowingclub.app.dto.MembershipTypeResponse;
import com.rowingclub.app.dto.UpdateMembershipTypeRequest;
import com.rowingclub.app.service.MembershipTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/membership-types")
@RequiredArgsConstructor
public class AdminMembershipTypeController {

    private final MembershipTypeService membershipTypeService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<MembershipTypeResponse>> create(
            @Valid @RequestBody CreateMembershipTypeRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Üyelik tipi oluşturuldu", membershipTypeService.create(request)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ANTRENÖR')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MembershipTypeResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(membershipTypeService.getAll()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MembershipTypeResponse>> update(
            @PathVariable UUID id,
            @RequestBody UpdateMembershipTypeRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Üyelik tipi güncellendi", membershipTypeService.update(id, request))
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        membershipTypeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Üyelik tipi silindi", null));
    }
}