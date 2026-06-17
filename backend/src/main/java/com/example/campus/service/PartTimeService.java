package com.example.campus.service;

import com.example.campus.dto.request.PartTimeRequest;
import com.example.campus.dto.response.PartTimeApplyResponse;
import com.example.campus.dto.response.PartTimeResponse;
import com.example.campus.dto.response.UserResponse;
import com.example.campus.entity.PartTime;
import com.example.campus.entity.PartTimeApply;
import com.example.campus.entity.User;
import com.example.campus.exception.BusinessException;
import com.example.campus.exception.ResourceNotFoundException;
import com.example.campus.repository.PartTimeApplyRepository;
import com.example.campus.repository.PartTimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;

@Service
public class PartTimeService {

    private final PartTimeRepository partTimeRepository;
    private final PartTimeApplyRepository partTimeApplyRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ImageJsonService imageJsonService;

    public PartTimeService(
            PartTimeRepository partTimeRepository,
            PartTimeApplyRepository partTimeApplyRepository,
            UserService userService,
            NotificationService notificationService,
            ImageJsonService imageJsonService) {
        this.partTimeRepository = partTimeRepository;
        this.partTimeApplyRepository = partTimeApplyRepository;
        this.userService = userService;
        this.notificationService = notificationService;
        this.imageJsonService = imageJsonService;
    }

    @Transactional
    public PartTimeResponse createPartTime(Long userId, PartTimeRequest request) {
        User user = userService.getCurrentUser(userId);

        if (!"merchant".equals(user.getRole())) {
            throw new BusinessException("只有商家可以发布兼职信息");
        }
        validatePartTimeRequest(request);

        PartTime partTime = new PartTime();
        partTime.setMerchant(user);
        partTime.setTitle(request.getTitle().trim());
        partTime.setDescription(request.getDescription().trim());
        partTime.setSalaryMin(request.getSalaryMin());
        partTime.setSalaryMax(request.getSalaryMax());
        partTime.setRecruitCount(request.getRecruitCount());
        partTime.setWorkTime(request.getWorkTime().trim());
        partTime.setRequirements(normalizeBlank(request.getRequirements()));
        partTime.setLocation(normalizeBlank(request.getLocation()));
        partTime.setImages(imageJsonService.stringify(request.getImages()));
        partTime.setStatus("pending");

        PartTime savedPartTime = partTimeRepository.save(partTime);
        return convertToResponse(savedPartTime);
    }

    public List<PartTimeResponse> getAllPartTimes() {
        return partTimeRepository.findByStatusNot("closed").stream()
                .map(this::closeIfRecruitmentFull)
                .filter(partTime -> !"closed".equals(partTime.getStatus()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public PartTimeResponse getPartTimeById(@NonNull Long id) {
        PartTime partTime = partTimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("兼职信息不存在"));
        closeIfRecruitmentFull(partTime);
        return convertToResponse(partTime);
    }

    public List<PartTimeResponse> getPartTimesByMerchant(Long merchantId) {
        return partTimeRepository.findByMerchant_UserId(merchantId).stream()
                .map(this::closeIfRecruitmentFull)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<PartTimeApplyResponse> getReceivedApplications(Long merchantId) {
        return partTimeApplyRepository.findByPartTime_Merchant_UserId(merchantId).stream()
                .map(this::convertToApplyResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void applyPartTime(Long userId, Long partTimeId, String resume) {
        PartTime partTime = partTimeRepository.findById(partTimeId)
                .orElseThrow(() -> new ResourceNotFoundException("兼职信息不存在"));

        if (partTime.getMerchant().getUserId().equals(userId)) {
            throw new BusinessException("不能申请自己发布的兼职");
        }

        closeIfRecruitmentFull(partTime);
        if (!"active".equals(partTime.getStatus())) {
            throw new BusinessException("该兼职已关闭");
        }
        if (isRecruitmentFull(partTime)) {
            throw new BusinessException("招聘人数已满");
        }

        User user = userService.getCurrentUser(userId);

        PartTimeApply apply = new PartTimeApply();
        apply.setPartTime(partTime);
        apply.setUser(user);
        apply.setResume(resume);
        apply.setStatus("pending");

        partTimeApplyRepository.save(apply);
        notificationService.create(
                partTime.getMerchant().getUserId(),
                "收到新的兼职申请",
                user.getUsername() + " 申请了兼职：" + partTime.getTitle(),
                "part-time",
                "part-time",
                partTime.getPartTimeId());
    }

    @Transactional
    public void handleApply(Long userId, Long partTimeId, Long applyId, String status) {
        PartTime partTime = partTimeRepository.findById(partTimeId)
                .orElseThrow(() -> new ResourceNotFoundException("兼职信息不存在"));

        if (!partTime.getMerchant().getUserId().equals(userId)) {
            throw new BusinessException("无权操作此兼职");
        }

        PartTimeApply apply = partTimeApplyRepository.findById(applyId)
                .orElseThrow(() -> new ResourceNotFoundException("申请不存在"));

        if (!apply.getPartTime().getPartTimeId().equals(partTimeId)) {
            throw new BusinessException("申请不属于该兼职");
        }
        if (!"pending".equals(apply.getStatus())) {
            throw new BusinessException("申请已处理");
        }
        if (!"accepted".equals(status) && !"rejected".equals(status)) {
            throw new BusinessException("申请状态不合法");
        }

        if ("accepted".equals(status)) {
            if (isRecruitmentFull(partTime)) {
                throw new BusinessException("招聘人数已满");
            }
        }

        apply.setStatus(status);
        partTimeApplyRepository.save(apply);
        if ("accepted".equals(status)) {
            closeIfRecruitmentFull(partTime);
        }
        notificationService.create(
                apply.getUser().getUserId(),
                "兼职申请已处理",
                "你申请的兼职“" + partTime.getTitle() + "”已" + ("accepted".equals(status) ? "同意" : "拒绝"),
                "part-time",
                "part-time",
                partTime.getPartTimeId());
    }

    @Transactional
    public void closePartTime(Long userId, Long partTimeId) {
        PartTime partTime = partTimeRepository.findById(partTimeId)
                .orElseThrow(() -> new ResourceNotFoundException("兼职信息不存在"));

        if (!partTime.getMerchant().getUserId().equals(userId)) {
            throw new BusinessException("无权关闭此兼职");
        }
        if ("closed".equals(partTime.getStatus())) {
            throw new BusinessException("该兼职已关闭");
        }

        partTime.setStatus("closed");
        partTimeRepository.save(partTime);
        partTimeApplyRepository.findByPartTime_PartTimeId(partTimeId).forEach(apply ->
                notificationService.create(
                        apply.getUser().getUserId(),
                        "兼职已关闭",
                        "你申请的兼职“" + partTime.getTitle() + "”已关闭",
                        "part-time",
                        "part-time",
                        partTimeId));
    }

    private PartTimeResponse convertToResponse(PartTime partTime) {
        PartTimeResponse response = new PartTimeResponse();
        response.setPartTimeId(partTime.getPartTimeId());
        response.setMerchant(convertToUserResponse(partTime.getMerchant()));
        response.setTitle(partTime.getTitle());
        response.setDescription(partTime.getDescription());
        response.setSalaryMin(partTime.getSalaryMin());
        response.setSalaryMax(partTime.getSalaryMax());
        response.setRecruitCount(normalizeRecruitCount(partTime.getRecruitCount()));
        response.setAcceptedCount(getAcceptedCount(partTime));
        response.setWorkTime(partTime.getWorkTime());
        response.setRequirements(partTime.getRequirements());
        response.setLocation(partTime.getLocation());
        response.setImages(imageJsonService.parse(partTime.getImages()));
        response.setStatus(partTime.getStatus());
        response.setCreateTime(partTime.getCreateTime());
        return response;
    }

    private PartTime closeIfRecruitmentFull(PartTime partTime) {
        if ("active".equals(partTime.getStatus()) && isRecruitmentFull(partTime)) {
            partTime.setStatus("closed");
            return partTimeRepository.save(partTime);
        }
        return partTime;
    }

    private boolean isRecruitmentFull(PartTime partTime) {
        Integer quota = normalizeRecruitCount(partTime.getRecruitCount());
        long acceptedCount = getAcceptedCount(partTime);
        return acceptedCount >= quota;
    }

    private long getAcceptedCount(PartTime partTime) {
        return partTimeApplyRepository.countByPartTime_PartTimeIdAndStatus(partTime.getPartTimeId(), "accepted");
    }

    private PartTimeApplyResponse convertToApplyResponse(PartTimeApply apply) {
        PartTime partTime = apply.getPartTime();
        PartTimeApplyResponse response = new PartTimeApplyResponse();
        response.setApplyId(apply.getApplyId());
        response.setPartTimeId(partTime.getPartTimeId());
        response.setTitle(partTime.getTitle());
        response.setSalaryMin(partTime.getSalaryMin());
        response.setSalaryMax(partTime.getSalaryMax());
        response.setRecruitCount(normalizeRecruitCount(partTime.getRecruitCount()));
        response.setAcceptedCount(getAcceptedCount(partTime));
        response.setWorkTime(partTime.getWorkTime());
        response.setLocation(partTime.getLocation());
        response.setResume(apply.getResume());
        response.setApplicant(convertToUserResponse(apply.getUser()));
        response.setStatus(apply.getStatus());
        response.setCreateTime(apply.getCreateTime());
        return response;
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        return response;
    }

    private void validatePartTimeRequest(PartTimeRequest request) {
        BigDecimal salaryMin = request.getSalaryMin();
        BigDecimal salaryMax = request.getSalaryMax();

        if (salaryMin == null || salaryMin.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("最低薪资必须大于 0");
        }
        if (salaryMax == null || salaryMax.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("最高薪资必须大于 0");
        }
        if (salaryMax.compareTo(salaryMin) < 0) {
            throw new BusinessException("最高薪资不能小于最低薪资");
        }
        if (request.getRecruitCount() == null || request.getRecruitCount() <= 0) {
            throw new BusinessException("招聘人数必须为正整数");
        }
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Integer normalizeRecruitCount(Integer value) {
        return value == null || value <= 0 ? 1 : value;
    }
}
