import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'
import MobileDetailPageTemplate from './MobileDetailPageTemplate.vue'

describe('MobileDetailPageTemplate', () => {
  it('renders title, subtitle, summary, content and sticky actions slots', () => {
    render(MobileDetailPageTemplate, {
      props: {
        title: '工单详情',
        subtitle: '处理中'
      },
      slots: {
        summary: '<p>摘要内容</p>',
        default: '<section>主体内容</section>',
        actions: '<button type="button">提交处置结果</button>'
      }
    })

    expect(screen.getByTestId('mobile-detail-page-template')).toBeTruthy()
    expect(screen.getByRole('heading', { name: '工单详情' })).toBeTruthy()
    expect(screen.getByText('处理中')).toBeTruthy()
    expect(screen.getByText('摘要内容')).toBeTruthy()
    expect(screen.getByText('主体内容')).toBeTruthy()
    expect(screen.getByRole('button', { name: '提交处置结果' })).toBeTruthy()
  })

  it('omits optional regions when subtitle and slots are absent', () => {
    render(MobileDetailPageTemplate, {
      props: {
        title: '工单详情'
      },
      slots: {
        default: '<section>主体内容</section>'
      }
    })

    expect(screen.getByRole('heading', { name: '工单详情' })).toBeTruthy()
    expect(screen.getByText('主体内容')).toBeTruthy()
    expect(screen.queryByTestId('mobile-detail-page-template-subtitle')).toBeNull()
    expect(screen.queryByTestId('mobile-detail-page-template-summary')).toBeNull()
    expect(screen.queryByTestId('mobile-detail-page-template-actions')).toBeNull()
  })
})
