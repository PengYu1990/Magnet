package com.pengyu.magnet.service.impl;

import com.pengyu.magnet.domain.Job;
import com.pengyu.magnet.domain.User;
import com.pengyu.magnet.domain.assessment.Question;
import com.pengyu.magnet.domain.assessment.TestPaper;
import com.pengyu.magnet.dto.TestPaperDTO;
import com.pengyu.magnet.exception.ResourceNotFoundException;
import com.pengyu.magnet.mapper.TestPaperMapper;
import com.pengyu.magnet.repository.JobRepository;
import com.pengyu.magnet.repository.UserRepository;
import com.pengyu.magnet.repository.assessment.TestPaperRepository;
import com.pengyu.magnet.service.TestPaperService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Test Paper Service, operate TestPaper object which will be generated by AI
 */
@Service
@AllArgsConstructor
public class TestPaperServiceImpl implements TestPaperService {

    private final TestPaperRepository testPaperRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    /**
     * Create
     * @param testPaperDTO
     * @return
     */
    @Override
    public TestPaperDTO save(TestPaperDTO testPaperDTO) {
        // Map testPaperDTO tp testPaper
        TestPaper testPaper = TestPaperMapper.INSTANCE.mapTestPaperDTOToTestPaper(testPaperDTO);

        // Find Job
        Job job = jobRepository
                .findById(testPaperDTO.getJobId())
                .orElseThrow(()->new ResourceNotFoundException("No job find with id " + testPaperDTO.getJobId()));
        testPaper.setJob(job);

        // Find User
        testPaper.setUser(job.getCompany().getUser());

        // Set Create Time
        testPaper.setCreatedAt(LocalDateTime.now());

        // Bind TestPaper for every Question
        for(Question question : testPaper.getQuestionList()) {
            question.setTestPaper(testPaper);
            // Bind Question for every option
            if(question.getOptionList() != null)
                question.getOptionList().forEach(optionAnswer -> optionAnswer.setQuestion(question));
        }

        TestPaperDTO testPaperDTONew = TestPaperMapper.INSTANCE.mapTestPaperToTestPaperDTO(testPaperRepository.save(testPaper));
        testPaperDTONew.setJobId(job.getId());

        return testPaperDTONew;
    }

    /**
     * Find one
     * @param id
     * @return
     */
    @Override
    public TestPaperDTO find(Long id) {
        return TestPaperMapper.INSTANCE.mapTestPaperToTestPaperDTO(testPaperRepository
                .findById(id)
                .orElseThrow(()->new ResourceNotFoundException("No such test paper with id "+id)));
    }

    /**
     * Find All owned by current user
     * @param pageable
     * @return
     */
    @Override
    public List<TestPaperDTO> findAllByCurrentUser(Pageable pageable) {
        // Get Current login user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        return testPaperRepository
                .findAllByUser(pageable, user)
                .map(testPaper -> TestPaperMapper.INSTANCE.mapTestPaperToTestPaperDTO(testPaper))
                .toList();
    }
}
