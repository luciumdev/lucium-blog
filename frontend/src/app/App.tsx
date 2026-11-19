import {QueryClientProvider} from "@tanstack/react-query";
import {ReactQueryDevtools} from "@tanstack/react-query-devtools";
import {RouterProvider} from "react-router-dom";
import {router} from "@/app/router.tsx";
import { queryClient } from '@/shared/api/queryClient'
import {AuthBootstrap} from "@/features/auth/AuthBootstrap.tsx";

export default function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <AuthBootstrap/>
            <RouterProvider router={router} />
            {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
        </QueryClientProvider>
    )
}
