import { useQueries, useQuery } from "@tanstack/react-query";
import {
  Championship,
  Enrollment,
  fetchChampionships,
  fetchEnrollments,
  fetchMatches,
  fetchStandings,
  Match,
} from "./api";

export function useChampionships() {
  return useQuery({ queryKey: ["championships"], queryFn: fetchChampionships });
}

export function useMatches() {
  return useQuery({ queryKey: ["matches"], queryFn: fetchMatches });
}

export function useEnrollments(championshipId: string | null) {
  return useQuery({
    queryKey: ["enrollments", championshipId],
    queryFn: () => fetchEnrollments(championshipId!),
    enabled: championshipId !== null,
  });
}

export function useStandings(groupId: string | null) {
  return useQuery({
    queryKey: ["standings", groupId],
    queryFn: () => fetchStandings(groupId!),
    enabled: groupId !== null,
  });
}

// ---- visao agregada das inscricoes (usada nos totais da Home) ----

export interface ChampionshipEnrollments {
  championship: Championship;
  enrollments: Enrollment[];
}

export function useAllEnrollments(): { byChampionship: ChampionshipEnrollments[]; isLoading: boolean } {
  const { data: championships = [], isLoading: loadingChampionships } = useChampionships();

  const results = useQueries({
    queries: championships.map((championship) => ({
      queryKey: ["enrollments", championship.id],
      queryFn: () => fetchEnrollments(championship.id),
    })),
  });

  return {
    byChampionship: championships.map((championship, index) => ({
      championship,
      enrollments: results[index]?.data ?? [],
    })),
    isLoading: loadingChampionships || results.some((result) => result.isLoading),
  };
}

// ---- utilidades de apresentacao ----

/** Rotulos legiveis para group_id (UUID nunca aparece na UI): "Grupo 1", "Grupo 2"… por campeonato. */
export function buildGroupLabels(matches: Match[]): Map<string, string> {
  const groupsByChampionship = new Map<string, string[]>();
  for (const match of matches) {
    if (!match.group_id) continue;
    const groups = groupsByChampionship.get(match.championship_id) ?? [];
    if (!groups.includes(match.group_id)) groups.push(match.group_id);
    groupsByChampionship.set(match.championship_id, groups);
  }
  const labels = new Map<string, string>();
  for (const groups of groupsByChampionship.values()) {
    groups.forEach((groupId, index) => labels.set(groupId, `Grupo ${index + 1}`));
  }
  return labels;
}

const STATUS_ORDER: Record<Match["status"], number> = { EM_ANDAMENTO: 0, AGENDADA: 1, FINALIZADA: 2 };

/** Ao vivo primeiro, depois agendadas (mais proxima antes), depois encerradas (mais recente antes). */
export function sortMatches(matches: Match[]): Match[] {
  return [...matches].sort((a, b) => {
    if (STATUS_ORDER[a.status] !== STATUS_ORDER[b.status]) {
      return STATUS_ORDER[a.status] - STATUS_ORDER[b.status];
    }
    if (a.status === "FINALIZADA") {
      return (b.played_at ?? b.scheduled_at).localeCompare(a.played_at ?? a.scheduled_at);
    }
    return a.scheduled_at.localeCompare(b.scheduled_at);
  });
}
