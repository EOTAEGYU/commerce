import { render, screen, fireEvent } from '@testing-library/react'
import FilterSidebar from '@/components/products/FilterSidebar'

const mockPush = jest.fn()
jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: jest.fn(), back: jest.fn(), forward: jest.fn(), prefetch: jest.fn(), refresh: jest.fn() }),
  usePathname: () => '/categories/1',
  useSearchParams: () => ({ get: () => null, toString: () => '' }),
}))

beforeEach(() => mockPush.mockClear())

const categories = [
  { id: 1, name: '상의', displayOrder: 1, children: [{ id: 4, name: '반팔티셔츠', displayOrder: 1, children: [] }] },
  { id: 2, name: '하의', displayOrder: 2, children: [] },
]

describe('FilterSidebar', () => {
  it('상위 카테고리 목록이 렌더링된다', () => {
    render(<FilterSidebar categories={categories} searchParams={{}} />)
    expect(screen.getByText('상의')).toBeInTheDocument()
    expect(screen.getByText('하의')).toBeInTheDocument()
  })

  it('하위 카테고리도 렌더링된다', () => {
    render(<FilterSidebar categories={categories} searchParams={{}} />)
    expect(screen.getByText('반팔티셔츠')).toBeInTheDocument()
  })

  it('사이즈 칩 5개(XS~XL)가 렌더링된다', () => {
    render(<FilterSidebar categories={categories} searchParams={{}} />)
    ;['XS', 'S', 'M', 'L', 'XL'].forEach((size) => {
      expect(screen.getByRole('button', { name: size })).toBeInTheDocument()
    })
  })

  it('사이즈 S 클릭 시 router.push에 size=S 쿼리가 포함된다', () => {
    render(<FilterSidebar categories={categories} searchParams={{}} />)
    fireEvent.click(screen.getByRole('button', { name: 'S' }))
    expect(mockPush).toHaveBeenCalledWith('/categories/1?size=S')
  })

  it('이미 선택된 사이즈를 다시 클릭하면 필터가 해제된다', () => {
    render(<FilterSidebar categories={categories} searchParams={{ size: 'S' }} />)
    fireEvent.click(screen.getByRole('button', { name: 'S' }))
    expect(mockPush).toHaveBeenCalledWith('/categories/1')
  })

  it('컬러 버튼 6개가 렌더링된다', () => {
    render(<FilterSidebar categories={categories} searchParams={{}} />)
    expect(screen.getByLabelText('Black')).toBeInTheDocument()
    expect(screen.getByLabelText('White')).toBeInTheDocument()
  })

  it('가격 체크박스 4개가 렌더링된다', () => {
    render(<FilterSidebar categories={categories} searchParams={{}} />)
    expect(screen.getByText('5만원 미만')).toBeInTheDocument()
    expect(screen.getByText('5만원 ~ 10만원')).toBeInTheDocument()
    expect(screen.getByText('10만원 ~ 20만원')).toBeInTheDocument()
    expect(screen.getByText('20만원 이상')).toBeInTheDocument()
  })
})
