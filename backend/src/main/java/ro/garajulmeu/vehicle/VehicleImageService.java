package ro.garajulmeu.vehicle;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.common.Thumbnail;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.storage.FileStorageProvider;
import ro.garajulmeu.storage.VehicleImage;
import ro.garajulmeu.storage.VehicleImageValidator;
import ro.garajulmeu.vehicle.dto.VehicleImageContent;

/**
 * The photograph of one vehicle. Specification sections 16 and 22.
 *
 * <p><strong>A replacement writes a new key rather than overwriting the old
 * one.</strong> The address a client reads is the vehicle's, which never
 * changes, so overwriting would leave a browser showing the previous photograph
 * from its cache with nothing to tell it otherwise. A new key also makes the
 * removal of the old object a step somebody wrote, rather than something the
 * bucket did quietly.
 *
 * <p><strong>The two writes are ordered in opposite directions, and both
 * orderings are the safe one for their case.</strong> Replacing writes the
 * object, then the row, then removes the old object: every failure leaves either
 * an unreferenced object or the previous photograph still working. Deleting
 * writes the row first and removes the object after: if that removal fails, the
 * row already says there is no photograph, which is what was asked for. Both
 * choices prefer a leaked object over a wrong answer, because an orphan costs
 * storage and a wrong answer costs trust.
 *
 * <p><strong>Every photograph has a second object beside it since 1.0.2</strong>:
 * the thumbnail the cards draw. It is derived, never uploaded, and its key is the
 * photograph's with a suffix - so nothing has to be stored to find it, and every
 * place that removes a photograph removes it in the same breath. Making one is
 * always best-effort: a picture the owner can see beats a circle they cannot,
 * and no failure of ours may cost them an upload.
 *
 * <p><strong>This class also owns the cleanup that happens when the row is taken
 * away by somebody else</strong> - a deleted vehicle, or a deleted account whose
 * vehicles go with it. That work lives here rather than in the two services that
 * trigger it, because {@code image_object_key} is this class's business and
 * spreading storage knowledge across three packages is how one of them ends up
 * forgetting.
 */
@Service
public class VehicleImageService {

	private static final Logger log = LoggerFactory.getLogger(VehicleImageService.class);

	private final VehicleRepository vehicleRepository;
	private final VehicleImageValidator validator;
	private final FileStorageProvider storage;

	VehicleImageService(VehicleRepository vehicleRepository, VehicleImageValidator validator,
			FileStorageProvider storage) {
		this.vehicleRepository = vehicleRepository;
		this.validator = validator;
		this.storage = storage;
	}

	@Transactional
	public void replace(UUID accountId, UUID vehicleId, byte[] upload) {
		Vehicle vehicle = owned(accountId, vehicleId);
		VehicleImage image = validator.accept(upload);

		String previous = vehicle.getImageObjectKey();
		String objectKey = keyFor(vehicleId);

		storage.put(objectKey, image.bytes(), image.contentType());
		writeThumbnail(vehicleId, objectKey, image.bytes());

		vehicle.setImage(objectKey, image.contentType(), image.sizeBytes());
		vehicleRepository.saveAndFlush(vehicle);

		if (previous != null) {
			storage.delete(previous);
			storage.delete(thumbnailKeyOf(previous));
		}

		log.info("Stored an image for vehicle {} ({} bytes, {})", vehicleId, image.sizeBytes(),
				image.contentType());
	}

	/** Idempotent: a vehicle with no photograph is already in the asked-for state. */
	@Transactional
	public void remove(UUID accountId, UUID vehicleId) {
		Vehicle vehicle = owned(accountId, vehicleId);
		String objectKey = vehicle.getImageObjectKey();

		if (objectKey == null) {
			return;
		}

		vehicle.clearImage();
		vehicleRepository.saveAndFlush(vehicle);
		storage.delete(objectKey);
		storage.delete(thumbnailKeyOf(objectKey));

		log.info("Removed the image of vehicle {}", vehicleId);
	}

	/**
	 * <p>A row pointing at an object that is no longer in the bucket answers 404
	 * rather than 500. It should not happen and it is not a server fault when it
	 * does - somebody emptied the bucket - and the client's correct response is
	 * the same as for a vehicle that never had a photograph.
	 *
	 * <p>The row is deliberately <strong>not</strong> repaired here. This is a
	 * read, and a GET that quietly writes is a surprise waiting for whoever
	 * debugs it later; the log line is enough to notice the drift.
	 */
	@Transactional(readOnly = true)
	public VehicleImageContent read(UUID accountId, UUID vehicleId) {
		Vehicle vehicle = owned(accountId, vehicleId);
		String objectKey = vehicle.getImageObjectKey();

		if (objectKey == null) {
			throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
		}

		byte[] bytes = storage.get(objectKey).orElseThrow(() -> {
			log.warn("Vehicle {} points at an object the store does not have", vehicleId);
			return new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
		});

		return new VehicleImageContent(bytes, vehicle.getImageContentType());
	}

	/**
	 * The small square the cards draw, made on the way past when the store has not
	 * got one yet.
	 *
	 * <p><strong>This GET writes, and that is the point.</strong> The read above
	 * refuses to repair its row and says why; this is a different thing entirely -
	 * nothing is corrected, a derived object is filled in from bytes we already
	 * have. It is also what spares this feature a migration: every photograph
	 * uploaded before 1.0.2 has no thumbnail, and rather than a batch job walking
	 * a bucket, the first card that asks for one makes it, once. A failed write
	 * costs the next reader another decode and nothing else, which is why it is
	 * not allowed to fail the request.
	 *
	 * <p>A photograph no reader can decode answers 404 rather than 500: the card
	 * then draws the empty circle, which is the same thing it draws for a vehicle
	 * nobody has photographed, and a picture that cannot be shrunk is not a server
	 * fault.
	 */
	@Transactional(readOnly = true)
	public VehicleImageContent readThumbnail(UUID accountId, UUID vehicleId) {
		Vehicle vehicle = owned(accountId, vehicleId);
		String objectKey = vehicle.getImageObjectKey();

		if (objectKey == null) {
			throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
		}

		Optional<byte[]> stored = storage.get(thumbnailKeyOf(objectKey));

		if (stored.isPresent()) {
			return new VehicleImageContent(stored.get(), Thumbnail.CONTENT_TYPE);
		}

		byte[] original = storage.get(objectKey).orElseThrow(() -> {
			log.warn("Vehicle {} points at an object the store does not have", vehicleId);
			return new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
		});

		byte[] thumbnail = writeThumbnail(vehicleId, objectKey, original)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

		return new VehicleImageContent(thumbnail, Thumbnail.CONTENT_TYPE);
	}

	/**
	 * Makes the thumbnail and stores it, and lets neither step fail the caller.
	 *
	 * <p>The bytes are returned so that the reader above can answer with what it
	 * just made even when the store would not take it.
	 */
	private Optional<byte[]> writeThumbnail(UUID vehicleId, String objectKey, byte[] original) {
		Optional<byte[]> thumbnail = Thumbnail.of(original);

		if (thumbnail.isEmpty()) {
			log.warn("No thumbnail could be made of the image of vehicle {}", vehicleId);
			return thumbnail;
		}

		try {
			storage.put(thumbnailKeyOf(objectKey), thumbnail.get(), Thumbnail.CONTENT_TYPE);
		} catch (RuntimeException exception) {
			log.warn("Could not store the thumbnail of vehicle {}", vehicleId, exception);
		}

		return thumbnail;
	}

	/**
	 * Derived rather than stored, so that a row written before 1.0.2 names its
	 * thumbnail as surely as one written after it. Still UUID-derived, per section
	 * 22, because the photograph's key is.
	 */
	static String thumbnailKeyOf(String objectKey) {
		return objectKey + "-thumbnail";
	}

	/**
	 * Every photograph in one account's garage, to be read <strong>before</strong>
	 * anything deletes the rows that name them. Once the account row goes, the
	 * cascade takes the vehicles and their keys with it, and nothing left in the
	 * database knows where the objects are.
	 */
	@Transactional(readOnly = true)
	public List<String> imageKeysOf(UUID accountId) {
		return vehicleRepository.imageKeysOf(accountId);
	}

	/**
	 * Removes objects whose rows have already gone, and <strong>never
	 * throws</strong>.
	 *
	 * <p>That is the whole design of this method. It runs inside the transaction
	 * that deleted the rows, so an exception here would roll that deletion back -
	 * which would mean somebody could not delete their account because an object
	 * store was unreachable. A person's right to be forgotten does not depend on
	 * a bucket answering. A failure leaves an unreferenced object, which is the
	 * same leak this phase exists to fix, except now it is rare, logged, and
	 * caused by an outage rather than by design.
	 *
	 * <p>The key is logged in full, and that is deliberate rather than careless:
	 * section 22 requires keys to be UUID-derived precisely so that no email,
	 * registration number, owner name or VIN can appear in a line like this one.
	 */
	public void discard(Collection<String> objectKeys) {
		for (String objectKey : objectKeys) {
			try {
				storage.delete(objectKey);
				storage.delete(thumbnailKeyOf(objectKey));
			} catch (RuntimeException exception) {
				log.error("Could not remove object {}, whose row has already been deleted",
						objectKey, exception);
			}
		}
	}

	/**
	 * Section 22: UUID-derived, with no email, registration number, owner name or
	 * VIN anywhere in the path. The random half is what makes a replacement a new
	 * object; the vehicle's own identifier is what makes a bucket listing
	 * navigable when somebody has to look.
	 */
	private static String keyFor(UUID vehicleId) {
		return "vehicles/" + vehicleId + "/" + UUID.randomUUID();
	}

	private Vehicle owned(UUID accountId, UUID vehicleId) {
		return vehicleRepository.findByIdAndUserId(vehicleId, accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.VEHICLE_NOT_FOUND));
	}
}