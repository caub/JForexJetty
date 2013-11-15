package jforex.jetty;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StaticServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String filename = request.getPathInfo();
        if (filename.endsWith("/"))
        	filename+="index.html";
        StringBuffer buf=new StringBuffer();
        try {
    		FileInputStream fstream = new FileInputStream(new File("static", filename));
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String s;
            while (((s=br.readLine()) != null) ) {
                buf.append(s).append('\n');
                
            }
	            
            in.close();
            response.setStatus(HttpServletResponse.SC_OK);
            if (filename.endsWith("js"))
	            response.setContentType("application/javascript");
            else if(filename.endsWith("css"))
            	response.setContentType("text/css");
            else
            	response.setContentType("text/html");
            
	        response.getWriter().write(buf.toString());
            
        } catch (Exception e) { // Catch exception if any
        	System.out.println("File access error: " + e.getMessage());
        	 response.setStatus(HttpServletResponse.SC_NOT_FOUND);
             //response.getWriter().write(buf.toString());
        } 
    }
}