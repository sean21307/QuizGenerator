import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;


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
        String filePath = "src/main/java/template.txt";
        Random rand = new Random();

        try (Scanner file = new Scanner(new File(filePath));) {
            XWPFDocument document = new XWPFDocument();
            FileOutputStream out = new FileOutputStream("quiz.docx");

            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();

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

                    // Used to denote a set of units
                    if (sub.contains("{")) {
                        ArrayList<String> setValues = new ArrayList<String>();
                        sub = sub.substring(2, sub.length() - 1);

                        String[] split = sub.split(",");
                        for (String setValueName: split) {
                            setValues.add(setValueName.trim());
                        }

                        variables.put(variableName, setValues);
                    }

                    // Used to randomly pick a unit from a set of units
                    else if (sub.contains("from")) {
                        int hashMark = sub.indexOf("#");
                        int hashMark2 = sub.indexOf("#", hashMark + 1);
                        String setNum = sub.substring(hashMark + 1, hashMark2);

                        ArrayList<String> setOptions = (ArrayList<String>) variables.get(setNum);

                        int setSize = setOptions.size();

                        String pickedValue = setOptions.get((int)(Math.random() * setSize));
                        variables.put(variableName, pickedValue);
                    } else {
                        String[] variableInformation = sub.trim().split(",");
                        String variableType = variableInformation[0].trim();
                        String generationType = variableInformation[1].trim();

                        if (variableType.equals("int")) {
                            if (generationType.equals("random")) {
                                int min = Integer.parseInt(variableInformation[2].trim());
                                int max = Integer.parseInt(variableInformation[3].trim());
                                int generated = rand.nextInt(max + 1 - min) + min;
                                variables.put(variableName, generated);
                            }
                        }


                        else if (variableType.equals("double")) {
                            if (generationType.equals("random")) {
                                int min = Integer.parseInt(variableInformation[2].trim());
                                int max = Integer.parseInt(variableInformation[3].trim());
                                double generated = (double) rand.nextInt(max + 1 - min) + min;
                                variables.put(variableName, generated);
                            }
                        }


                    }
                } else if (nextLine.equals(":Text:")) {

                    boolean setQuestionText = false;
                    while (file.hasNext()) {
                        String textLine = file.nextLine();
                        if (textLine.equals(":EndText:")) {
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
                } else if (nextLine.contains("Solution:")) {
                    int colonIndex = nextLine.indexOf(":");
                    String solutionString = nextLine.substring(colonIndex + 1).trim();
                    solutionString = replaceVariables(solutionString);
                    double result = EvaluateExpression.evaluateExpression(solutionString);
                    String resultString;
                    String type = "double";

                    // Get type of solution value
                    nextLine = file.nextLine();
                    if (nextLine.startsWith("SolutionType:")) {
                        type = nextLine.substring(nextLine.indexOf(":") + 1).trim();
                    }

                    // Get units
                    List<String> units = new ArrayList<>();
                    nextLine = file.nextLine();
                    if (nextLine.startsWith("Unit:")) {
                        String unitString = nextLine.substring(nextLine.indexOf("{") + 1, nextLine.indexOf("}"));
                        units = Arrays.asList(unitString.split(","));
                        for (int i = 0; i < units.size(); i++) {
                            units.set(i, units.get(i).trim());
                        }
                    }

                    switch (type) {
                        case "long":
                        case "int":
                            result = (int) result;
                            resultString = String.valueOf((int) result);
                            break;
                        default:
                            resultString = String.valueOf(new DecimalFormat("#.##").format(result));
                            break;
                    }


                    StringBuilder solutionBuilder = new StringBuilder("[");
                    for (String unit : units) {
                        solutionBuilder.append(resultString).append(unit).append(", ");
                    }
                    solutionBuilder.setLength(solutionBuilder.length() - 2);  // Remove last comma and space
                    solutionBuilder.append("]");
                    String solution = solutionBuilder.toString();

                    run.setText(String.valueOf(solution));
                    run.addBreak();
                    run.addBreak();
                }
            }

            document.write(out);
            System.out.println("quiz.docx generated successfully");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}