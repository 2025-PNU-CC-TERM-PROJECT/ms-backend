package org.example.term_pj.service;

import java.util.HashMap;
import java.util.Map;

import org.example.term_pj.exception.ResourceNotFoundException;
import org.example.term_pj.model.User;
import org.example.term_pj.repository.UsageHistoryRepository;
import org.example.term_pj.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UsageHistoryRepository usageHistoryRepository;    

    public UserService(UserRepository userRepository, UsageHistoryRepository usageHistoryRepository) {
        this.usageHistoryRepository = usageHistoryRepository;
        this.userRepository = userRepository;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }


    public Map<String, Object> getUsageStats(Long id) {
        int totalCount = usageHistoryRepository.countByUserId(id);
        int imageCount = usageHistoryRepository.countByUserIdAndModelType(id, "IMAGE");
        int textCount = usageHistoryRepository.countByUserIdAndModelType(id, "summary");
        Map<String, Object> result = new HashMap<>();
        result.put("usageCount", totalCount);
        result.put("imageCount", imageCount);
        result.put("textCount", textCount);

        return result;
    }
}