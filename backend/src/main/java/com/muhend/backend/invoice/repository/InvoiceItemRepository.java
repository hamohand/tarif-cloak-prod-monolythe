package com.muhend.backend.invoice.repository;

import com.muhend.backend.invoice.model.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
    
    /**
     * Récupère tous les éléments d'une facture.
     */
    List<InvoiceItem> findByInvoiceIdOrderById(Long invoiceId);
    
    /**
     * Supprime tous les éléments d'une facture.
     */
    @Modifying
    @Query("DELETE FROM InvoiceItem i WHERE i.invoice.id = :invoiceId")
    int deleteByInvoiceId(@Param("invoiceId") Long invoiceId);
}

