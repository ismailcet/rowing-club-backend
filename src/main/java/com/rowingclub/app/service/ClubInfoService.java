package com.rowingclub.app.service;

import com.rowingclub.app.dto.ClubInfoResponse;
import com.rowingclub.app.dto.ClubInfoUpdateRequest;
import com.rowingclub.app.entity.ClubInfo;
import com.rowingclub.app.repository.ClubInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubInfoService {

    private final ClubInfoRepository clubInfoRepository;

    /** Tekil club_info satırını döndürür (migration ile seed edilir). */
    public ClubInfoResponse get() {
        return toResponse(getEntity());
    }

    @Transactional
    public ClubInfoResponse update(ClubInfoUpdateRequest request) {
        ClubInfo info = getEntity();
        info.setName(request.name());
        info.setAbout(request.about());
        info.setPhone(request.phone());
        info.setEmail(request.email());
        info.setAddress(request.address());
        info.setLatitude(request.latitude());
        info.setLongitude(request.longitude());
        info.setInstagram(request.instagram());
        info.setWebsite(request.website());
        clubInfoRepository.save(info);
        return toResponse(info);
    }

    private ClubInfo getEntity() {
        // Tek satır; yoksa (migration atlanmışsa) yeni oluştur.
        return clubInfoRepository.findAll().stream().findFirst()
                .orElseGet(() -> clubInfoRepository.save(
                        ClubInfo.builder().name("Kulüp").build()));
    }

    private ClubInfoResponse toResponse(ClubInfo c) {
        return new ClubInfoResponse(
                c.getId(),
                c.getName(),
                c.getAbout(),
                c.getPhone(),
                c.getEmail(),
                c.getAddress(),
                c.getLatitude(),
                c.getLongitude(),
                c.getInstagram(),
                c.getWebsite()
        );
    }
}