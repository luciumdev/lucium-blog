import { http } from '@/shared/api/http'

export interface PresignedUrlRequest {
    fileName: string
    contentType: string
}

export interface PresignedUrlResponse {
    uploadUrl: string
    fileUrl: string
    expiresIn: number
}

export interface UploadOptions {
    onProgress?: (progress: number) => void
    signal?: AbortSignal
}

export const filesApi = {
    /** POST /v1/files/presigned-url — admin */
    presignedUrl: (req: PresignedUrlRequest) =>
        http.post<PresignedUrlResponse>('/v1/files/presigned-url', req),
}


/**
 * 2단계 업로드 헬퍼 — XHR 기반(progress 이벤트 지원).
 *  1) 백엔드에서 presigned URL 발급
 *  2) S3로 직접 PUT 업로드 (진행률 트래킹)
 *
 * 주의:
 *  - PUT Content-Type은 presigned 발급 시 contentType과 정확히 일치해야 함
 *  - xhr.upload.progress는 클라이언트→S3 업로드 단계만 (응답 다운로드는 별개, S3 응답은 거의 빈 응답)
 *  - signal로 abort 가능 (페이지 이탈 등 cleanup)
 */
export async function uploadFileToS3(file: File, options?: UploadOptions): Promise<string> {
    const contentType = file.type || 'application/octet-stream'

    const { uploadUrl, fileUrl } = await filesApi.presignedUrl({
        fileName: file.name,
        contentType,
    })

    await new Promise<void>((resolve, reject) => {
        const xhr = new XMLHttpRequest()
        xhr.open('PUT', uploadUrl)
        xhr.setRequestHeader('Content-Type', contentType)

        // 업로드 진행률
        xhr.upload.addEventListener('progress', (e) => {
            if (e.lengthComputable && options?.onProgress) {
                const percent = Math.round((e.loaded / e.total) * 100)
                options.onProgress(percent)
            }
        })
        xhr.addEventListener('load', () => {
            if (xhr.status >= 200 && xhr.status < 300) {
                resolve()
            } else {
                reject(new Error(`S3 upload failed: ${xhr.status} ${xhr.statusText}`))
            }
        })
        xhr.addEventListener('error', () => reject(new Error('Network error')))
        xhr.addEventListener('abort', () => reject(new Error('Upload aborted')))

        if (options?.signal) {
            options.signal.addEventListener('abort', () => xhr.abort())
        }
        xhr.send(file)
    })
    return fileUrl
}