import { render, screen, fireEvent } from '@testing-library/react'
import SortDropdown from '@/components/products/SortDropdown'

const mockPush = jest.fn()
jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: jest.fn(), back: jest.fn(), forward: jest.fn(), prefetch: jest.fn(), refresh: jest.fn() }),
  usePathname: () => '/categories/1',
  useSearchParams: () => ({ get: (k: string) => null, toString: () => '' }),
}))

beforeEach(() => mockPush.mockClear())

describe('SortDropdown', () => {
  it('정렬 옵션 3개가 렌더링된다', () => {
    render(<SortDropdown />)
    expect(screen.getByRole('option', { name: '최신순' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: '가격 낮은순' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: '가격 높은순' })).toBeInTheDocument()
  })

  it('기본값은 최신순이다', () => {
    render(<SortDropdown />)
    expect(screen.getByDisplayValue('최신순')).toBeInTheDocument()
  })

  it('가격 낮은순 선택 시 router.push에 sort=price_asc가 포함된다', () => {
    render(<SortDropdown />)
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'price_asc' } })
    expect(mockPush).toHaveBeenCalledWith(expect.stringContaining('sort=price_asc'))
  })

  it('정렬 변경 시 page 파라미터가 제거된다', () => {
    render(<SortDropdown />)
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'price_desc' } })
    expect(mockPush).toHaveBeenCalledWith(expect.not.stringContaining('page='))
  })
})
