import {useQuery} from "@tanstack/react-query";
import {marketApi} from "@/features/market/api.ts";

export function useCommoditiesQuery() {
    return useQuery({
        queryKey: ['market', 'commodities'],
        queryFn: () => marketApi.commodities(),
        staleTime: 5 * 60_000,
        refetchInterval: 30 * 60_000,
    })
}