package com.pengyu.magnet.service.match;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengyu.magnet.domain.Job;
import com.pengyu.magnet.domain.Resume;
import com.pengyu.magnet.domain.match.JobRequirements;
import com.pengyu.magnet.domain.match.MatchingIndex;
import com.pengyu.magnet.domain.match.ResumeInsights;
import com.pengyu.magnet.dto.JobResponse;
import com.pengyu.magnet.dto.MatchingIndexDTO;
import com.pengyu.magnet.dto.ResumeDTO;
import com.pengyu.magnet.exception.ApiException;
import com.pengyu.magnet.exception.ResourceNotFoundException;
import com.pengyu.magnet.langchan4j.MatchAgent;
import com.pengyu.magnet.mapper.JobMapper;
import com.pengyu.magnet.mapper.MatchingIndexMapper;
import com.pengyu.magnet.repository.JobRepository;
import com.pengyu.magnet.repository.ResumeRepository;
import com.pengyu.magnet.repository.UserRepository;
import com.pengyu.magnet.repository.match.MatchingIndexRepository;
import com.pengyu.magnet.service.resume.ResumeServiceImpl;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.input.structured.StructuredPromptProcessor;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * OpenAI Job and Resume Match Service
 */
@Service
@RequiredArgsConstructor
public class OpenAIMatchServiceImpl implements AIMatchService {
    private final MatchAgent matchAgent;
    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;

    private final MatchingIndexRepository matchingIndexRepository;
    private final JobRequirementsService jobRequirementsService;
    private final ResumeInsightsService resumeInsightsService;
    private final UserRepository userRepository;



    // Prompt Template
    @StructuredPrompt({
            "Please extract software development technology skills such as programming language, libraries, concepts, software, methdologies needed for this job from the job description.  Every skill should be an independent technology.",
            "Job Description: {{jobDescription}}",
            "Structure your answer in the following way:",
            """
                     [
                     {"skill":"Java"},
                     {"skill","Scrum"},
                     {"skill","..."}
                     ]
                    """
    })

    /**
     * Prompt Template Params
     */
    @AllArgsConstructor
    static class SkillsExtractPrompt {
        private String jobDescription;

    }

    @StructuredPrompt({
            "Please extract the requirements of the position based on the job description. The items includes: degree, major, skills, experience, language. ",
            "degree: Bachelor's degree, Master's degree, Doctor's, Diploma or other",
            "major: for example, Computer Science",
            "skills: software development technology skills such as programming language, libraries, concepts, software, methodologies needed for this job from the job description. Every skill should be an independent technology. Assign a value to weight based on their importance in the job description and the weight should range from 1 to 10.",
            "experience: the requirement of years of work experience, for example 4+ years.",
            "language: for example, Fluent",
            "If there is no specified requirement for any item just leave it empty",
            "Job Description: {{jobDescription}}",
            "Structure your answer in the following way:",
            """
                    {
                      "degree":"...",
                      "major":"...",
                      skills:
                      [
                      {"skill":"Java"},
                      {"skill","Scrum"},
                      {"skill","..."}
                      ],
                      "experience":"...",
                      "language":"..."
                      }
            """
    })
    @AllArgsConstructor
    static class JobRequirementsExtractionPrompt {
        private String jobDescription;
    }

    @StructuredPrompt({
            "Please extract the insights of the resume based on the Resume. The items includes: degree, major, skills, experience, language. ",
            "degree: Bachelor's degree, Master's degree, Doctor's, Diploma or other",
            "major: for example, Computer Science",
            "skills: professional skills such as programming language, libraries, concepts, software, methodologies the job seeker has. Every skill should be an independent technology. If the skill is present in education, work experience and project experience, the weight of the skill will be increased. One point is added for each occurrence.",
            "experience: how long does the job seeker have in related work experience, for example 4+ years.",
            "language: for example, Fluent in English",
            "If there is no specified information for any item just leave it empty",
            "Resume: {{resume}}",
            "Structure your answer in the following way:",
            """
                    {
                      "degree":"...",
                      "major":"...",
                      skills:
                      [
                      {"skill":"Java", weight:"5"},
                      {"skill","Scrum", weight:"1"},
                      {"skill","...", weight:"..."}
                      ],
                      "experience":"...",
                      "language":"..."
                      }
            """
    })
    @AllArgsConstructor
    static class ResumeExtractionPrompt {
        private String resume;
    }

    @StructuredPrompt({
            "Please calculate the match between the Resume and the job based on the ResumeInsights and the JobRequirements, with the result as a decimal indicating the percentage of the Resume that meets the requirements of the job description.",
            "The calculation contains the following data:",
            "degree: degree match",
            "major: major match",
            "skill: skill match (as a percentage of matching skills)",
            "experience: Experience Matching Degree",
            "language: language match",
            "overall: overall match",
            "Each data is a decimal, ranging from 0 to 1, with two decimal places. If there is no specified requirement JobRequirements, the value should be 1. \n",
            "ResumeInsights: {{resumeInsights}}",
            "JobRequirements: {{jobRequirements}}",
            "Structure your answer in the following way:",
            """
               {
                 "degree": "...",
                 "major": "...",
                 "skill": "...",
                 "experience": "...",
                 "language": "...",
                 "overall": "..."
               }
            """
    })
    @AllArgsConstructor
    static class JobResumeMatchingPrompt {
        private String resumeInsights;
        private String jobRequirements;
    }

    public JobRequirements extractJobRequirements(Long jobId) {
        try {

            // Get Job info
//            Job job = jobRepository
//                    .findById(jobId)
//                    .orElseThrow(() -> new ResourceNotFoundException("No such job found with id " + jobId));

            // Get Job info
            JobResponse jobResponse = JobMapper.INSTANCE.mapJobToJobResponse(jobRepository.findById(jobId).orElse(null));
            // Build prompt template
            JobRequirementsExtractionPrompt jobRequirementsExtractionPrompt =
                    new JobRequirementsExtractionPrompt(objectMapper.writeValueAsString(jobResponse));

            // Render template
            Prompt prompt = StructuredPromptProcessor.toPrompt(jobRequirementsExtractionPrompt);

            // Call AI API to generate questions
            String json = matchAgent.chat(prompt.toUserMessage().text());

            // Parse return json
            JobRequirements jobRequirementsNew = objectMapper.readValue(json, JobRequirements.class);

            // Check if jobRequirements already exist
            JobRequirements jobRequirements = jobRequirementsService.findByJobId(jobId);
            if(jobRequirements != null) {
                jobRequirementsNew.setId(jobRequirements.getId());
            }

            // Save to Data
            JobRequirements saved = jobRequirementsService.save(jobRequirementsNew, jobId);

            return saved;

        } catch (Exception e) {
            throw new ApiException(e.getMessage());
        }

    }


    /**
     * Extract Resume Characteristics using AI
     * @param resumeId
     * @return
     * @throws JsonProcessingException
     */
    public ResumeInsights extractResumeInsights(Long resumeId) {
        try {


            // Get Resume info
            ResumeDTO resume = ResumeServiceImpl.mapResumeToResumeDTO(resumeRepository.findById(resumeId).orElse(null));

            // Build prompt template
            ResumeExtractionPrompt resumeExtractionPrompt =
                    new ResumeExtractionPrompt(objectMapper.writeValueAsString(resume));

            // Render template
            Prompt prompt = StructuredPromptProcessor.toPrompt(resumeExtractionPrompt);

            // Call AI API to extract Resume Characteristics
            String json = matchAgent.chat(prompt.toUserMessage().text());

            // Parse return json
            ResumeInsights resumeInsightsNew = objectMapper.readValue(json, ResumeInsights.class);

            // Check if ResumeInsights already exist
            ResumeInsights resumeInsights = resumeInsightsService.findByResumeId(resumeId);
            if(resumeInsights != null) {
                resumeInsightsNew.setId(resumeInsights.getId());
            }

            // Save to database
            ResumeInsights saved = resumeInsightsService.save(resumeInsights, resumeId);

            return saved;

        } catch (JsonProcessingException e) {
            throw new ApiException(e.getMessage());
        }

    }

    /**
     * Call AI to math job and resume
     * @param jobId
     * @param resumeId
     * @return
     */
    @Override
    public MatchingIndexDTO match(Long jobId, Long resumeId) {
        try {
            // Get Job info
            JobRequirements jobRequirements = jobRequirementsService.findByJobId(jobId);
            // Get Resume Info
            ResumeInsights resumeInsights = resumeInsightsService.findByResumeId(resumeId);

            // Build prompt template
            JobResumeMatchingPrompt resumeExtractionPrompt =
                    new JobResumeMatchingPrompt(objectMapper.writeValueAsString(jobRequirements), objectMapper.writeValueAsString(resumeInsights));

            // Render template
            Prompt prompt = StructuredPromptProcessor.toPrompt(resumeExtractionPrompt);

            // Call AI API to extract Resume Characteristics
            String json = matchAgent.chat(prompt.toUserMessage().text());

            // Parse return json
            MatchingIndex matchingIndex = objectMapper.readValue(json, MatchingIndex.class);

            // Bind MatchingIndex and Job, Resume
            Job job = jobRepository
                    .findById(jobId)
                    .orElseThrow(() -> new ResourceNotFoundException("No such job found with jobId " + jobId));
            matchingIndex.setJob(job);

            Resume resume = resumeRepository
                    .findById(resumeId)
                    .orElseThrow(() -> new ResourceNotFoundException("No such Resume found with resumeId " + resumeId));
            matchingIndex.setResume(resume);

            // Sava to Database
            MatchingIndex saved = matchingIndexRepository.save(matchingIndex);

            // return
            MatchingIndexDTO matchingIndexDTO = MatchingIndexMapper.INSTANCE.mapMatchingIndexToMatchingIndexDTO(saved);
            matchingIndexDTO.setResumeDTO(ResumeServiceImpl.mapResumeToResumeDTO(resume));
            matchingIndexDTO.setJobResponse(JobMapper.INSTANCE.mapJobToJobResponse(job));
            return matchingIndexDTO;
        } catch (JsonProcessingException e) {
            throw new ApiException(e.getMessage());
        }
    }

    @Override
    public MatchingIndexDTO find(Long jobId, Long resumeId) {
        MatchingIndex matchingIndex =
                matchingIndexRepository
                        .findByJobIdAndResumeId(jobId, resumeId)
                        .orElseThrow(() -> new ResourceNotFoundException("No such MatchingIndex found with jobId %s and resumeId %s".formatted(jobId, resumeId)));
                ;
        // return
        MatchingIndexDTO matchingIndexDTO = MatchingIndexMapper.INSTANCE.mapMatchingIndexToMatchingIndexDTO(matchingIndex);
        matchingIndexDTO.setResumeDTO(ResumeServiceImpl.mapResumeToResumeDTO(matchingIndex.getResume()));
        matchingIndexDTO.setJobResponse(JobMapper.INSTANCE.mapJobToJobResponse(matchingIndex.getJob()));
        return matchingIndexDTO;
    }

    /**
     * Call AI API, extract skills from job description
     *
     * @param jobId
     * @return
     */
    /*public List<ResumeDTO.SkillDTO> extractSkills(Long jobId) {
        // Get Job info
        Job job = jobRepository
                .findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("No such job found with id " + jobId));

        // Get Current login user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        // Check if current user owns this job
        if (!job.getCompany().getUser().getEmail().equals(user.getEmail())) {
            throw new InsufficientAuthenticationException("Sorry, you can not generate question for other user's job!");
        }

        // Build prompt template
        SkillsExtractPrompt createTestPrompt =
                new SkillsExtractPrompt(job.getDescription());

        // Render template
        Prompt prompt = StructuredPromptProcessor.toPrompt(createTestPrompt);

        // Call AI API to generate questions
        String json = matchAgent.chat(prompt.toUserMessage().text());


        try {
            // Parse return json
            List<ResumeDTO.SkillDTO> skillDTOS = Arrays.asList(objectMapper.readValue(json, ResumeDTO.SkillDTO[].class));

            return skillDTOS;

        } catch (Exception e) {
            throw new ApiException(e.getMessage());
        }

    }*/

}