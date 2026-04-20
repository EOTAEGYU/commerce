import { render, screen, fireEvent } from '@testing-library/react'
import SearchOverlay from '@/components/search/SearchOverlay'

const mockPush = jest.fn()
jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: jest.fn(), back: jest.fn(), forward: jest.fn(), prefetch: jest.fn(), refresh: jest.fn() }),
  usePathname: () => '/',
  useSearchParams: () => ({ get: () => null, toString: () => '' }),
}))

beforeEach(() => {
  mockPush.mockClear()
  localStorage.clear()
})

describe('SearchOverlay', () => {
  it('isOpen=false 이면 렌더링하지 않는다', () => {
    const { container } = render(<SearchOverlay isOpen={false} onClose={jest.fn()} />)
    expect(container.firstChild).toBeNull()
  })

  it('isOpen=true 이면 오버레이가 표시된다', () => {
    render(<SearchOverlay isOpen={true} onClose={jest.fn()} />)
    expect(screen.getByPlaceholderText('검색어를 입력하세요...')).toBeInTheDocument()
  })

  it('인기 검색어 5개가 표시된다', () => {
    render(<SearchOverlay isOpen={true} onClose={jest.fn()} />)
    expect(screen.getByText('Popular Now')).toBeInTheDocument()
    expect(screen.getByText('봄 자켓')).toBeInTheDocument()
    expect(screen.getByText('스니커즈')).toBeInTheDocument()
  })

  it('배경 클릭 시 onClose가 호출된다', () => {
    const onClose = jest.fn()
    render(<SearchOverlay isOpen={true} onClose={onClose} />)
    // 고정 오버레이 div 클릭 (첫 번째 fixed div)
    const overlay = document.querySelector('.fixed')!
    fireEvent.click(overlay)
    expect(onClose).toHaveBeenCalled()
  })

  it('ESC 키 입력 시 onClose가 호출된다', () => {
    const onClose = jest.fn()
    render(<SearchOverlay isOpen={true} onClose={onClose} />)
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalled()
  })

  it('검색어 입력 후 폼 submit 시 router.push가 호출된다', () => {
    const onClose = jest.fn()
    render(<SearchOverlay isOpen={true} onClose={onClose} />)
    const input = screen.getByPlaceholderText('검색어를 입력하세요...')
    fireEvent.change(input, { target: { value: '린넨 셔츠' } })
    fireEvent.submit(input.closest('form')!)
    expect(mockPush).toHaveBeenCalledWith('/search?q=%EB%A6%B0%EB%84%A8%20%EC%85%94%EC%B8%A0')
  })

  it('검색 후 localStorage에 검색어가 저장된다', () => {
    render(<SearchOverlay isOpen={true} onClose={jest.fn()} />)
    const input = screen.getByPlaceholderText('검색어를 입력하세요...')
    fireEvent.change(input, { target: { value: '청바지' } })
    fireEvent.submit(input.closest('form')!)
    const history = JSON.parse(localStorage.getItem('search_history') ?? '[]')
    expect(history[0]).toBe('청바지')
  })

  it('빈 검색어로 submit 시 router.push가 호출되지 않는다', () => {
    render(<SearchOverlay isOpen={true} onClose={jest.fn()} />)
    const input = screen.getByPlaceholderText('검색어를 입력하세요...')
    fireEvent.submit(input.closest('form')!)
    expect(mockPush).not.toHaveBeenCalled()
  })
})
