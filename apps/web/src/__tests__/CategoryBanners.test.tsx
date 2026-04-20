import { render, screen } from '@testing-library/react'
import CategoryBanners from '@/components/home/CategoryBanners'

const makeCategories = (count: number) =>
  Array.from({ length: count }, (_, i) => ({
    id: i + 1,
    name: `카테고리 ${i + 1}`,
    displayOrder: i + 1,
    children: [],
  }))

describe('CategoryBanners', () => {
  it('카테고리 이름이 렌더링된다', () => {
    render(<CategoryBanners categories={makeCategories(3)} />)
    expect(screen.getByText('카테고리 1')).toBeInTheDocument()
    expect(screen.getByText('카테고리 2')).toBeInTheDocument()
    expect(screen.getByText('카테고리 3')).toBeInTheDocument()
  })

  it('5개 이상이면 최대 4개만 렌더링된다', () => {
    render(<CategoryBanners categories={makeCategories(6)} />)
    expect(screen.getAllByText(/카테고리/)).toHaveLength(4)
  })

  it('각 배너 링크가 올바른 categories/[id] href를 가진다', () => {
    render(<CategoryBanners categories={makeCategories(2)} />)
    const links = screen.getAllByRole('link')
    expect(links[0]).toHaveAttribute('href', '/categories/1')
    expect(links[1]).toHaveAttribute('href', '/categories/2')
  })

  it('빈 배열이면 아무것도 렌더링하지 않는다', () => {
    const { container } = render(<CategoryBanners categories={[]} />)
    expect(container.querySelector('a')).toBeNull()
  })
})
