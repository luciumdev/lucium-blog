import { http } from '@/shared/api/http'
import type { Category } from './types'

export const categoryApi = {
    list: () => http.get<Category[]>('/v1/categories'),
}