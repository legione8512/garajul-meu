package ro.garajulmeu.vehiclepayment;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.reminder.ReminderService;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;
import ro.garajulmeu.vehicle.VehicleRepository;
import ro.garajulmeu.vehicledocument.DocumentCoverage;
import ro.garajulmeu.vehiclepayment.dto.PaymentDetails;
import ro.garajulmeu.vehiclepayment.dto.SavePaymentRequest;

/**
 * Recurring payments on a vehicle, and the reminders that follow them. Added in
 * 1.1; the rules are docs/DESIGN_1_1_INSTALMENTS.md.
 *
 * <p>Built on the documents' pattern on purpose: ownership matched in SQL, a
 * correction that replaces the whole schedule, and reminders cancelled and
 * regenerated in the same transaction as the write that changed them.
 */
@Service
public class VehiclePaymentService {

	private static final Logger log = LoggerFactory.getLogger(VehiclePaymentService.class);

	/** The leads a person can choose, largest first. */
	static final List<Integer> ALLOWED_LEADS = List.of(7, 3, 1, 0);

	/** The owner's default when nothing is chosen: three days before, and one. */
	static final List<Integer> DEFAULT_LEADS = List.of(3, 1);

	private final VehiclePaymentRepository paymentRepository;
	private final VehicleRepository vehicleRepository;
	private final UserRepository userRepository;
	private final ReminderService reminderService;
	private final Clock clock;

	VehiclePaymentService(VehiclePaymentRepository paymentRepository,
			VehicleRepository vehicleRepository, UserRepository userRepository,
			ReminderService reminderService, Clock clock) {
		this.paymentRepository = paymentRepository;
		this.vehicleRepository = vehicleRepository;
		this.userRepository = userRepository;
		this.reminderService = reminderService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<PaymentDetails> paymentsOf(UUID accountId, UUID vehicleId) {
		requireVehicle(accountId, vehicleId);
		LocalDate today = todayFor(accountId);

		return paymentRepository.ofVehicle(vehicleId, accountId).stream()
				.map(payment -> view(payment, today))
				.toList();
	}

	@Transactional
	public PaymentDetails add(UUID accountId, UUID vehicleId, SavePaymentRequest request) {
		requireVehicle(accountId, vehicleId);

		VehiclePayment payment = new VehiclePayment(vehicleId);
		apply(request, payment);
		paymentRepository.saveAndFlush(payment);
		reminderService.reconcilePayment(accountId, payment);

		log.info("Added {} payment {} to vehicle {}", payment.getKind(), payment.getId(), vehicleId);

		return view(payment, todayFor(accountId));
	}

	@Transactional
	public PaymentDetails correct(UUID accountId, UUID vehicleId, UUID paymentId,
			SavePaymentRequest request) {
		VehiclePayment payment = require(accountId, vehicleId, paymentId);

		apply(request, payment);
		paymentRepository.saveAndFlush(payment);
		reminderService.reconcilePayment(accountId, payment);

		return view(payment, todayFor(accountId));
	}

	/** Its reminders go with it: V13 cascades from the payment to them. */
	@Transactional
	public void delete(UUID accountId, UUID vehicleId, UUID paymentId) {
		VehiclePayment payment = require(accountId, vehicleId, paymentId);

		paymentRepository.delete(payment);
		paymentRepository.flush();
		log.info("Deleted payment {} of vehicle {}", paymentId, vehicleId);
	}

	/**
	 * Everything is checked before anything is written to the entity, so a
	 * refused correction leaves the stored payment exactly as it was - the
	 * lesson 1.0.2's vehicle-use field taught when its first version set the
	 * nickname and only then read the use.
	 */
	static void apply(SavePaymentRequest request, VehiclePayment payment) {
		PaymentKind kind = PaymentKind.of(request.kind())
				.orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR));
		PaymentFrequency frequency = PaymentFrequency.of(request.frequency())
				.orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR));
		LocalDate last = lastDueDateOf(request, frequency);
		List<Integer> leads = leadsOf(request.remindDaysBefore());

		payment.setKind(kind);
		payment.setSchedule(request.firstDueDate(), frequency, last);
		payment.setRemindDaysBefore(leads);
	}

	/**
	 * Exactly one end, and a series of one to {@link InstalmentSeries#MAX_INSTALMENTS}
	 * instalments. A last date that falls between two instalments is accepted and
	 * ends the series at the one before it, which is what a contract's final date
	 * means.
	 */
	private static LocalDate lastDueDateOf(SavePaymentRequest request, PaymentFrequency frequency) {
		boolean byDate = request.lastDueDate() != null;
		boolean byCount = request.instalmentCount() != null;

		if (byDate == byCount) {
			throw new ApiException(ErrorCode.PAYMENT_INVALID_SCHEDULE);
		}

		LocalDate first = request.firstDueDate();

		if (byCount) {
			int count = request.instalmentCount();
			if (count < 1 || count > InstalmentSeries.MAX_INSTALMENTS) {
				throw new ApiException(ErrorCode.PAYMENT_INVALID_SCHEDULE);
			}
			return InstalmentSeries.lastDueDateFor(first, frequency, count);
		}

		int count = InstalmentSeries.countUpTo(first, frequency, request.lastDueDate());
		if (count < 1 || count > InstalmentSeries.MAX_INSTALMENTS) {
			throw new ApiException(ErrorCode.PAYMENT_INVALID_SCHEDULE);
		}
		// Stored as the last instalment's own date, not the date typed, so the
		// row says where the series really ends.
		return InstalmentSeries.lastDueDateFor(first, frequency, count);
	}

	/**
	 * Nothing chosen means the owner's default. Anything outside 7, 3, 1 and 0 is
	 * refused rather than ignored, and duplicates collapse; the result is stored
	 * largest first, the order the reminders fire in.
	 */
	private static List<Integer> leadsOf(List<Integer> requested) {
		if (requested == null || requested.isEmpty()) {
			return DEFAULT_LEADS;
		}

		Set<Integer> unique = new LinkedHashSet<>();
		for (Integer lead : requested) {
			if (lead == null || !ALLOWED_LEADS.contains(lead)) {
				throw new ApiException(ErrorCode.VALIDATION_ERROR);
			}
			unique.add(lead);
		}

		return unique.stream().sorted(Comparator.reverseOrder()).toList();
	}

	static PaymentDetails view(VehiclePayment payment, LocalDate today) {
		InstalmentSeries series = payment.series();
		var next = series.nextOnOrAfter(today);

		return new PaymentDetails(
				payment.getId(),
				payment.getKind(),
				payment.getFirstDueDate(),
				payment.getFrequency(),
				payment.getLastDueDate(),
				series.count(),
				payment.getRemindDaysBefore(),
				next.map(InstalmentSeries.Instalment::dueDate).orElse(null),
				next.map(InstalmentSeries.Instalment::number).orElse(null),
				next.map(instalment -> ChronoUnit.DAYS.between(today, instalment.dueDate()))
						.orElse(null));
	}

	private void requireVehicle(UUID accountId, UUID vehicleId) {
		vehicleRepository.findByIdAndUserId(vehicleId, accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.VEHICLE_NOT_FOUND));
	}

	private VehiclePayment require(UUID accountId, UUID vehicleId, UUID paymentId) {
		return paymentRepository.byIdOfVehicle(paymentId, vehicleId, accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
	}

	private LocalDate todayFor(UUID accountId) {
		User user = userRepository.findById(accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

		return DocumentCoverage.todayFor(clock, ZoneId.of(user.getTimezone()));
	}
}
