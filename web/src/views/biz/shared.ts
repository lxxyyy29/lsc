import { computed, ref } from 'vue'

export interface BizFormErrors {
  [key: string]: string
}

export function safeParseRoiJson(value: string) {
  const trimmed = value.trim()
  if (!trimmed) {
    return { valid: true, formatted: '' }
  }

  try {
    const parsed = JSON.parse(trimmed)
    return {
      valid: true,
      formatted: JSON.stringify(parsed, null, 2),
      parsed
    }
  } catch {
    return {
      valid: false,
      formatted: trimmed,
      parsed: null
    }
  }
}

export function isPointInPolygon(
  lng: number,
  lat: number,
  polygon: Array<{ lng: number; lat: number }>
): boolean {
  let inside = false
  const n = polygon.length
  for (let i = 0, j = n - 1; i < n; j = i++) {
    const xi = polygon[i].lng, yi = polygon[i].lat
    const xj = polygon[j].lng, yj = polygon[j].lat
    const intersect =
      yi > lat !== yj > lat &&
      lng < ((xj - xi) * (lat - yi)) / (yj - yi) + xi
    if (intersect) inside = !inside
  }
  return inside
}

export function isValidRoiPolygon(value: string) {
  const result = safeParseRoiJson(value)
  if (!result.valid) {
    return false
  }
  if (!value.trim()) {
    return true
  }

  return (
    Array.isArray(result.parsed) &&
    result.parsed.length >= 3 &&
    result.parsed.every(
      (item) =>
        item &&
        typeof item === 'object' &&
        typeof item.lng === 'number' &&
        typeof item.lat === 'number'
    )
  )
}

export function useBizPageState() {
  const loading = ref(false)
  const submitting = ref(false)
  const errorMessage = ref('')
  const dialogOpen = ref(false)

  return {
    loading,
    submitting,
    errorMessage,
    dialogOpen,
    tableMeta(count: number, unit: string) {
      return computed(() => `当前共 ${count} ${unit}`)
    }
  }
}

export function buildBaseErrors(): BizFormErrors {
  return {
    areaName: '',
    merchantName: '',
    vendorName: '',
    areaId: '',
    merchantPhotoUrl: '',
    legalPersonPhotoUrl: '',
    legalPersonPhone: '',
    vendorPhotoUrl: '',
    longitude: '',
    latitude: '',
    roiJson: ''
  }
}

export function isValidHttpUrl(value: string) {
  if (!value.trim()) {
    return true
  }

  try {
    const url = new URL(value)
    return ['http:', 'https:'].includes(url.protocol)
  } catch {
    return false
  }
}

export function isLikelyPhone(value: string) {
  if (!value.trim()) {
    return true
  }

  return /^[0-9+()\-\s]{6,20}$/.test(value.trim())
}
