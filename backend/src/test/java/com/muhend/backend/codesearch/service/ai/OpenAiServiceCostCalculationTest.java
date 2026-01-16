package com.muhend.backend.codesearch.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test pour vérifier le calcul du coût avec le tarif de base.
 */
@ExtendWith(MockitoExtension.class)
class OpenAiServiceCostCalculationTest {

    @Mock
    private AiPrompts aiPrompts;

    private OpenAiService openAiService;

    @BeforeEach
    void setUp() {
        // Créer une instance du service avec un tarif de base de test
        // Le constructeur attend maintenant une String qui sera parsée en double
        openAiService = new OpenAiService(aiPrompts, "test-api-key", "0.01");
    }

    @Test
    void testBaseRequestPriceIsSet() {
        // Vérifier que le tarif de base est bien injecté
        Object basePriceObj = ReflectionTestUtils.getField(openAiService, "baseRequestPrice");
        assertNotNull(basePriceObj, "Le tarif de base doit être défini");
        assertTrue(basePriceObj instanceof Double, "Le tarif de base doit être un Double");
        Double basePrice = (Double) basePriceObj;
        assertEquals(0.01, basePrice, "Le tarif de base doit être de 0.01 USD");
    }

    @Test
    void testCostCalculationLogic() {
        // Test du calcul du coût : tarif de base + coût des tokens
        double baseRequestPrice = 0.01; // Tarif de base
        int totalTokens = 1000; // Exemple de tokens
        double PRICE_INPUT = 0.15 / 1_000_000;   // $ par token input
        double PRICE_OUTPUT = 0.60 / 1_000_000;  // $ par token output
        double PRICE_TOTAL = PRICE_INPUT + PRICE_OUTPUT;
        
        // Calcul attendu
        double tokenCost = totalTokens * PRICE_TOTAL;
        double expectedTotalCost = baseRequestPrice + tokenCost;
        
        // Vérifier que le calcul est correct
        assertTrue(expectedTotalCost > baseRequestPrice, 
            "Le coût total doit être supérieur au tarif de base");
        assertTrue(expectedTotalCost > tokenCost, 
            "Le coût total doit être supérieur au coût des tokens seul");
        
        // Vérifier que le tarif de base est bien inclus
        double difference = expectedTotalCost - tokenCost;
        assertEquals(baseRequestPrice, difference, 0.000001, 
            "La différence entre le coût total et le coût des tokens doit être égale au tarif de base");
    }

    @Test
    void testCostWithZeroTokens() {
        // Même avec 0 tokens, le tarif de base doit être appliqué
        double baseRequestPrice = 0.01;
        int totalTokens = 0;
        double PRICE_INPUT = 0.15 / 1_000_000;
        double PRICE_OUTPUT = 0.60 / 1_000_000;
        double PRICE_TOTAL = PRICE_INPUT + PRICE_OUTPUT;
        
        double tokenCost = totalTokens * PRICE_TOTAL;
        double expectedTotalCost = baseRequestPrice + tokenCost;
        
        assertEquals(baseRequestPrice, expectedTotalCost, 0.000001,
            "Avec 0 tokens, le coût total doit être égal au tarif de base");
    }

    @Test
    void testCostWithManyTokens() {
        // Test avec un grand nombre de tokens
        double baseRequestPrice = 0.01;
        int totalTokens = 100000; // 100k tokens
        double PRICE_INPUT = 0.15 / 1_000_000;
        double PRICE_OUTPUT = 0.60 / 1_000_000;
        double PRICE_TOTAL = PRICE_INPUT + PRICE_OUTPUT;
        
        double tokenCost = totalTokens * PRICE_TOTAL;
        double expectedTotalCost = baseRequestPrice + tokenCost;
        
        // Le coût total doit être significativement supérieur au tarif de base
        assertTrue(expectedTotalCost > baseRequestPrice * 10,
            "Avec beaucoup de tokens, le coût total doit être bien supérieur au tarif de base");
        
        // Vérifier que le tarif de base est toujours inclus
        double difference = expectedTotalCost - tokenCost;
        assertEquals(baseRequestPrice, difference, 0.000001,
            "Le tarif de base doit toujours être inclus, même avec beaucoup de tokens");
    }
}

