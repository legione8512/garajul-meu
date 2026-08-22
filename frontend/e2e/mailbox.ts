import { readFile } from 'node:fs/promises'

const FILE = '../backend/target/e2e-mail/messages.tsv'

interface Recorded {
  readonly purpose: string
  readonly recipient: string
  readonly code: string
}

async function recorded(): Promise<readonly Recorded[]> {
  let contents: string

  try {
    contents = await readFile(FILE, 'utf8')
  } catch {
    // Not written yet. An empty inbox is an ordinary state a moment after
    // registering, not a failure - the caller is polling.
    return []
  }

  return contents
    .split('\n')
    .filter(line => line.trim().length > 0)
    .map((line) => {
      const [, purpose, recipient, , , code] = line.split('\t')
      return { purpose, recipient, code }
    })
}

/**
 * The most recent code of a given kind sent to one address.
 *
 * <p>The last matching line rather than the first: resending supersedes the
 * earlier code, and a test that read the first one would be using a code the
 * backend has already invalidated - which is exactly the behaviour
 * `VerificationTokenRepositoryTest` asserts.
 *
 * <p>Polled, because the file is written by another process during the request
 * the browser is still waiting on. Two seconds is generous for a local write and
 * short enough that a genuinely missing message fails quickly.
 */
export async function waitForCode(purpose: string, recipient: string): Promise<string> {
  const deadline = Date.now() + 2_000

  for (;;) {
    const matches = (await recorded())
      .filter(message => message.purpose === purpose && message.recipient === recipient)

    const latest = matches.at(-1)

    if (latest !== undefined) {
      return latest.code
    }

    if (Date.now() > deadline) {
      throw new Error(`No ${purpose} code was recorded for ${recipient}.`)
    }

    await new Promise(resolve => setTimeout(resolve, 100))
  }
}