import { render, screen, fireEvent } from '@testing-library/react'
import AppliedFilters from '@/components/products/AppliedFilters'

const mockPush = jest.fn()
jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: jest.fn(), back: jest.fn(), forward: jest.fn(), prefetch: jest.fn(), refresh: jest.fn() }),
  usePathname: () => '/categories/1',
  useSearchParams: () => ({ get: () => null, toString: () => '' }),
}))

beforeEach(() => mockPush.mockClear())

describe('AppliedFilters', () => {
  it('필터가 없으면 null을 반환한다', () => {
    const { container } = render(<AppliedFilters searchParams={{}} />)
    expect(container.firstChild).toBeNull()
  })

  it('size 필터 칩이 렌더링된다', () => {
    render(<AppliedFilters searchParams={{ size: 'S' }} />)
    expect(screen.getByText('Size: S')).toBeInTheDocument()
  })

  it('콤마 구분 size 필터는 칩 두 개로 분리된다', () => {
    render(<AppliedFilters searchParams={{ size: 'S,M' }} />)
    expect(screen.getByText('Size: S')).toBeInTheDocument()
    expect(screen.getByText('Size: M')).toBeInTheDocument()
  })

  it('price 필터는 한국어 레이블로 표시된다', () => {
    render(<AppliedFilters searchParams={{ price: 'under50' }} />)
    expect(screen.getByText('Price: 5만원 미만')).toBeInTheDocument()
  })

  it('✕ 클릭 시 해당 필터가 제거되고 router.push가 호출된다', () => {
    render(<AppliedFilters searchParams={{ size: 'S', color: 'black' }} />)
    fireEvent.click(screen.getByLabelText('Size: S 필터 제거'))
    expect(mockPush).toHaveBeenCalledWith('/categories/1?color=black')
  })

  it('clear all 클릭 시 모든 필터가 제거된다', () => {
    render(<AppliedFilters searchParams={{ size: 'S', color: 'black', price: 'under50' }} />)
    fireEvent.click(screen.getByText('clear all'))
    expect(mockPush).toHaveBeenCalledWith('/categories/1')
  })
})
