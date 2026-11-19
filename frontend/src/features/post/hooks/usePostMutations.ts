import { useMutation, useQueryClient } from '@tanstack/react-query'
import { postApi } from '../api'
import type { PostCreateInput, PostUpdateInput } from '../types'

export function useCreatePostMutation() {
    const qc = useQueryClient()
    return useMutation({
        mutationFn: (input: PostCreateInput) => postApi.create(input),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['posts'] })
            qc.invalidateQueries({ queryKey: ['tags'] })
            qc.invalidateQueries({ queryKey: ['stat'] })
        },
    })
}

export function useUpdatePostMutation() {
    const qc = useQueryClient()
    return useMutation({
        mutationFn: ({ publicId, input }: { publicId: string; input: PostUpdateInput }) =>
            postApi.update(publicId, input),
        onSuccess: (_data, vars) => {
            qc.invalidateQueries({ queryKey: ['posts'] })
            qc.invalidateQueries({ queryKey: ['posts', 'detail', vars.publicId] })
            qc.invalidateQueries({ queryKey: ['tags'] })
            qc.invalidateQueries({ queryKey: ['stat'] })
        },
    })
}

export function useDeletePostMutation() {
    const qc = useQueryClient()
    return useMutation({
        mutationFn: (publicId: string) => postApi.remove(publicId),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['posts'] })
            qc.invalidateQueries({ queryKey: ['tags'] })
            qc.invalidateQueries({ queryKey: ['stat'] })
        },
    })
}

export function useTogglePublishMutation() {
    const qc = useQueryClient()
    return useMutation({
        mutationFn: ({ publicId, publish }: { publicId: string; publish: boolean }) =>
            postApi.togglePublish(publicId, publish),
        onSuccess: (_data, vars) => {
            qc.invalidateQueries({ queryKey: ['posts'] })
            qc.invalidateQueries({ queryKey: ['posts', 'detail', vars.publicId] })
            qc.invalidateQueries({ queryKey: ['tags'] })
            qc.invalidateQueries({ queryKey: ['stat'] })
        },
    })
}