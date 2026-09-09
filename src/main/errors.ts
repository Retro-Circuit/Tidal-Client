export function flattenError(error: unknown): string {
  if (error == null) return 'Unknown error'
  if (typeof error === 'string') {
    return stripRemotePrefix(error)
  }
  if (error instanceof Error) {
    const nested = error as Error & { errors?: unknown[]; cause?: unknown; code?: string }
    if (Array.isArray(nested.errors) && nested.errors.length > 0) {
      const joined = nested.errors.map(flattenError).filter(Boolean).join(' | ')
      if (joined) return joined
    }
    const message = stripRemotePrefix(error.message || error.name || 'Error')
    if ((message === 'AggregateError' || error.name === 'AggregateError') && nested.cause) {
      return flattenError(nested.cause)
    }
    if (nested.cause) {
      const cause = flattenError(nested.cause)
      if (cause && cause !== message) return `${message} (${cause})`
    }
    if (message === 'AggregateError' || error.name === 'AggregateError') {
      return 'Could not reach Mojang or the loader CDN. Check your internet connection and try again.'
    }
    return message
  }
  return String(error)
}

function stripRemotePrefix(text: string): string {
  return text.replace(/^Error invoking remote method '[^']+':\s*/, '')
}

export async function withRetries<T>(label: string, run: () => Promise<T>, attempts = 3): Promise<T> {
  let last: unknown
  for (let i = 0; i < attempts; i++) {
    try {
      return await run()
    } catch (error) {
      last = error
      await new Promise((resolve) => setTimeout(resolve, 400 * (i + 1)))
    }
  }
  throw new Error(`${label} failed: ${flattenError(last)}`)
}

export async function withTimeout<T>(label: string, ms: number, run: () => Promise<T>): Promise<T> {
  let timer: ReturnType<typeof setTimeout> | undefined
  try {
    return await Promise.race([
      run(),
      new Promise<T>((_, reject) => {
        timer = setTimeout(() => {
          reject(new Error(`${label} timed out. Check your internet connection and try again.`))
        }, ms)
      })
    ])
  } finally {
    if (timer) clearTimeout(timer)
  }
}
