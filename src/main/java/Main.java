import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class Main {
    static final String QUESTION_PREFIX = "Question #";
    static final String CODE_SECTION = ":Code:";
    static final String END_CODE_SECTION = ":EndCode:";
    static final String TEXT_SECTION = ":Text:";
    static final String END_TEXT_SECTION = ":EndText:";
    static final String SOLUTION_PREFIX = "Solution:";
    static final String SOLUTION_TYPE_PREFIX = "SolutionType:";
    static final String UNIT_PREFIX = "Unit:";
    static Map<String, Object> variables = new HashMap<>();

    public static void main(String[] args) {
        generateQuizFile("Chapter2Template");
        generateQuizFile("Chapter3Template");
        generateQuizFile("Chapter4Template");
        generateQuizFile("Chapter5Template");
        generateQuizFile("Chapter6Template");
        generateQuizFile("Chapter7Template");
    }

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

    public static void generateQuizFile(String name) {
        String filePath = String.format("src/main/java/%s.txt", name);
        String outputFileName = String.format("%s.docx", name).replace("Template", "Quiz");

        try (Scanner file = new Scanner(new File(filePath));
             XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(outputFileName)) {

            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();

            String questionNumber = "";
            String executionCode = "";
            boolean mustExecute = false;

            while (file.hasNext()) {
                String nextLine = file.nextLine();
                if (nextLine.isEmpty() || nextLine.contains("##")) continue;

                if (nextLine.contains(QUESTION_PREFIX)) {
                    int hashIndex = nextLine.indexOf("#");
                    questionNumber = nextLine.substring(hashIndex + 1, hashIndex + 2);
                    executionCode = "";
                    mustExecute = false;
                }

                if (nextLine.startsWith("#")) {
                    createVariables(nextLine);
                } else if (nextLine.equals(CODE_SECTION)) {
                    mustExecute = true;
                    executionCode = readCodeSection(file);
                } else if (nextLine.equals(TEXT_SECTION)) {
                    readTextSection(file, run, questionNumber);
                } else if (nextLine.contains(SOLUTION_PREFIX)) {
                    processSolution(file, nextLine, run, mustExecute, executionCode);
                }
            }

            document.write(out);
            System.out.println(String.format("%s generated successfully", outputFileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createVariables(String nextLine) {
        Random rand = new Random();
        String variableName = nextLine.substring(1, 3);
        int colonIndex = nextLine.indexOf(":");
        String sub = nextLine.substring(colonIndex + 1);

        if (sub.contains("{")) {
            ArrayList<String> setValues = new ArrayList<>(Arrays.asList(sub.substring(2, sub.length() - 1).split(",")));
            variables.put(variableName, setValues);
        } else if (sub.contains("from")) {
            int hashMark = sub.indexOf("#");
            int hashMark2 = sub.indexOf("#", hashMark + 1);
            String setNum = sub.substring(hashMark + 1, hashMark2);

            @SuppressWarnings("unchecked")
            ArrayList<String> setOptions = (ArrayList<String>) variables.get(setNum);
            String pickedValue = setOptions.get(rand.nextInt(setOptions.size()));
            variables.put(variableName, pickedValue);
        } else {
            String[] variableInformation = sub.trim().split(",");
            String variableType = variableInformation[0].trim();
            String generationType = variableInformation[1].trim();

            if (variableType.equals("int") && generationType.equals("random")) {
                int min = Integer.parseInt(variableInformation[2].trim());
                int max = Integer.parseInt(variableInformation[3].trim());
                int generated = rand.nextInt(max + 1 - min) + min;
                variables.put(variableName, generated);
            } else if (variableType.equals("double") && generationType.equals("random")) {
                int min = Integer.parseInt(variableInformation[2].trim());
                int max = Integer.parseInt(variableInformation[3].trim());
                double generated = rand.nextInt(max + 1 - min) + min + (rand.nextInt(9) + 1) / 10.0;
                variables.put(variableName, generated);
            }
        }
    }

    private static String readCodeSection(Scanner file) {
        StringBuilder executionCode = new StringBuilder();
        while (file.hasNext()) {
            String textLine = file.nextLine();
            if (textLine.equals(END_CODE_SECTION)) {
                break;
            }
            executionCode.append(replaceVariables(textLine)).append("\n");
        }
        return executionCode.toString();
    }

    private static void readTextSection(Scanner file, XWPFRun run, String questionNumber) {
        boolean setQuestionText = false;
        while (file.hasNext()) {
            String textLine = file.nextLine();
            if (textLine.equals(END_TEXT_SECTION)) {
                break;
            }
            textLine = replaceVariables(textLine);
            if (!setQuestionText && !textLine.contains("Type: ")) {
                setQuestionText = true;
                run.setText(questionNumber + ". " + textLine);
            } else {
                run.setText(textLine);
            }
            run.addBreak();
        }
    }

    private static void processSolution(Scanner file, String nextLine, XWPFRun run, boolean mustExecute, String executionCode) {
        if (mustExecute) {
            System.out.println(executionCode);
            String[] solutionStringArray = Executor.compileAndExecute(executionCode).split("\n");
            run.addCarriageReturn();
            for (String solString : solutionStringArray) {
                run.setText(solString);
                run.addCarriageReturn();
            }
            run.addBreak();
            run.addBreak();
        } else {
            int colonIndex = nextLine.indexOf(":");
            String solutionString = nextLine.substring(colonIndex + 1).trim();
            solutionString = replaceVariables(solutionString);
            double result = EvaluateExpression.evaluateExpression(solutionString);
            String resultString = formatResult(file, result);
            List<String> units = readUnits(file);

            String solution = formatSolution(resultString, units);
            run.setText(solution);
            run.addBreak();
            run.addBreak();
        }
    }

    private static String formatResult(Scanner file, double result) {
        String resultString;
        String type = "double";
        String nextLine = file.nextLine();
        if (nextLine.startsWith(SOLUTION_TYPE_PREFIX)) {
            type = nextLine.substring(nextLine.indexOf(":") + 1).trim();
        }

        switch (type) {
            case "long":
                resultString = String.valueOf((long) result);
                break;
            case "short":
                resultString = String.valueOf((short) result);
                break;
            case "int":
                resultString = String.valueOf((int) result);
                break;
            default:
                resultString = String.valueOf(new DecimalFormat("#.##").format(result));
                if (!resultString.contains(".")) {
                    resultString += ".0";
                }
                break;
        }
        return resultString;
    }

    private static List<String> readUnits(Scanner file) {
        List<String> units = new ArrayList<>();
        String nextLine = file.nextLine();
        if (nextLine.startsWith(UNIT_PREFIX)) {
            String unitString = nextLine.substring(nextLine.indexOf("{") + 1, nextLine.indexOf("}"));
            units = Arrays.asList(unitString.split(","));
            for (int i = 0; i < units.size(); i++) {
                units.set(i, units.get(i).trim());
            }
        }
        return units;
    }

    private static String formatSolution(String resultString, List<String> units) {
        StringBuilder solutionBuilder = new StringBuilder("[");
        for (String unit : units) {
            solutionBuilder.append(resultString).append(unit).append(", ");
        }
        solutionBuilder.setLength(solutionBuilder.length() - 2);  // Remove last comma and space
        solutionBuilder.append("]");
        return solutionBuilder.toString();
    }
}
