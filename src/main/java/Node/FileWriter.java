package Node;

import java.io.*;
import java.util.ArrayList;

public class FileWriter{
    private String outFile="output.txt";
    private PrintWriter writer;
    private ArrayList<String> list=new ArrayList<String>();

    public void addLine(String line){
        list.add(line);
    }

    public FileWriter(){
        try{
            writer=new PrintWriter(outFile);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void closeFile(){
        for(String object: list){
            writer.println(object);
        }
        writer.close();        
    }

}