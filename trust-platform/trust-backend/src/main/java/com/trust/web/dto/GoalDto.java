package com.trust.web.dto;

/**
 * هدف واحد. influencesEngine تكشف صراحةً ما إذا كانت الأولوية تُغيّر سلوك المحرك
 * فعلًا - خمسة من سبعة لا تفعل بعد، وإخفاء ذلك يجعل الواجهة تَعِد بما لا تملكه.
 */
public record GoalDto(
        String type,
        String labelAr,
        String pillar,
        String pillarLabelAr,
        int priority,
        boolean influencesEngine
) {}
