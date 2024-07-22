import com.opencsv.CSVWriter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    static final String QUESTION_PREFIX = "Question #";
    static final String QUESTION_TYPE_PREFIX = "QuestionType:";
    static final String CODE_SECTION = ":Code:";
    static final String END_CODE_SECTION = ":EndCode:";
    static final String TEXT_SECTION = ":Text:";
    static final String END_TEXT_SECTION = ":EndText:";
    static final String SOLUTION_PREFIX = "Solution:";
    static final String SOLUTION_TYPE_PREFIX = "SolutionType:";
    static final String UNIT_PREFIX = "Unit:";
    static final String TITLE_PREFIX = "Title:";
    static Map<String, Object> variables = new HashMap<>();

    public static void main(String[] args) {

        generateQuizFile("Chapter2Template");
        generateQuizFile("Chapter3Template");
        generateQuizFile("Chapter4Template");
        generateQuizFile("Chapter5Template");
        generateQuizFile("Chapter6Template");
        generateQuizFile("Chapter7Template");
        generateQuizFile("MCTemplate");

    }

    // Generates a CSV file for questions
    public static void writeToCsv(String fileName, List<String[][]> questions) {
        File file = new File(fileName);
        try {
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(file);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);

            for (String[][] arr: questions) {
                for (String[] data: arr) {
                    writer.writeNext(data);
                }

                writer.writeNext(new String[]{});
            }

            // closing writer connection
            writer.close();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // Replaces variable symbols with the generated values (#R1# -> 2)
    public static String replaceVariables(String line) {
        int hash1 = line.indexOf("#");
        int hash2 = line.indexOf("#", hash1 + 1);

        while (hash1 != -1) {
            if (hash2 == -1) {
                System.out.println("Isolated hashtag in line: " + line);
                continue;
            }
            String findVar = line.substring(hash1 + 1, hash2);
            Object var = variables.get(findVar);

            if (var == null) {
                throw new IllegalArgumentException(findVar + " is not a defined variable");
            }

            line = line.replace("#" + findVar + "#", var.toString());

            hash1 = line.indexOf("#");
            hash2 = line.indexOf("#", hash1 + 1);
        }

        return line;
    }

    public static void generateQuizFile(String name) {
        String filePath = String.format("src/main/java/%s.txt", name);
        String outputFileName = String.format("%s.docx", name).replace("Template", "Quiz");
        String csvFileName = String.format("%s.csv", name).replace("Template", "Quiz");

        List<String[][]> allQuestions = new ArrayList<>();

        try (Scanner file = new Scanner(new File(filePath));
             XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(outputFileName)) {

            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();

            String questionNumber = "";
            String executionCode = "";
            String questionType = "";
            String csvQuestionText = "";
            String title = "";
            boolean mustExecute = false;

            while (file.hasNext()) {
                String nextLine = file.nextLine();
                if (nextLine.isEmpty() || nextLine.contains("##")) continue;

                if (nextLine.contains(QUESTION_PREFIX)) {
                    int hashIndex = nextLine.indexOf("#");
                    questionNumber = nextLine.substring(hashIndex + 1, hashIndex + 2);
                    executionCode = "";
                    mustExecute = false;
                } else if (nextLine.contains(TITLE_PREFIX)) {
                    int colonIndex = nextLine.indexOf(":");
                    title = nextLine.substring(colonIndex + 1).trim();
                }

                if (nextLine.startsWith("#")) {
                    createVariables(nextLine);
                } else if (nextLine.equals(CODE_SECTION)) {
                    mustExecute = true;
                    executionCode = readCodeSection(file);
                } else if (nextLine.equals(TEXT_SECTION)) {
                    csvQuestionText = readTextSection(file, run, questionNumber);
                } else if (nextLine.contains(SOLUTION_PREFIX)) {
                    processSolution(file, nextLine, run, mustExecute, executionCode, questionType, csvFileName, csvQuestionText, allQuestions, title);
                } else if (nextLine.contains(QUESTION_TYPE_PREFIX)) {
                    questionType = nextLine.substring(nextLine.indexOf(":") + 1).trim();
                }
            }

            document.write(out);
            writeToCsv(csvFileName, allQuestions);
            System.out.println(String.format("%s generated successfully", outputFileName));
            System.out.println(String.format("%s generated successfully", csvFileName));
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

        } else if (sub.contains("#")) {
            String[] variableInformation = sub.trim().split(",");
            String varName = variableInformation[0].trim();
            String method = variableInformation[1].trim();
            String value = variableInformation[2].trim();

            int hashMark = sub.indexOf("#");
            int hashMark2 = sub.indexOf("#", hashMark + 1);
            String var = sub.substring(hashMark + 1, hashMark2);
            Object referenceVariable = variables.get(var);

            if (method.equalsIgnoreCase("add")) {
                variables.put(variableName, (int) referenceVariable + Integer.parseInt(value));
            }

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

    private static String readTextSection(Scanner file, XWPFRun run, String questionNumber) {
        boolean setQuestionText = false;
        String csvQuestionText = "";
        while (file.hasNext()) {
            String textLine = file.nextLine();
            if (textLine.equals(END_TEXT_SECTION)) {
                break;
            }
            textLine = replaceVariables(textLine);
            csvQuestionText += textLine + "\n";
            if (!setQuestionText && !textLine.contains("Type: ")) {
                setQuestionText = true;
                run.setText(questionNumber + ". " + textLine);
            } else {
                run.setText(textLine);
            }
            run.addBreak();
        }

        return csvQuestionText;
    }

    public static Object extractValue(String str) {
        // Pattern for extracting a number
        Pattern numberPattern = Pattern.compile("\\[(\\d+)]");
        Matcher numberMatcher = numberPattern.matcher(str);

        if (numberMatcher.find()) {
            return Integer.parseInt(numberMatcher.group(1));
        }

        // Pattern for extracting a boolean
        Pattern booleanPattern = Pattern.compile("\\[(true|false)]");
        Matcher booleanMatcher = booleanPattern.matcher(str);

        if (booleanMatcher.find()) {
            return Boolean.parseBoolean(booleanMatcher.group(1));
        }

        // Pattern for extracting a custom string inside []
        Pattern customPattern = Pattern.compile("\\[(.*?)]");
        Matcher customMatcher = customPattern.matcher(str);

        if (customMatcher.find()) {
            return customMatcher.group(1);
        }

        return null;
    }

    private static void processSolution(Scanner file, String nextLine, XWPFRun run, boolean mustExecute, String executionCode, String questionType, String csvFileName, String questionText, List<String[][]> allQuestions, String title) {
        if (mustExecute) {
            System.out.println(executionCode);
            String[] solutionStringArray = Executor.compileAndExecute(executionCode).split("\n");

            if (questionType.equalsIgnoreCase("MC")) {
                String[] choices = new String[0];
                String[] points = new String[0];
                boolean readingPoints = false;
                int index = 0;

                for (String solString : solutionStringArray) {
                    if (solString.contains("Choices:")) {
                        int length = Integer.parseInt(solString.substring(solString.indexOf(":") + 1).trim());
                        choices = new String[length];
                        points = new String[length];
                        continue;
                    } else if (solString.contains("Points:")) {
                        readingPoints = true;
                        index = 0;
                        continue;
                    }

                    if (readingPoints) {
                        points[index] = solString;
                    } else {
                        choices[index] = solString;
                    }

                    index += 1;
                }

                run.addCarriageReturn();
                for (int i = 0; i < choices.length; i++) {
                    run.setText(choices[i].trim() + ": " + points[i].trim() + "%\n");
                    run.addCarriageReturn();
                }

                run.addBreak();
                run.addBreak();


                // Generate CSV
                String[][] csvData = new String[5 + choices.length][];
                csvData[0] = new String[]{"NewQuestion", "MC"};
                csvData[1] = new String[]{"Title", title};
                csvData[2] = new String[]{"QuestionText", questionText};
                csvData[3] = new String[]{"Points", "1"};
                csvData[4] = new String[]{"Difficulty", "1"};
                for (int i = 0; i < choices.length; i++) {
                    csvData[5 + i] = new String[]{"Option", points[i].trim(), choices[i].trim()};
                }


                allQuestions.add(csvData);

            } else {
                // Generate CSV
                String[][] csvData = new String[6 + solutionStringArray.length][];
                csvData[0] = new String[]{"NewQuestion", "SA"};
                csvData[1] = new String[]{"Title", title};
                csvData[2] = new String[]{"QuestionText", questionText};
                csvData[3] = new String[]{"Points", "1"};
                csvData[4] = new String[]{"Difficulty", "1"};
                csvData[5] = new String[]{"InputBox", String.valueOf(solutionStringArray.length), "40"};
                for (int i = 0; i < solutionStringArray.length; i++) {
                    csvData[6 + i] = new String[]{"Answer", "100", String.valueOf(extractValue(solutionStringArray[i]))};
                }

                allQuestions.add(csvData);

                run.addCarriageReturn();
                for (String solString : solutionStringArray) {
                    run.setText(solString);
                    run.addCarriageReturn();
                }
                run.addBreak();
                run.addBreak();
            }
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


            // Generate CSV
            String str = solution.replaceAll("[\\[\\]]", "").trim();
            String[] entries = str.split("\\s*,\\s*");

            String[][] csvData = new String[6 + entries.length][];
            csvData[0] = new String[]{"NewQuestion", "SA"};
            csvData[1] = new String[]{"Title", title};
            csvData[2] = new String[]{"QuestionText", questionText};
            csvData[3] = new String[]{"Points", "1"};
            csvData[4] = new String[]{"Difficulty", "1"};
            csvData[5] = new String[]{"InputBox", String.valueOf(entries.length), "40"};
            for (int i = 0; i < entries.length; i++) {
                csvData[6 + i] = new String[]{"Answer", "100", String.valueOf(entries[i])};
            }

            allQuestions.add(csvData);

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
