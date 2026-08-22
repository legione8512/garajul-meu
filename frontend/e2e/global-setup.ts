const HEALTH = 'http://localhost:8080/actuator/health'

/**
 * Refuses to start unless the backend is up, and says how to start it.
 *
 * <p>Without this the whole suite fails inside the first form submission, with a
 * timeout on a button that looks broken - and the real cause, that nothing is
 * listening on 8080, appears nowhere. One request up front turns twenty
 * confusing failures into one sentence.
 *
 * <p>The original error travels as `cause`. The text below is a guess about why
 * the request failed, and it is the likeliest guess by far - but it is still a
 * guess. If the real reason is something else entirely, the message would send
 * whoever reads it to start a backend that is already running, so the evidence
 * that contradicts it has to survive.
 */
export default async function globalSetup() {
  try {
    const response = await fetch(HEALTH)

    if (!response.ok) {
      throw new Error(`answered ${String(response.status)}`)
    }
  } catch (cause) {
    throw new Error(
      `The backend is not answering at ${HEALTH} (${String(cause)}).\n\n`
      + 'Start it against a throwaway container, from backend/:\n\n'
      + '    mvnw spring-boot:test-run\n\n'
      + 'Not the ordinary spring-boot:run - that points at Neon development, and '
      + 'section 26 keeps automated tests off it.',
      { cause },
    )
  }
}