package PR3_2;

import java.io.File;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ForkJoinPool;
import java.util.Scanner;

// Завдання на пошук файлів, у назві яких є вказане слово чи літера
class FileSearchTask extends RecursiveTask<Integer> {
    private final File directory;
    private final String query;

    public FileSearchTask(File directory, String query) {
        this.directory = directory;
        this.query = query;
    }

    @Override
    protected Integer compute() {
        int count = 0;
        File[] files = directory.listFiles();
        if (files == null) {
            return 0;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // Створіть підзавдання для кожного підкаталогу
                FileSearchTask subTask = new FileSearchTask(file, query);
                subTask.fork(); // Виконувати асинхронно
                count += subTask.join(); // Об’єднайте результати
            } else if (file.getName().contains(query)) {
                count++;
            }
        }
        return count;
    }
}

public class FileSearchApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Отримати шлях до каталогу від користувача
        System.out.print("Введіть шлях до каталогу: ");
        String directoryPath = scanner.nextLine();

        // Отримати пошуковий запит від користувача
        System.out.print("Введіть літеру або слово для пошуку: ");
        String query = scanner.nextLine();

        File directory = new File(directoryPath);

        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("Недійсний шлях до каталогу.");
            return;
        }

        // Створіть ForkJoinPool для виконання завдання
        ForkJoinPool pool = new ForkJoinPool();
        FileSearchTask task = new FileSearchTask(directory, query);

        // Почніть виконувати завдання та отримайте результат
        int result = pool.invoke(task);

        System.out.println("Загальна кількість файлів, що містять '" + query + "': " + result);
    }
}
