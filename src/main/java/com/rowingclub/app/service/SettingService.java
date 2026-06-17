package com.rowingclub.app.service;

import com.rowingclub.app.common.exception.ResourceNotFoundException;
import com.rowingclub.app.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SettingService {

    private final SettingRepository settingRepository;

    public int getIntValue(String key) {
        var setting = settingRepository.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting", "key", key));
        return Integer.parseInt(setting.getValue());
    }

    public String getStringValue(String key) {
        var setting = settingRepository.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting", "key", key));
        return setting.getValue();
    }

    public void updateValue(String key, String newValue) {
        var setting = settingRepository.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting", "key", key));
        setting.setValue(newValue);
        settingRepository.save(setting);
    }
}