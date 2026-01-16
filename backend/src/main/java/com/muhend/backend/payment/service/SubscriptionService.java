package com.muhend.backend.payment.service;

import com.muhend.backend.payment.dto.SubscriptionDto;
import com.muhend.backend.payment.model.Subscription;
import com.muhend.backend.payment.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour gérer les abonnements.
 */
@Service
@Slf4j
public class SubscriptionService {
    
    private final SubscriptionRepository subscriptionRepository;
    
    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }
    
    /**
     * Récupère tous les abonnements d'une organisation.
     */
    @Transactional(readOnly = true)
    public List<SubscriptionDto> getSubscriptionsByOrganization(Long organizationId) {
        List<Subscription> subscriptions = subscriptionRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
        return subscriptions.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère l'abonnement actif d'une organisation.
     */
    @Transactional(readOnly = true)
    public SubscriptionDto getActiveSubscription(Long organizationId) {
        if (organizationId == null) {
            return null;
        }
        return subscriptionRepository.findByOrganizationIdAndStatus(organizationId, Subscription.SubscriptionStatus.ACTIVE)
                .map(this::toDto)
                .orElse(null);
    }
    
    /**
     * Récupère un abonnement par son ID.
     */
    @Transactional(readOnly = true)
    public SubscriptionDto getSubscriptionById(Long id) {
        return subscriptionRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Abonnement introuvable: " + id));
    }
    
    /**
     * Récupère un abonnement par son ID chez le processeur de paiement.
     */
    @Transactional(readOnly = true)
    public SubscriptionDto getSubscriptionByProviderId(String paymentProviderSubscriptionId) {
        return subscriptionRepository.findByPaymentProviderSubscriptionId(paymentProviderSubscriptionId)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Abonnement introuvable avec l'ID processeur: " + paymentProviderSubscriptionId));
    }
    
    /**
     * Vérifie si une organisation a un abonnement actif.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(Long organizationId) {
        return subscriptionRepository.existsByOrganizationIdAndStatus(organizationId, Subscription.SubscriptionStatus.ACTIVE);
    }
    
    /**
     * Récupère tous les abonnements (admin uniquement).
     */
    @Transactional(readOnly = true)
    public List<SubscriptionDto> getAllSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        return subscriptions.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Convertit un Subscription en DTO.
     */
    private SubscriptionDto toDto(Subscription subscription) {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setId(subscription.getId());
        dto.setOrganizationId(subscription.getOrganizationId());
        dto.setPricingPlanId(subscription.getPricingPlanId());
        dto.setStatus(subscription.getStatus());
        dto.setPaymentProvider(subscription.getPaymentProvider());
        dto.setPaymentProviderSubscriptionId(subscription.getPaymentProviderSubscriptionId());
        dto.setPaymentProviderCustomerId(subscription.getPaymentProviderCustomerId());
        dto.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
        dto.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        dto.setTrialStart(subscription.getTrialStart());
        dto.setTrialEnd(subscription.getTrialEnd());
        dto.setCanceledAt(subscription.getCanceledAt());
        dto.setCancelAtPeriodEnd(subscription.getCancelAtPeriodEnd());
        dto.setCreatedAt(subscription.getCreatedAt());
        dto.setUpdatedAt(subscription.getUpdatedAt());
        return dto;
    }
}

