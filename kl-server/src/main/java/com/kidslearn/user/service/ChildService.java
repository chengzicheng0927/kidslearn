package com.kidslearn.user.service;

import com.kidslearn.common.exception.ResourceNotFoundException;
import com.kidslearn.user.domain.Child;
import com.kidslearn.user.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChildService {

    private final ChildRepository childRepository;

    @Transactional
    public void updateGrade(UUID childId, String grade) {
        Child child = childRepository.findById(childId)
            .orElseThrow(() -> new ResourceNotFoundException("Child", childId));
        child.setGrade(grade);
        childRepository.save(child);
    }
}
