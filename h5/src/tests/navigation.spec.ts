import { afterEach, describe, expect, it, vi } from 'vitest'
import { navigateToPath, redirectToPath } from '../uni/navigation'

describe('h5 uni navigation', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    delete (globalThis as { uni?: unknown }).uni
  })

  it('uses switchTab when redirecting to configured tabBar pages', () => {
    const switchTab = vi.fn()
    const reLaunch = vi.fn()
    ;(globalThis as { uni?: unknown }).uni = {
      reLaunch,
      switchTab
    }

    redirectToPath('/workbench')

    expect(switchTab).toHaveBeenCalledWith({ url: '/pages/workbench/index' })
    expect(reLaunch).not.toHaveBeenCalled()
  })

  it('does not treat the work order list as a tabBar page', () => {
    const switchTab = vi.fn()
    const navigateTo = vi.fn()
    ;(globalThis as { uni?: unknown }).uni = {
      reLaunch: vi.fn(),
      switchTab,
      navigateTo
    }

    navigateToPath('/work-orders')

    expect(navigateTo).toHaveBeenCalledWith({ url: '/pages/workorder/list' })
    expect(switchTab).not.toHaveBeenCalled()
  })
})
