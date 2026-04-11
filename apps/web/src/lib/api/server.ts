const BASE_URL = process.env.NEXT_PUBLIC_API_URL

export async function serverFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options?.headers },
  })
  const body = await res.json()
  if (!body.success || body.data === null) {
    throw new Error(body.error?.message ?? 'Server fetch error')
  }
  return body.data
}
