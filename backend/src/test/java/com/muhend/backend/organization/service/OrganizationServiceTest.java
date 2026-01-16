package com.muhend.backend.organization.service;

import com.muhend.backend.organization.exception.QuotaExceededException;
import com.muhend.backend.organization.model.Organization;
import com.muhend.backend.organization.repository.OrganizationRepository;
import com.muhend.backend.organization.repository.OrganizationUserRepository;
import com.muhend.backend.usage.repository.UsageLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour OrganizationService - Phase 4 MVP : Quotas Basiques
 */
@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @Mock
    private UsageLogRepository usageLogRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Organization");
        organization.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testCheckQuota_WhenOrganizationIsNull_ShouldReturnTrue() {
        // Test: Si organizationId est null, on autorise
        assertTrue(organizationService.checkQuota(null));
        verify(organizationRepository, never()).findById(any());
        verify(usageLogRepository, never()).countByOrganizationIdAndTimestampBetween(any(), any(), any());
    }

    @Test
    void testCheckQuota_WhenOrganizationNotFound_ShouldReturnTrue() {
        // Test: Si l'organisation n'existe pas, on autorise (non bloquant)
        when(organizationRepository.findById(1L)).thenReturn(Optional.empty());

        assertTrue(organizationService.checkQuota(1L));
        verify(organizationRepository).findById(1L);
        verify(usageLogRepository, never()).countByOrganizationIdAndTimestampBetween(any(), any(), any());
    }

    @Test
    void testCheckQuota_WhenQuotaIsNull_ShouldReturnTrue() {
        // Test: Si monthlyQuota est null, le quota est illimité
        organization.setMonthlyQuota(null);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));

        assertTrue(organizationService.checkQuota(1L));
        verify(organizationRepository).findById(1L);
        verify(usageLogRepository, never()).countByOrganizationIdAndTimestampBetween(any(), any(), any());
    }

    @Test
    void testCheckQuota_WhenQuotaNotExceeded_ShouldReturnTrue() {
        // Test: Si le quota n'est pas dépassé, on autorise
        organization.setMonthlyQuota(100);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(usageLogRepository.countByOrganizationIdAndTimestampBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(50L); // 50 requêtes utilisées sur 100

        assertTrue(organizationService.checkQuota(1L));
        verify(organizationRepository).findById(1L);
        verify(usageLogRepository).countByOrganizationIdAndTimestampBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testCheckQuota_WhenQuotaExceeded_ShouldThrowQuotaExceededException() {
        // Test: Si le quota est dépassé, on lève une exception
        organization.setMonthlyQuota(100);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(usageLogRepository.countByOrganizationIdAndTimestampBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(100L); // 100 requêtes utilisées sur 100 (quota atteint)

        QuotaExceededException exception = assertThrows(QuotaExceededException.class, () -> {
            organizationService.checkQuota(1L);
        });

        assertTrue(exception.getMessage().contains("Quota mensuel dépassé"));
        assertTrue(exception.getMessage().contains("Test Organization"));
        assertTrue(exception.getMessage().contains("100/100"));
        verify(organizationRepository).findById(1L);
        verify(usageLogRepository).countByOrganizationIdAndTimestampBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testCheckQuota_WhenQuotaExceeded_MoreThanQuota_ShouldThrowQuotaExceededException() {
        // Test: Si le quota est dépassé (plus que le quota), on lève une exception
        organization.setMonthlyQuota(100);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(usageLogRepository.countByOrganizationIdAndTimestampBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(150L); // 150 requêtes utilisées sur 100 (quota dépassé)

        QuotaExceededException exception = assertThrows(QuotaExceededException.class, () -> {
            organizationService.checkQuota(1L);
        });

        assertTrue(exception.getMessage().contains("Quota mensuel dépassé"));
        assertTrue(exception.getMessage().contains("150/100"));
        verify(organizationRepository).findById(1L);
        verify(usageLogRepository).countByOrganizationIdAndTimestampBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testUpdateMonthlyQuota_WhenOrganizationExists_ShouldUpdateQuota() {
        // Test: Mise à jour du quota d'une organisation existante
        organization.setMonthlyQuota(50);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(organizationRepository.save(any(Organization.class))).thenReturn(organization);

        var result = organizationService.updateMonthlyQuota(1L, 200);

        assertNotNull(result);
        assertEquals(200, result.getMonthlyQuota());
        verify(organizationRepository).findById(1L);
        verify(organizationRepository).save(organization);
        assertEquals(200, organization.getMonthlyQuota());
    }

    @Test
    void testUpdateMonthlyQuota_WhenSettingQuotaToNull_ShouldSetUnlimitedQuota() {
        // Test: Mettre le quota à null pour quota illimité
        organization.setMonthlyQuota(100);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(organizationRepository.save(any(Organization.class))).thenReturn(organization);

        var result = organizationService.updateMonthlyQuota(1L, null);

        assertNotNull(result);
        assertNull(result.getMonthlyQuota());
        verify(organizationRepository).findById(1L);
        verify(organizationRepository).save(organization);
        assertNull(organization.getMonthlyQuota());
    }

    @Test
    void testUpdateMonthlyQuota_WhenOrganizationNotFound_ShouldThrowException() {
        // Test: Si l'organisation n'existe pas, on lève une exception
        when(organizationRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            organizationService.updateMonthlyQuota(1L, 100);
        });

        assertTrue(exception.getMessage().contains("Organisation non trouvée"));
        verify(organizationRepository).findById(1L);
        verify(organizationRepository, never()).save(any());
    }
}

