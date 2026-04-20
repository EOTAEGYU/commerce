import { render, screen, fireEvent, act } from '@testing-library/react'
import HeroBanner from '@/components/home/HeroBanner'

beforeEach(() => {
  jest.useFakeTimers()
})

afterEach(() => {
  jest.useRealTimers()
})

describe('HeroBanner', () => {
  it('첫 번째 슬라이드 제목이 초기에 표시된다', () => {
    render(<HeroBanner />)
    expect(screen.getByText('Spring Drop')).toBeInTheDocument()
  })

  it('점 인디케이터가 3개 표시된다', () => {
    render(<HeroBanner />)
    const dots = screen.getAllByRole('button', { name: /슬라이드 \d+/ })
    expect(dots).toHaveLength(3)
  })

  it('다음 화살표 클릭 시 두 번째 슬라이드로 이동한다', () => {
    render(<HeroBanner />)
    fireEvent.click(screen.getByLabelText('다음 슬라이드'))
    expect(screen.getByText('Summer Collection')).toBeInTheDocument()
  })

  it('이전 화살표 클릭 시 마지막 슬라이드로 순환한다', () => {
    render(<HeroBanner />)
    fireEvent.click(screen.getByLabelText('이전 슬라이드'))
    expect(screen.getByText('Best Sellers')).toBeInTheDocument()
  })

  it('점 인디케이터 클릭 시 해당 슬라이드로 이동한다', () => {
    render(<HeroBanner />)
    fireEvent.click(screen.getByLabelText('슬라이드 3'))
    expect(screen.getByText('Best Sellers')).toBeInTheDocument()
  })

  it('3초 후 자동으로 다음 슬라이드로 이동한다', () => {
    render(<HeroBanner />)
    act(() => {
      jest.advanceTimersByTime(3000)
    })
    expect(screen.getByText('Summer Collection')).toBeInTheDocument()
  })

  it('Shop Now 버튼이 표시된다', () => {
    render(<HeroBanner />)
    expect(screen.getByText('Shop Now')).toBeInTheDocument()
  })
})
