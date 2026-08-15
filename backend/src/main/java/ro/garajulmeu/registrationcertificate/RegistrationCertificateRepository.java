package ro.garajulmeu.registrationcertificate;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationCertificateRepository extends JpaRepository<RegistrationCertificate, UUID> {
}