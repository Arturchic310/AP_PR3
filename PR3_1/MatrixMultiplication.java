package PR3_1;

import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;

public class MatrixMultiplication {
    public static void main(String[] args) {
        // Введення параметрів користувача
        Scanner scanner = new Scanner(System.in);
        System.out.print("Введіть кількість рядків для Матриці A: ");
        int rowsA = scanner.nextInt();
        System.out.print("Введіть кількість стовпців для Матриці A (і рядків для Матриці B): ");
        int colsA = scanner.nextInt();
        System.out.print("Введіть кількість стовпців для Матриці B: ");
        int colsB = scanner.nextInt();

        System.out.print("Введіть мінімальне значення для елементів матриці: ");
        int minValue = scanner.nextInt();
        System.out.print("Введіть максимальне значення для елементів матриці: ");
        int maxValue = scanner.nextInt();

        // Генерація матриць
        int[][] matrixA = generateMatrix(rowsA, colsA, minValue, maxValue);
        int[][] matrixB = generateMatrix(colsA, colsB, minValue, maxValue);

        System.out.println("\nМатриця A:");
        printMatrix(matrixA);

        System.out.println("\nМатриця B:");
        printMatrix(matrixB);

        // Вирахування добутку матриць з використанням Fork/Join Framework (Work stealing)
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        long startForkJoin = System.currentTimeMillis();
        int[][] resultForkJoin = forkJoinPool.invoke(new MatrixMultiplicationTask(matrixA, matrixB, 0, rowsA));
        long endForkJoin = System.currentTimeMillis();

        System.out.println("\nРезультат (Fork/Join Framework):");
        printMatrix(resultForkJoin);
        System.out.println("Час виконання (Fork/Join): " + (endForkJoin - startForkJoin) + " ms");

        // Вирахування добутку матриць з використанням Thread Pool (Work dealing)
        ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        long startThreadPool = System.currentTimeMillis();
        int[][] resultThreadPool = multiplyMatricesWithThreadPool(matrixA, matrixB, threadPool);
        long endThreadPool = System.currentTimeMillis();

        threadPool.shutdown();

        System.out.println("\nРезультат (Thread Pool):");
        printMatrix(resultThreadPool);
        System.out.println("Час виконання (Thread Pool): " + (endThreadPool - startThreadPool) + " ms");
    }

    // Генерація матриці з рандомними значеннями
    private static int[][] generateMatrix(int rows, int cols, int minValue, int maxValue) {
        Random random = new Random();
        int[][] matrix = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = random.nextInt(maxValue - minValue + 1) + minValue;
            }
        }
        return matrix;
    }

    // Вивід матриці на екран
    private static void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            for (int value : row) {
                System.out.printf("%4d", value);
            }
            System.out.println();
        }
    }

    // Множення матриць через Fork/Join Framework
    private static class MatrixMultiplicationTask extends RecursiveTask<int[][]> {
        private static final int THRESHOLD = 100;
        private final int[][] matrixA;
        private final int[][] matrixB;
        private final int startRow;
        private final int endRow;

        public MatrixMultiplicationTask(int[][] matrixA, int[][] matrixB, int startRow, int endRow) {
            this.matrixA = matrixA;
            this.matrixB = matrixB;
            this.startRow = startRow;
            this.endRow = endRow;
        }

        @Override
        protected int[][] compute() {
            int rows = endRow - startRow;
            if (rows <= THRESHOLD) {
                return multiplyMatricesDirectly(matrixA, matrixB, startRow, endRow);
            } else {
                int midRow = startRow + rows / 2;
                MatrixMultiplicationTask upperHalf = new MatrixMultiplicationTask(matrixA, matrixB, startRow, midRow);
                MatrixMultiplicationTask lowerHalf = new MatrixMultiplicationTask(matrixA, matrixB, midRow, endRow);
                invokeAll(upperHalf, lowerHalf);
                int[][] upperResult = upperHalf.join();
                int[][] lowerResult = lowerHalf.join();
                return combineResults(upperResult, lowerResult);
            }
        }

        private int[][] multiplyMatricesDirectly(int[][] matrixA, int[][] matrixB, int startRow, int endRow) {
            int colsB = matrixB[0].length;
            int[][] result = new int[endRow - startRow][colsB];
            for (int i = startRow; i < endRow; i++) {
                for (int j = 0; j < colsB; j++) {
                    for (int k = 0; k < matrixA[0].length; k++) {
                        result[i - startRow][j] += matrixA[i][k] * matrixB[k][j];
                    }
                }
            }
            return result;
        }

        private int[][] combineResults(int[][] upperResult, int[][] lowerResult) {
            int[][] combined = new int[upperResult.length + lowerResult.length][upperResult[0].length];
            System.arraycopy(upperResult, 0, combined, 0, upperResult.length);
            System.arraycopy(lowerResult, 0, combined, upperResult.length, lowerResult.length);
            return combined;
        }
    }

    // Множення матриць через Thread Pool
    private static int[][] multiplyMatricesWithThreadPool(int[][] matrixA, int[][] matrixB, ExecutorService threadPool) {
        int rowsA = matrixA.length;
        int colsB = matrixB[0].length;
        int[][] result = new int[rowsA][colsB];

        CountDownLatch latch = new CountDownLatch(rowsA);
        for (int i = 0; i < rowsA; i++) {
            final int row = i;
            threadPool.submit(() -> {
                for (int j = 0; j < colsB; j++) {
                    for (int k = 0; k < matrixA[0].length; k++) {
                        result[row][j] += matrixA[row][k] * matrixB[k][j];
                    }
                }
                latch.countDown();
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        return result;
    }
}
