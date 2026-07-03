package com.trust.service;

import com.trust.domain.Notification;
import com.trust.repository.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notify(Long organizationId, String title, String message, Notification.Severity severity) {
        Notification n = new Notification();
        n.setOrganizationId(organizationId);
        n.setTitle(title);
        n.setMessage(message);
        n.setSeverity(severity);
        notificationRepository.save(n);
    }
}
