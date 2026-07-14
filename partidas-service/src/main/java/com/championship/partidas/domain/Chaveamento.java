package com.championship.partidas.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Regras puras (sem estado) da geração de confrontos — ver especificação de
 * formatos: grupos sempre em potência de 2 com mínimo de 3 times por grupo,
 * byes só na primeira rodada do mata-mata, alternados entre as metades do
 * bracket para equilibrar as chaves.
 */
public final class Chaveamento {

    private Chaveamento() {
    }

    /** Nº de grupos para N times: maior valor em {8, 4, 2} tal que N/G >= 3. */
    public static int numeroDeGrupos(int times) {
        for (int grupos : new int[]{8, 4, 2}) {
            if (times / grupos >= 3) {
                return grupos;
            }
        }
        throw new IllegalArgumentException("grupos+playoffs exige pelo menos 6 times, recebido: " + times);
    }

    /** Distribui em rodízio (1º item -> grupo A, 2º -> B, ...): diferença máxima de 1 por grupo. */
    public static <T> List<List<T>> distribuirEmGrupos(List<T> itens, int grupos) {
        List<List<T>> resultado = new ArrayList<>();
        for (int g = 0; g < grupos; g++) {
            resultado.add(new ArrayList<>());
        }
        for (int i = 0; i < itens.size(); i++) {
            resultado.get(i % grupos).add(itens.get(i));
        }
        return resultado;
    }

    /** Pares (índices) de todos contra todos, turno único: n*(n-1)/2 confrontos. */
    public static List<int[]> todosContraTodos(int n) {
        List<int[]> pares = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                pares.add(new int[]{i, j});
            }
        }
        return pares;
    }

    /** Menor potência de 2 >= n (tamanho do bracket). */
    public static int proximaPotenciaDe2(int n) {
        int potencia = 1;
        while (potencia < n) {
            potencia *= 2;
        }
        return potencia;
    }

    /** Nº de rodadas de um bracket de tamanho potência de 2 (log2). */
    public static int rodadas(int tamanhoBracket) {
        return Integer.numberOfTrailingZeros(tamanhoBracket);
    }

    /**
     * Slots da 2ª rodada reservados para byes, alternando metade de cima e de
     * baixo do bracket (0, última, 1, penúltima...): chaves equilibradas.
     */
    public static List<Integer> slotsDeBye(int tamanhoBracket, int byes) {
        int slotsRodada2 = tamanhoBracket / 2;
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < byes; i++) {
            slots.add(i % 2 == 0 ? i / 2 : slotsRodada2 - 1 - i / 2);
        }
        return slots;
    }
}
