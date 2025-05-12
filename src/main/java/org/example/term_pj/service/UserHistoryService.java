package org.example.term_pj.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.term_pj.dto.request.FileSaveRequest;
import org.example.term_pj.dto.request.UsageHistoryRequest;
import org.example.term_pj.dto.response.UsageHistoryResponse;
import org.example.term_pj.model.FilePath;
import org.example.term_pj.model.UsageHistory;
import org.example.term_pj.model.User;
import org.example.term_pj.repository.FilePathRepository;
import org.example.term_pj.repository.UsageHistoryRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserHistoryService {
    private final UsageHistoryRepository usageHistoryRepository;
    private final UserService userService;
    private final FilePathRepository filePathRepository;

    public UserHistoryService(UsageHistoryRepository usageHistoryRepository, UserService userService, FilePathRepository filePathRepository){
        this.usageHistoryRepository = usageHistoryRepository;
        this.userService = userService;
        this.filePathRepository = filePathRepository;
    }

    public void saveUsageatText(UsageHistoryRequest usageHistoryRequest) {
        System.out.println(".getId(): " + usageHistoryRequest.getUserId());
        User user = userService.getUserById(usageHistoryRequest.getUserId());
        UsageHistory history = new UsageHistory();
        history.setUser(user);
        history.setModelType(usageHistoryRequest.getModelType());
        history.setUsageTime(LocalDateTime.now());
        history.setModelInput(usageHistoryRequest.getInputFile());
        history.setModelOutput(usageHistoryRequest.getResultSummary());
        usageHistoryRepository.save(history);
    }

    @Transactional
    public void saveUsageatImage(UsageHistoryRequest usageRequest, FileSaveRequest fileRequest, MultipartFile file) {
        User user = userService.getUserById(usageRequest.getUserId());
        UsageHistory history = createHistory(usageRequest, user);
        saveFile(fileRequest, history, file);
    }

    private UsageHistory createHistory(UsageHistoryRequest req, User user) {
        UsageHistory history = new UsageHistory();
        history.setUser(user);
        history.setModelType(req.getModelType());
        history.setUsageTime(LocalDateTime.now());
        ObjectMapper objectMapper = new ObjectMapper();
        String resultJson;
        try {
            resultJson = objectMapper.writeValueAsString(req.getResultSummary());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
        history.setModelOutput(resultJson);

        return usageHistoryRepository.save(history);
    }
    private void saveFile(FileSaveRequest req, UsageHistory history,MultipartFile file) {

        FilePath filePath = new FilePath(history,req.getExtension(),req.getFilename(),req.getFilepath(),req.getUuid());
        filePathRepository.save(filePath);
    }


    public List<UsageHistoryResponse> getUsageHistoryByUserId(Long userId) {
        List<UsageHistory> historyList = usageHistoryRepository
                .findByUserIdOrderByUsageTimeDesc(userId, Pageable.unpaged())
                .getContent();

        return historyList.stream()
                .map(history -> new UsageHistoryResponse(
                        history.getId(),
                        history.getUser().getId(),
                        history.getUser().getUsername(),
                        history.getModelType(), // String
                        history.getUsageTime(),
                        history.getModelInput(),        // IMAGE 타입일 경우 null이고, FilePath 라는 다른 데이터베이스 안에 이미지가 저장되어있음.
                        history.getModelOutput()
                ))
                .collect(Collectors.toList());
    }

    public UsageHistoryResponse getImageMetaWithResult(Long historyId, Long userId) {
        UsageHistory history = usageHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!history.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // 이미지 경로 조회
        String imagePath = filePathRepository.findByUsageHistoryId(historyId)
                .map(FilePath::getFilepath)
                .orElse(null); // 실제 경로는 프론트에 안 넘기게

        // 이미지 자체는 다른 API에서 서빙
        return new UsageHistoryResponse(
                history.getId(),
                history.getUser().getId(),
                history.getUser().getUsername(),
                history.getModelType(),
                history.getUsageTime(),
                null, // inputFile은 이미지니까 프론트에 경로 넘기지 않음
                history.getModelOutput() // 이게 resultSummary
        );
    }
    public Resource getImageResourceByHistoryId(Long historyId, Long userId) {
        UsageHistory history = usageHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!history.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        FilePath filePath = filePathRepository.findByUsageHistoryId(historyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        try {
            Path path = Paths.get(filePath.getFilepath());
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "이미지를 읽을 수 없습니다.");
            }
            return resource;

        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "잘못된 파일 경로입니다.");
        }
    }

}

