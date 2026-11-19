package com.career.resumeanalyzer.repository;

import com.career.resumeanalyzer.model.UploadedResume;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UploadedResumeRepository extends JpaRepository<UploadedResume, Long> {
    List<UploadedResume> findByUserIdOrderByUploadDateDesc(Long userId);
    List<UploadedResume> findByUserId(Long userId);
    java.util.Optional<UploadedResume> findByIdAndUserId(Long id, Long userId);
}
