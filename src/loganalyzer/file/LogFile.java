package loganalyzer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class LogAnalyzerCountingMalformed{
    public static void main(String[] args){
        String filePath = "server.log";
        int malformedCount = 0;
        
        String expectedFormat = "^\\[\\d{4}-\\d{2}-\\d{2}\\] \\[(INFO|WARN|ERROR|DEBUG)\\] .*";
        Pattern pattern = Pattern.compile(expectedFormat);
        
        try(BufferedReader br = new BufferedReader(new FileReader(filePath))){
            String line;
            while((line = br.readLine()) != null){
                if(line.trim().isEmpty()){
                    continue;
                }
                
                Matcher matcher = pattern.matcher(line);
                
                if(!matcher.matches()){
                    malformedCount++;
                }
            }
        }catch(IOException e){
            System.err.println("Error reading the file: " + e.getMessage());
        }
        
        System.out.println("Total malformed lines found: " + malformedCount);
    }
}
