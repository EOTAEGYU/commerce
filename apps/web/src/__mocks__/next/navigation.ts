export const useRouter = jest.fn(() => ({
  push: jest.fn(),
  replace: jest.fn(),
  back: jest.fn(),
  forward: jest.fn(),
  prefetch: jest.fn(),
  refresh: jest.fn(),
}))

export const usePathname = jest.fn(() => '/')

export const useSearchParams = jest.fn(() => ({
  get: jest.fn((key: string) => null),
  toString: jest.fn(() => ''),
}))
