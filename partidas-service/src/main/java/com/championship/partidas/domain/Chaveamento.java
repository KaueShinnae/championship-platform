package com.championship.partidas.domain;

import java.util.ArrayList;
import java.util.List;

public final class Chaveamento {

    private Chaveamento() {
    }

    public static int numeroDeGrupos(int times) {
        for (int grupos : new int[]{8, 4, 2}) {
            if (times / grupos >= 3) {
                return grupos;
            }
        }
        throw new IllegalArgumentException("grupos+playoffs exige pelo menos 6 times, recebido: " + times);
    }

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

    public static List<int[]> todosContraTodos(int n) {
        List<int[]> pares = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                pares.add(new int[]{i, j});
            }
        }
        return pares;
    }

    public static List<List<int[]>> todosContraTodosPorRodada(int n) {
        List<List<int[]>> rodadas = new ArrayList<>();
        if (n < 2) {
            return rodadas;
        }
        int m = (n % 2 == 0) ? n : n + 1; // time fictício (folga) quando ímpar
        int bye = n;                       // índice do bye = n; só aparece quando ímpar (m > n)
        List<Integer> circulo = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            circulo.add(i);
        }
        for (int r = 0; r < m - 1; r++) {
            List<int[]> jogos = new ArrayList<>();
            for (int i = 0; i < m / 2; i++) {
                int a = circulo.get(i);
                int b = circulo.get(m - 1 - i);
                if (a != bye && b != bye) {
                    // ordena para casar com a lista original (índice menor = casa)
                    jogos.add(a < b ? new int[]{a, b} : new int[]{b, a});
                }
            }
            rodadas.add(jogos);
            // rotaciona mantendo o primeiro fixo: o último passa para a 2ª posição
            circulo.add(1, circulo.remove(m - 1));
        }
        return rodadas;
    }

    public static int proximaPotenciaDe2(int n) {
        int potencia = 1;
        while (potencia < n) {
            potencia *= 2;
        }
        return potencia;
    }

    public static int rodadas(int tamanhoBracket) {
        return Integer.numberOfTrailingZeros(tamanhoBracket);
    }

    public static List<Integer> slotsDeBye(int tamanhoBracket, int byes) {
        int slotsRodada2 = tamanhoBracket / 2;
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < byes; i++) {
            slots.add(i % 2 == 0 ? i / 2 : slotsRodada2 - 1 - i / 2);
        }
        return slots;
    }
}
