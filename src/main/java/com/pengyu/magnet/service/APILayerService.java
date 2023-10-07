package com.pengyu.magnet.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface APILayerService {
    String parseResume(MultipartFile file) throws IOException;
    String fetchSkills(String skill);
}
