import { render, screen } from '@testing-library/react'
import Breadcrumb from '@/components/layout/Breadcrumb'

describe('Breadcrumb', () => {
  it('href 없는 마지막 항목은 span으로 렌더링된다', () => {
    render(<Breadcrumb items={[{ label: 'Home', href: '/' }, { label: '상의' }]} />)
    expect(screen.getByText('상의').tagName).toBe('SPAN')
  })

  it('href 있는 항목은 a 링크로 렌더링된다', () => {
    render(<Breadcrumb items={[{ label: 'Home', href: '/' }, { label: '상의' }]} />)
    const link = screen.getByText('Home')
    expect(link.tagName).toBe('A')
    expect(link).toHaveAttribute('href', '/')
  })

  it('구분자 › 가 항목 사이에 표시된다', () => {
    render(<Breadcrumb items={[{ label: 'Home', href: '/' }, { label: '상의' }]} />)
    expect(screen.getByText('›')).toBeInTheDocument()
  })

  it('단일 항목도 렌더링된다', () => {
    render(<Breadcrumb items={[{ label: 'Home' }]} />)
    expect(screen.getByText('Home')).toBeInTheDocument()
  })

  it('3단계 경로를 모두 렌더링한다', () => {
    render(
      <Breadcrumb
        items={[
          { label: 'Home', href: '/' },
          { label: '상의', href: '/categories/1' },
          { label: '반팔티셔츠' },
        ]}
      />
    )
    expect(screen.getByText('Home')).toBeInTheDocument()
    expect(screen.getByText('상의')).toBeInTheDocument()
    expect(screen.getByText('반팔티셔츠')).toBeInTheDocument()
  })
})
