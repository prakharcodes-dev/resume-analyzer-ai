package com.career.resumeanalyzer.repository;

import com.career.resumeanalyzer.model.UploadedResume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadedResumeRepository extends JpaRepository<UploadedResume, Long> {
    List<UploadedResume> findByUserIdOrderByUploadDateDesc(Long userId);
}
