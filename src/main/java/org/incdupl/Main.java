package org.incdupl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.Normalizer;
import java.util.*;

public class Main {

    private static final int NUM_HASH_FUNCTIONS = 100; // Количество хэш-функций для MinHash

    public static void main(String[] args) throws IOException {

        String[] filePaths = {"src/main/resources/f1.txt", "src/main/resources/f2.txt", "src/main/resources/f3.txt"};

        List<int[]> minHashSignatures = new ArrayList<>(); // Список для хранения MinHash-подписей

        // Генерируем коэффициенты для хэш-функций
        int[][] hashCoefficients = generateHashFunctions();

        List<Set<String>> allShingles = new ArrayList<>(); // Список для хранения шинглов всех документов

        for (String filePath : filePaths) {
            // Читаем содержимое файла
            String content = readFile(filePath);

            // Канонизируем текст
            String canonicalText = canonicalizeText(content);

            // Генерируем шинглы (разбиваем текст на подстроки длиной 3 слова)
            Set<String> shingles = generateShingles(canonicalText, 3);
            allShingles.add(shingles);

            // Вычисляем MinHash-подпись для текущего набора шинглов
            int[] signature = computeMinHashSignature(shingles, hashCoefficients);
            minHashSignatures.add(signature);
        }

        // Сравниваем все документы друг с другом
        for (int i = 0; i < minHashSignatures.size(); i++) {
            for (int j = i + 1; j < minHashSignatures.size(); j++) {
                // Вычисляем схожесть по MinHash-подписям
                double similarity = computeMinHashSimilarity(minHashSignatures.get(i), minHashSignatures.get(j));

                // Находим пересечение шинглов между двумя документами
                Set<String> intersection = new HashSet<>(allShingles.get(i));
                intersection.retainAll(allShingles.get(j));
                System.out.printf("Intersection size between Doc %d and Doc %d: %d\n", i + 1, j + 1, intersection.size());

                // Выводим процент схожести
                System.out.printf("Similarity between Document %d and Document %d: %.2f%%\n", i + 1, j + 1, similarity * 100);
            }
        }
    }

    // Читаем тексты из файлов
    private static String readFile(String filePath) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String sCurrentLine;
            // Читаем файл построчно и добавляем строки в StringBuilder
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append(" ");
            }
        }
        return contentBuilder.toString(); // Возвращаем весь текст файла
    }

    // Канонизация текста
    private static String canonicalizeText(String text) {
        text = text.toLowerCase(); // Переводим весь текст в нижний регистр
        // Убираем диакритические знаки
        text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        // Оставляем только буквы, цифры и пробелы
        text = text.replaceAll("[^a-z0-9\\s]", " ");
        // Удаляем ведущие нули, если они есть
        text = text.replaceAll("\\b0(?=\\d)", "");
        // Упрощаем пробелы и удаляем лишние пробелы в начале и конце
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    // Генерация шинглов
    private static Set<String> generateShingles(String text, int shingleLength) {
        Set<String> shingles = new HashSet<>();
        String[] words = text.split("\\s+"); // Разделяем текст на слова
        // Составляем шинглы из последовательностей shingleLength слов
        for (int i = 0; i <= words.length - shingleLength; i++) {
            StringBuilder shingle = new StringBuilder();
            for (int j = 0; j < shingleLength; j++) {
                shingle.append(words[i + j]);
                if (j < shingleLength - 1) {
                    shingle.append(" ");
                }
            }
            shingles.add(shingle.toString()); // Добавляем шингл в набор
        }
        return shingles; // Возвращаем набор шинглов
    }

    // Генерация коэффициентов для хэш-функций
    private static int[][] generateHashFunctions() {
        int[][] hashCoefficients = new int[Main.NUM_HASH_FUNCTIONS][2];
        Random rand = new Random(12345);
        // Генерируем коэффициенты a и b для каждой хэш-функции
        for (int i = 0; i < Main.NUM_HASH_FUNCTIONS; i++) {
            hashCoefficients[i][0] = rand.nextInt(Integer.MAX_VALUE) + 1;
            hashCoefficients[i][1] = rand.nextInt(Integer.MAX_VALUE) + 1;
        }
        return hashCoefficients; // Возвращаем массив коэффициентов
    }

    // Считаем хэш-подписи
    private static int[] computeMinHashSignature(Set<String> shingles, int[][] hashCoefficients) {
        int numHashFunctions = hashCoefficients.length;
        int[] signature = new int[numHashFunctions];
        Arrays.fill(signature, Integer.MAX_VALUE);

        for (String shingle : shingles) {
            int shingleHash = shingle.hashCode(); // Вычисляем хэш шингла
            for (int i = 0; i < numHashFunctions; i++) {
                int a = hashCoefficients[i][0];
                int b = hashCoefficients[i][1];

                int hashValue = Math.abs(a * shingleHash + b);
                // Обновляем значение MinHash подписи
                signature[i] = Math.min(signature[i], hashValue);
            }
        }
        return signature; // Возвращаем MinHash-подпись
    }

    // Вычисление сходства хэш-подписей
    private static double computeMinHashSimilarity(int[] signature1, int[] signature2) {
        int identicalMinHashes = 0;
        // Считаем, сколько хэшей совпадает в подписях
        for (int i = 0; i < signature1.length; i++) {
            if (signature1[i] == signature2[i]) {
                identicalMinHashes++;
            }
        }
        // Возвращаем процент совпадений
        return (double) identicalMinHashes / signature1.length;
    }
}
