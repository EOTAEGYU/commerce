import { render, screen } from '@testing-library/react'
import HomeSection from '@/components/home/HomeSection'

jest.mock('@/components/products/ProductCard', () => ({
  __esModule: true,
  default: ({ product }: { product: { id: number; name: string } }) => (
    <div data-testid="product-card">{product.name}</div>
  ),
}))

const makeProducts = (count: number) =>
  Array.from({ length: count }, (_, i) => ({
    id: i + 1,
    name: `상품 ${i + 1}`,
    price: 10000,
    imageUrl: null,
    categoryId: 1,
    categoryName: '상의',
    stockQuantity: 10,
    likeCount: 0,
  }))

describe('HomeSection', () => {
  it('빈 배열이면 null을 반환한다', () => {
    const { container } = render(
      <HomeSection title="New Arrivals" products={[]} href="/search" />
    )
    expect(container.firstChild).toBeNull()
  })

  it('섹션 제목이 렌더링된다', () => {
    render(<HomeSection title="New Arrivals" products={makeProducts(3)} href="/search" />)
    expect(screen.getByText('New Arrivals')).toBeInTheDocument()
  })

  it('See All 링크가 올바른 href를 가진다', () => {
    render(<HomeSection title="New Arrivals" products={makeProducts(3)} href="/search?sort=newest" />)
    const link = screen.getByText('See All').closest('a')
    expect(link).toHaveAttribute('href', '/search?sort=newest')
  })

  it('상품이 최대 5개까지 렌더링된다', () => {
    render(<HomeSection title="New Arrivals" products={makeProducts(10)} href="/search" />)
    expect(screen.getAllByTestId('product-card')).toHaveLength(5)
  })

  it('상품이 3개면 3개만 렌더링된다', () => {
    render(<HomeSection title="New Arrivals" products={makeProducts(3)} href="/search" />)
    expect(screen.getAllByTestId('product-card')).toHaveLength(3)
  })
})
