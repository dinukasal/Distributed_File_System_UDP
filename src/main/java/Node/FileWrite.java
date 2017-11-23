package Node;

import java.io.*;
import java.util.ArrayList;

public class FileWrite{
    private String outFile="output.txt";
    private PrintWriter writer;
    private ArrayList<String> list=new ArrayList<String>();

    public void addLine(String line){
        list.add(line);
    }

    public FileWrite(){
        try{
            writer=new PrintWriter(new BufferedWriter(new FileWriter(outFile,true)));
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