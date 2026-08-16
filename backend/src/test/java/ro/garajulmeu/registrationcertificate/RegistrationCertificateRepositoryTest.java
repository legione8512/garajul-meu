package ro.garajulmeu.registrationcertificate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;
import ro.garajulmeu.vehicle.Vehicle;
import ro.garajulmeu.vehicle.VehicleRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Three annotations and no MockMvc, matching RefreshTokenServiceTest exactly, so
 * the cached context is reused and no further container starts.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class RegistrationCertificateRepositoryTest {

	@Autowired
	private RegistrationCertificateRepository certificateRepository;

	@Autowired
	private VehicleRepository vehicleRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	private record Owned(UUID userId, UUID vehicleId) {
	}

	private Owned givenVehicle(String email) {
		UUID userId = userRepository.saveAndFlush(
				new User("Marius Robert", email, "argon2-placeholder")).getId();
		UUID vehicleId = vehicleRepository.saveAndFlush(new Vehicle(userId)).getId();
		return new Owned(userId, vehicleId);
	}

	private RegistrationCertificate givenCertificate(Owned owned, String vin) {
		return certificateRepository.saveAndFlush(new RegistrationCertificate(
				owned.vehicleId(), owned.userId(), "B 100 ABC", "Dacia", "Logan", vin));
	}

	/**
	 * Every column, set and read back through a cleared persistence context.
	 *
	 * <p>Long on purpose. A field that was never mapped raises nothing anywhere:
	 * ddl-auto validate checks that mapped properties have columns, not that
	 * columns have properties, so an omission is silent in the build, silent in
	 * the tests and visible only as a value that will not save. Twenty-eight
	 * optional fields arrived in one edit, and this is what proves all of them
	 * landed.
	 */
	@Test
	void everyCertificateFieldSurvivesTheDatabase() {
		Owned owned = givenVehicle("round-trip@example.com");
		RegistrationCertificate certificate = givenCertificate(owned, "VF1AAAAAAAA000001");

		certificate.setFirstRegistrationDate(LocalDate.of(2019, 3, 14));
		certificate.setRegistrationDate(LocalDate.of(2021, 7, 1));
		certificate.setCertificateIssueDate(LocalDate.of(2021, 7, 5));
		certificate.setVehicleCategory("M1");
		certificate.setTypeVariantVersion("SD/JSD/AB1");
		certificate.setTypeApprovalNumber("e2*2007/46*0123*05");
		certificate.setValidityPeriod("nelimitat");
		certificate.setMaximumPermissibleMassKg(1780);
		certificate.setVehicleMassKg(1165);
		certificate.setEngineCapacityCc(999);
		certificate.setMaximumPowerKw(new BigDecimal("66.00"));
		certificate.setFuelType("benzina");
		certificate.setPowerWeightRatio(new BigDecimal("56.652"));
		certificate.setColour("albastru");
		certificate.setSeats(5);
		certificate.setStandingPlaces(0);
		certificate.setCivNumber("K123456");
		certificate.setIssuingAuthority("DRPCIV");
		certificate.setObservations("Fara observatii");
		certificate.setCertificateNumber("A00123456");
		certificate.setOwnerNameOrCompany("Robert");
		certificate.setOwnerFirstName("Marius");
		certificate.setOwnerAddress("Str. Exemplu 1, Bucuresti");
		certificate.setC2EqualsC1(Boolean.TRUE);
		certificate.setUserNameOrCompany("Robert");
		certificate.setUserFirstName("Marius");
		certificate.setUserAddress("Str. Exemplu 1, Bucuresti");
		certificate.setC3EqualsC1(Boolean.FALSE);

		certificateRepository.saveAndFlush(certificate);
		entityManager.clear();

		RegistrationCertificate reloaded = certificateRepository.findById(certificate.getId()).orElseThrow();

		assertThat(reloaded.getRegistrationNumber()).isEqualTo("B 100 ABC");
		assertThat(reloaded.getMake()).isEqualTo("Dacia");
		assertThat(reloaded.getCommercialDescription()).isEqualTo("Logan");
		assertThat(reloaded.getVin()).isEqualTo("VF1AAAAAAAA000001");

		assertThat(reloaded.getFirstRegistrationDate()).isEqualTo(LocalDate.of(2019, 3, 14));
		assertThat(reloaded.getRegistrationDate()).isEqualTo(LocalDate.of(2021, 7, 1));
		assertThat(reloaded.getCertificateIssueDate()).isEqualTo(LocalDate.of(2021, 7, 5));
		assertThat(reloaded.getVehicleCategory()).isEqualTo("M1");
		assertThat(reloaded.getTypeVariantVersion()).isEqualTo("SD/JSD/AB1");
		assertThat(reloaded.getTypeApprovalNumber()).isEqualTo("e2*2007/46*0123*05");
		assertThat(reloaded.getValidityPeriod()).isEqualTo("nelimitat");

		assertThat(reloaded.getMaximumPermissibleMassKg()).isEqualTo(1780);
		assertThat(reloaded.getVehicleMassKg()).isEqualTo(1165);
		assertThat(reloaded.getEngineCapacityCc()).isEqualTo(999);
		// Compared by value, not by scale: the column is DECIMAL(8,2) and the
		// driver reads back what the column stores, not what was written.
		assertThat(reloaded.getMaximumPowerKw()).isEqualByComparingTo("66");
		assertThat(reloaded.getFuelType()).isEqualTo("benzina");
		assertThat(reloaded.getPowerWeightRatio()).isEqualByComparingTo("56.652");
		assertThat(reloaded.getColour()).isEqualTo("albastru");
		assertThat(reloaded.getSeats()).isEqualTo(5);
		assertThat(reloaded.getStandingPlaces()).isZero();

		assertThat(reloaded.getCivNumber()).isEqualTo("K123456");
		assertThat(reloaded.getIssuingAuthority()).isEqualTo("DRPCIV");
		assertThat(reloaded.getObservations()).isEqualTo("Fara observatii");
		assertThat(reloaded.getCertificateNumber()).isEqualTo("A00123456");

		assertThat(reloaded.getOwnerNameOrCompany()).isEqualTo("Robert");
		assertThat(reloaded.getOwnerFirstName()).isEqualTo("Marius");
		assertThat(reloaded.getOwnerAddress()).isEqualTo("Str. Exemplu 1, Bucuresti");
		assertThat(reloaded.getC2EqualsC1()).isTrue();
		assertThat(reloaded.getUserNameOrCompany()).isEqualTo("Robert");
		assertThat(reloaded.getUserFirstName()).isEqualTo("Marius");
		assertThat(reloaded.getUserAddress()).isEqualTo("Str. Exemplu 1, Bucuresti");
		assertThat(reloaded.getC3EqualsC1()).isFalse();
	}

	/** Section 8: a certificate with only the four required fields is complete. */
	@Test
	void aCertificateWithOnlyTheRequiredFourIsValidAndLeavesTheRestEmpty() {
		Owned owned = givenVehicle("minimal@example.com");
		RegistrationCertificate certificate = givenCertificate(owned, "VF1AAAAAAAA000002");

		entityManager.clear();
		RegistrationCertificate reloaded = certificateRepository.findById(certificate.getId()).orElseThrow();

		assertThat(reloaded.getFirstRegistrationDate()).isNull();
		assertThat(reloaded.getColour()).isNull();
		assertThat(reloaded.getSeats()).isNull();
		assertThat(reloaded.getOwnerNameOrCompany()).isNull();
		assertThat(reloaded.getC2EqualsC1()).isNull();
		assertThat(reloaded.getObservations()).isNull();
	}

	/** Section 15, at the layer where it is enforced rather than checked. */
	@Test
	void aCertificateIsFoundOnlyThroughItsOwner() {
		Owned owner = givenVehicle("certificate-owner@example.com");
		Owned stranger = givenVehicle("certificate-stranger@example.com");
		givenCertificate(owner, "VF1AAAAAAAA000003");

		assertThat(certificateRepository.findByVehicleIdAndUserId(owner.vehicleId(), owner.userId()))
				.isPresent();
		assertThat(certificateRepository.findByVehicleIdAndUserId(owner.vehicleId(), stranger.userId()))
				.isEmpty();
	}

	/**
	 * The entity holds an owner's name and home address, which section 8 forbids
	 * logging. A default toString would print them the first time anyone
	 * interpolates the entity into a message.
	 */
	@Test
	void theEntityRefusesToPrintItself() {
		Owned owned = givenVehicle("quiet@example.com");
		RegistrationCertificate certificate = givenCertificate(owned, "VF1AAAAAAAA000004");
		certificate.setOwnerAddress("Str. Exemplu 1, Bucuresti");

		assertThat(certificate.toString())
				.doesNotContain("Exemplu")
				.doesNotContain("Dacia")
				.contains(certificate.getId().toString());
	}
}