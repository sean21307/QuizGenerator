import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
//test change
public class Main {
    static Map<String, Object> variables = new HashMap<String, Object>();

    // Replaces variable symbols with the generated values (#R1# -> 2)
    public static String replaceVariables(String line) {
        int hash1 = line.indexOf("#");
        int hash2 = line.indexOf("#", hash1 + 1);
        while (hash1 != -1) {
            String findVar = line.substring(hash1 + 1, hash2);
            Object var = variables.get(findVar);

            line = line.replace("#" + findVar + "#", var.toString());

            hash1 = line.indexOf("#");
            hash2 = line.indexOf("#", hash1 + 1);
        }

        return line;
    }


    public static void main(String[] args) {
        String filePath = "src/template.txt";
        Random rand = new Random();

        try (Scanner file = new Scanner(new File(filePath));) {
            String questionNumber = "";

            while (file.hasNext()) {
                String nextLine = file.nextLine();
                if (nextLine.isEmpty() || nextLine.contains("##")) continue;

                if (nextLine.contains("Question #")) {
                    int hashIndex = nextLine.indexOf("#");
                    questionNumber = nextLine.substring(hashIndex + 1, hashIndex + 2);
                }

                if (nextLine.charAt(0) == '#') {
                    String variableName = nextLine.substring(1, 3);
                    int colonIndex = nextLine.indexOf(":");
                    String sub = nextLine.substring(colonIndex + 1);
                    String[] variableInformation = sub.trim().split(",");
                    String variableType = variableInformation[0].trim();
                    String generationType = variableInformation[1].trim();

                    if (variableType.equals("int")) {
                        if (generationType.equals("random")) {
                            int min = Integer.parseInt(variableInformation[2].trim());
                            int max = Integer.parseInt(variableInformation[3].trim());
                            int generated = rand.nextInt(max + 1 - min) + min;
                            variables.put(variableName, generated);
                            System.out.println(String.format("Q%s-%s: %d", questionNumber, variableName, generated));
                        }
                    }

                } else if (nextLine.equals(":Text:")) {
                    while (file.hasNext()) {
                        String textLine = file.nextLine();
                        if (textLine.equals(":EndText:")) {
                            break;
                        }

                        textLine = replaceVariables(textLine);
                        System.out.println(textLine);
                    }
                } else if (nextLine.contains("Solution:")) {
                    int colonIndex = nextLine.indexOf(":");
                    String solutionString = nextLine.substring(colonIndex + 1).trim();
                    solutionString = replaceVariables(solutionString);
                    int solution = EvaluateExpression.evaluateExpression(solutionString);
                    System.out.println(solutionString);
                    System.out.println(solution);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}