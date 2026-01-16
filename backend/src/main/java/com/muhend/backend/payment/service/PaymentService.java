package com.muhend.backend.payment.service;

import com.muhend.backend.payment.dto.PaymentDto;
import com.muhend.backend.payment.model.Payment;
import com.muhend.backend.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour gérer les paiements.
 */
@Service
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }
    
    /**
     * Récupère tous les paiements d'une organisation.
     */
    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsByOrganization(Long organizationId) {
        List<Payment> payments = paymentRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
        return payments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère tous les paiements d'un abonnement.
     */
    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsBySubscription(Long subscriptionId) {
        List<Payment> payments = paymentRepository.findBySubscriptionIdOrderByCreatedAtDesc(subscriptionId);
        return payments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère tous les paiements d'une facture.
     */
    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsByInvoice(Long invoiceId) {
        if (invoiceId == null) {
            return List.of();
        }
        List<Payment> payments = paymentRepository.findByInvoiceIdOrderByCreatedAtDesc(invoiceId);
        return payments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère un paiement par son ID.
     */
    @Transactional(readOnly = true)
    public PaymentDto getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Paiement introuvable: " + id));
    }
    
    /**
     * Récupère un paiement par son ID chez le processeur de paiement.
     */
    @Transactional(readOnly = true)
    public PaymentDto getPaymentByProviderId(String paymentProviderPaymentId) {
        return paymentRepository.findByPaymentProviderPaymentId(paymentProviderPaymentId)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Paiement introuvable avec l'ID processeur: " + paymentProviderPaymentId));
    }
    
    /**
     * Récupère un paiement par son ID d'intention de paiement (Stripe).
     */
    @Transactional(readOnly = true)
    public PaymentDto getPaymentByPaymentIntentId(String paymentIntentId) {
        return paymentRepository.findByPaymentProviderPaymentIntentId(paymentIntentId)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Paiement introuvable avec l'ID d'intention: " + paymentIntentId));
    }
    
    /**
     * Récupère tous les paiements (admin uniquement).
     */
    @Transactional(readOnly = true)
    public List<PaymentDto> getAllPayments() {
        List<Payment> payments = paymentRepository.findAll();
        return payments.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Convertit un Payment en DTO.
     */
    private PaymentDto toDto(Payment payment) {
        PaymentDto dto = new PaymentDto();
        dto.setId(payment.getId());
        dto.setSubscriptionId(payment.getSubscriptionId());
        dto.setOrganizationId(payment.getOrganizationId());
        dto.setInvoiceId(payment.getInvoiceId());
        dto.setAmount(payment.getAmount());
        dto.setCurrency(payment.getCurrency());
        dto.setStatus(payment.getStatus());
        dto.setPaymentProvider(payment.getPaymentProvider());
        dto.setPaymentProviderPaymentId(payment.getPaymentProviderPaymentId());
        dto.setPaymentProviderPaymentIntentId(payment.getPaymentProviderPaymentIntentId());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setPaymentMethodType(payment.getPaymentMethodType());
        dto.setDescription(payment.getDescription());
        dto.setFailureReason(payment.getFailureReason());
        dto.setInvoiceUrl(payment.getInvoiceUrl());
        dto.setReceiptUrl(payment.getReceiptUrl());
        dto.setPaidAt(payment.getPaidAt());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());
        return dto;
    }
}

