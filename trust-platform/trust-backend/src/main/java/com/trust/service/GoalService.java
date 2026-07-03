package com.trust.service;

import com.trust.domain.Goal;
import com.trust.domain.Organization;
import com.trust.repository.GoalRepository;
import com.trust.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class GoalService {

    public static final int DEFAULT_PRIORITY = 3;

    private final GoalRepository goalRepository;
    private final OrganizationRepository organizationRepository;

    public GoalService(GoalRepository goalRepository, OrganizationRepository organizationRepository) {
        this.goalRepository = goalRepository;
        this.organizationRepository = organizationRepository;
    }

    /** يعيد أولويات كل الأهداف السبعة للمؤسسة - القيمة الافتراضية 3 (محايدة) لأي هدف لم يُخصَّص بعد */
    public Map<Goal.Type, Integer> resolveForOrganization(Long organizationId) {
        Map<Goal.Type, Integer> result = new EnumMap<>(Goal.Type.class);
        for (Goal.Type type : Goal.Type.values()) {
            result.put(type, DEFAULT_PRIORITY);
        }
        for (Goal g : goalRepository.findByOrganizationId(organizationId)) {
            result.put(g.getType(), g.getPriority());
        }
        return result;
    }

    public int priorityOf(Long organizationId, Goal.Type type) {
        return goalRepository.findByOrganizationIdAndType(organizationId, type)
                .map(Goal::getPriority)
                .orElse(DEFAULT_PRIORITY);
    }

    public Map<Goal.Type, Integer> updateAll(Long organizationId, Map<Goal.Type, Integer> priorities) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("المؤسسة غير موجودة"));

        for (Map.Entry<Goal.Type, Integer> entry : priorities.entrySet()) {
            int priority = entry.getValue();
            if (priority < 1 || priority > 5) {
                throw new IllegalArgumentException("أولوية الهدف يجب أن تكون بين 1 و5");
            }
            Goal goal = goalRepository.findByOrganizationIdAndType(organizationId, entry.getKey())
                    .orElseGet(() -> {
                        Goal g = new Goal();
                        g.setOrganization(org);
                        g.setType(entry.getKey());
                        return g;
                    });
            goal.setPriority(priority);
            goalRepository.save(goal);
        }
        return resolveForOrganization(organizationId);
    }

    public List<Goal> listRaw(Long organizationId) {
        return goalRepository.findByOrganizationId(organizationId);
    }
}
