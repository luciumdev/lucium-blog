import {useQuery} from "@tanstack/react-query";
import {marketApi} from "@/features/market/api.ts";

export function useIndicesQuery() {
    return useQuery({
        queryKey: ['market', 'indices'],
        queryFn: () => marketApi.indices(),
        staleTime: 5 * 60_000,
        refetchInterval: 30 * 60_000,
    })
}
