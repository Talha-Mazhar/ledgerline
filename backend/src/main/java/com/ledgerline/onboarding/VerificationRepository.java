package com.ledgerline.onboarding;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface VerificationRepository extends JpaRepository<Verification, UUID> {

    List<Verification> findByStatusOrderBySubmittedAt(VerificationStatus status);
}
