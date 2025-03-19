import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class assembler{
    private static HashMap<String, String> dest = new HashMap<>();
    private static HashMap<String, String> jump = new HashMap<>();
    private static HashMap<String, String> comp = new HashMap<>();
    private static HashMap<String, String> symbols = new HashMap<>();

    private static ArrayList<String> commands = new ArrayList<>();
    private static ArrayList<String> asm = new ArrayList<>();

    private static int lineno = 0;
    private static int variableno = 16; 

    public static void populateDictionary(){
        //destination binary values (added equal sign to make reading easier)
        dest.put(" ", "000");
        dest.put("M=", "001");
        dest.put("D=", "010");
        dest.put("MD=", "011");
        dest.put("A=", "100");
        dest.put("AM=", "101");
        dest.put("AD=", "110" );
        dest.put("AMD=", "111");

        //jump binary values
        jump.put(" ", "000");
        jump.put("JGT", "001");
        jump.put("JEQ", "010");
        jump.put("JGE", "011");
        jump.put("JLT", "100");
        jump.put("JNE", "101");
        jump.put("JLE", "110");
        jump.put("JMP", "111");

        //comp binary values (value[0] is a)
        comp.put("0", "0101010");
        comp.put("1", "0111111");
        comp.put("-1", "0111010");
        comp.put("D", "0001100" );
        comp.put("A", "0110000");
        comp.put("M", "1110000");
        comp.put("!D", "0001101");
        comp.put("!A", "0110001");
        comp.put("!M", "1110001");
        comp.put("-D", "0001111");
        comp.put("-A", "0110011");
        comp.put("-M", "1110011");
        comp.put("D+1", "0011111");
        comp.put("A+1", "0110111");
        comp.put("M+1", "1110111");
        comp.put("D-1", "0001110");
        comp.put("A-1", "0110010");
        comp.put("M-1", "1110010");
        comp.put("D+A", "0000010");
        comp.put("D+M", "1000010");
        comp.put("D-A", "0010011");
        comp.put("D-M", "1010011");
        comp.put("A-D", "0000111");
        comp.put("M-D", "1000111");
        comp.put("D&A", "00000000");
        comp.put("D&M", "1000000");
        comp.put("D|A", "0010101");
        comp.put("D|M", "1010101");

        //symbols and registers dictionary 
        symbols.put("SP", "0");
        symbols.put("LCL", "1");
        symbols.put("ARG", "2");
        symbols.put("THIS", "3");
        symbols.put("THAT", "4");
        symbols.put("SCREEN", "16384");
        symbols.put("KBD", "24576");
        symbols.put("R0", "0");
        symbols.put("R1", "1");
        symbols.put("R2", "2");
        symbols.put("R3", "3");
        symbols.put("R4", "4");
        symbols.put("R5", "5");
        symbols.put("R6", "6");
        symbols.put("R7", "7");
        symbols.put("R8", "8");
        symbols.put("R9", "9");
        symbols.put("R10", "10");
        symbols.put("R11", "11");
        symbols.put("R12", "12");
        symbols.put("R13", "13");
        symbols.put("R14", "14");
        symbols.put("R15", "15");
    }
    public static void readAndCleanFile(String filename) throws FileNotFoundException {
        File file = new File("assembler\\Asm_Files\\" + filename + ".asm");
            Scanner scanner = new Scanner(file);
            System.out.println("Expected Path: " + file.getAbsolutePath());
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().replaceAll("//.*", "").trim(); //getting rid of comments
                if (!line.isEmpty()) {
                    commands.add(line);
                }
            }
        scanner.close();
    }

    public static void labelResolve(){
        for (String command : commands) {
            Matcher matcher = Pattern.compile("\\((.+)\\)").matcher(command); //finding labels and adding them to symbols
            if (matcher.find()) {
                symbols.put(matcher.group(1), Integer.toString(lineno));
                lineno--;
            }
            lineno++;
        }
    }

    public static void labelRemove(){
        for (String line : commands) {
            String cleaned = line.replaceAll("\\(.+\\)", "").trim(); //removing labels  
            if (!cleaned.isEmpty()) {
                asm.add(cleaned);
            }
        }
    }

    public static void variableResolve(){
        for (String command : asm) {
            Matcher matcher = Pattern.compile("@[a-zA-Z]+.*").matcher(command); //resolving variables to symbols if not existing
            if (matcher.find() && !symbols.containsKey(matcher.group().substring(1))) {
                symbols.put(matcher.group().substring(1), String.valueOf(variableno++));
            }
        }
    }

    public static void writeHack(String filename){
        try (FileWriter hackFile = new FileWriter(filename + ".hack")) {
            for (String command : asm) {
                if (command.startsWith("@")) { // A-instruction
                    int address = Integer.valueOf(symbols.getOrDefault(command.substring(1), command.substring(1)));
                    String binaryAddress = String.format("0%15s",Integer.toBinaryString(address)).replace(' ', '0');
                    hackFile.write(binaryAddress + "\n");
                } 
                else { //C-instruction
                    String destBits = "000";
                    String jumpBits = "000";
                    String compBits;

                   
                    if (command.contains("=")) {
                        String[] parts = command.split("=");
                        destBits = dest.getOrDefault(parts[0] + "=", "000");
                        command = parts[1]; 
                    }

                    
                    if (command.contains(";")) {
                        String[] parts = command.split(";");
                        compBits = comp.get(parts[0]); 
                        jumpBits = jump.getOrDefault(";" + parts[1], "000");
                    } else {
                        compBits = comp.get(command);
                    }
                    hackFile.write("111" + compBits + destBits + jumpBits + "\n");
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing to file.");
        }
    }
    public static void main(String[] args) {
        
        populateDictionary();

        Scanner input = new Scanner(System.in);
        String filename = input.nextLine();
        input.close();
        

        try {
            readAndCleanFile(filename);

            labelResolve();

            labelRemove();

            variableResolve();
            
            writeHack(filename);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } 
    }
}